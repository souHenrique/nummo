package com.amorim.finance_manager.invoice.service;

import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardCredit;
import com.amorim.finance_manager.creditcard.entity.CreditCardCreditApplication;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditApplicationRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.dto.InvoiceSummaryResponse;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.mapper.InvoiceMapper;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.shared.exception.InvalidInvoiceStatusException;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.shared.exception.InvoiceNotFoundException;
import com.amorim.finance_manager.user.service.CurrentUserService;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceClosingService {

    private final InvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardCreditRepository creditRepository;
    private final CreditCardCreditApplicationRepository applicationRepository;
    private final TransactionRepository transactionRepository;
    private final AccountBalanceService accountBalanceService;
    private final InvoiceMapper invoiceMapper;
    private final CurrentUserService currentUserService;
    private final Clock financeClock;

    @Transactional
    public InvoiceSummaryResponse close(
            UUID invoiceId,
            Long expectedVersion
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        Invoice invoice = invoiceRepository
                .findOwnedById(invoiceId, userId)
                .orElseThrow(InvoiceNotFoundException::new);

        if (expectedVersion == null || expectedVersion < 0) {
            throw new InvalidInvoiceStatusException("Informe uma versão válida da fatura");
        }

        if (!Objects.equals(invoice.getVersion(), expectedVersion)) {
            throw new OptimisticLockingFailureException("A fatura foi alterada. Consulte os dados novamente.");
        }

        if (invoice.getStatus() != InvoiceStatus.OPEN) {
            throw new InvalidInvoiceStatusException("Somente faturas abertas podem ser fechadas");
        }

        if (invoice.getPaidAt() != null) {
            throw new InvalidInvoiceStatusException("A fatura possui dados de pagamento incompatíveis");
        }

        LocalDate today = LocalDate.now(financeClock);

        if (invoice.getClosingDate() == null
                || !invoice.getClosingDate().isBefore(today)) {
            throw new InvalidInvoiceStatusException("O dia de fechamento da fatura ainda não terminou");
        }

        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().signum() < 0) {
            throw new InvalidInvoiceStatusException("A fatura possui total inválido");
        }

        invoice.setStatus(InvoiceStatus.CLOSED);

        Invoice saved = invoiceRepository.saveAndFlush(invoice);

        return invoiceMapper.toSummary(saved);
    }

    @Transactional
    public InvoiceSummaryResponse reopen(UUID invoiceId, Long expectedVersion) {
        UUID userId = currentUserService.getCurrentUserId();
        Invoice invoice = invoiceRepository.findOwnedById(invoiceId, userId)
                .orElseThrow(InvoiceNotFoundException::new);

        if (expectedVersion == null || expectedVersion < 0) {
            throw new InvalidInvoiceStatusException("Informe uma versão válida da fatura");
        }
        if (!Objects.equals(invoice.getVersion(), expectedVersion)) {
            throw new OptimisticLockingFailureException("A fatura foi alterada. Consulte os dados novamente.");
        }
        if (invoice.getStatus() != InvoiceStatus.CLOSED && invoice.getStatus() != InvoiceStatus.PAID) {
            throw new InvalidInvoiceStatusException("Somente faturas fechadas ou pagas podem ser reabertas");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            reversePayment(userId, invoice);
        }

        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setPaidAt(null);
        return invoiceMapper.toSummary(invoiceRepository.saveAndFlush(invoice));
    }

    private void reversePayment(UUID userId, Invoice invoice) {
        CreditCard card = creditCardRepository.findByIdAndUserId(invoice.getCreditCardId(), userId)
                .orElseThrow(CreditCardNotFoundException::new);

        BigDecimal amount = invoice.getTotalAmount();
        if (amount == null || amount.signum() < 0) {
            throw new InvalidInvoiceStatusException("A fatura possui total inválido");
        }
        if (card.getAvailableLimit().compareTo(amount) < 0) {
            throw new InvalidInvoiceStatusException(
                    "Não é possível reabrir esta fatura porque o limite do cartão já foi utilizado por compras posteriores"
            );
        }

        List<Transaction> payments = transactionRepository
                .findAllByInvoiceIdAndUserIdOrderByCompetenceDateAscCreatedAtAsc(invoice.getId(), userId)
                .stream()
                .filter(transaction -> transaction.getType() == TransactionType.CREDIT_CARD_PAYMENT)
                .filter(transaction -> transaction.getStatus() == TransactionStatus.COMPLETED)
                .toList();

        for (Transaction payment : payments) {
            if (payment.getSourceAccountId() != null && payment.getAmount().signum() > 0) {
                accountBalanceService.reverseDebit(userId, payment.getSourceAccountId(), payment.getAmount());
            }
            payment.setStatus(TransactionStatus.CANCELLED);
        }
        transactionRepository.saveAllAndFlush(payments);

        restoreAppliedCredits(invoice, userId);

        card.setAvailableLimit(card.getAvailableLimit().subtract(amount));
        creditCardRepository.saveAndFlush(card);
    }

    private void restoreAppliedCredits(Invoice invoice, UUID userId) {
        List<CreditCardCreditApplication> applications = applicationRepository.findAllByInvoiceId(invoice.getId());
        if (applications.isEmpty()) {
            return;
        }

        Map<UUID, CreditCardCredit> credits = creditRepository.findAllById(
                        applications.stream().map(CreditCardCreditApplication::getCreditId).toList()
                )
                .stream()
                .collect(java.util.stream.Collectors.toMap(CreditCardCredit::getId, credit -> credit));

        for (CreditCardCreditApplication application : applications) {
            CreditCardCredit credit = credits.get(application.getCreditId());
            if (credit == null || !Objects.equals(credit.getUserId(), userId)
                    || !Objects.equals(credit.getCreditCardId(), invoice.getCreditCardId())) {
                throw new InvalidInvoiceStatusException("A fatura possui créditos de pagamento inconsistentes");
            }

            BigDecimal restoredAmount = credit.getRemainingAmount().add(application.getAmount());
            if (restoredAmount.compareTo(credit.getOriginalAmount()) > 0) {
                throw new InvalidInvoiceStatusException("A fatura possui créditos de pagamento inconsistentes");
            }
            credit.setRemainingAmount(restoredAmount);
        }

        creditRepository.saveAllAndFlush(credits.values());
        applicationRepository.deleteAll(applications);
    }
}
