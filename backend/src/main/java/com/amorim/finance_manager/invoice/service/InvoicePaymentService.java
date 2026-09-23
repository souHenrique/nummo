package com.amorim.finance_manager.invoice.service;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditApplicationRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.creditcard.service.CreditCardCreditApplicationService;
import com.amorim.finance_manager.invoice.dto.InvoicePaymentResponse;
import com.amorim.finance_manager.invoice.dto.PayInvoiceRequest;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.shared.exception.InvalidInvoicePaymentException;
import com.amorim.finance_manager.shared.exception.InvoiceNotFoundException;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoicePaymentService {

    private final InvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private final CreditCardCreditApplicationRepository applicationRepository;
    private final CreditCardCreditApplicationService creditApplicationService;
    private final AccountBalanceService accountBalanceService;

    private final CurrentUserService currentUserService;
    private final Clock financeClock;

    @Transactional
    public InvoicePaymentResponse pay(
            UUID invoiceId,
            PayInvoiceRequest request
    ) {
        validateRequest(request);

        UUID userId = currentUserService.getCurrentUserId();

        Invoice invoice = invoiceRepository
                .findOwnedById(invoiceId, userId)
                .orElseThrow(InvoiceNotFoundException::new);

        validateInvoice(invoice, request.expectedVersion());

        CreditCard card = creditCardRepository
                .findByIdAndUserId(invoice.getCreditCardId(), userId)
                .orElseThrow(CreditCardNotFoundException::new);

        validateNoPreviousSettlement(invoice.getId(), userId);

        if (request.sourceAccountId() != null) {
            validateAccount(request.sourceAccountId(), userId);
        }

        BigDecimal totalAmount = invoice.getTotalAmount();

        BigDecimal newAvailableLimit = calculateAvailableLimit(card, totalAmount);

        BigDecimal creditAppliedAmount = creditApplicationService.apply(userId, invoice, totalAmount);

        if (creditAppliedAmount == null
                || creditAppliedAmount.signum() < 0
                || creditAppliedAmount.compareTo(totalAmount) > 0) {
            throw new InvalidInvoicePaymentException(
                    "O valor de crédito aplicado é inconsistente"
            );
        }

        BigDecimal cashPaidAmount = totalAmount.subtract(creditAppliedAmount);

        if (cashPaidAmount.signum() > 0 && request.sourceAccountId() == null) {
                throw new InvalidInvoicePaymentException("Informe uma conta para pagar o valor restante");
        }

        Instant paymentInstant = financeClock.instant();

        LocalDate paymentDate = request.paymentDate();

        UUID paymentTransactionId = null;

        if (cashPaidAmount.signum() > 0) {
            Transaction payment = createPaymentTransaction(
                    userId,
                    invoice,
                    request.sourceAccountId(),
                    cashPaidAmount,
                    paymentDate
            );

            paymentTransactionId = payment.getId();

            accountBalanceService.debit(userId, request.sourceAccountId(), cashPaidAmount);
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(paymentInstant);

        invoiceRepository.saveAndFlush(invoice);

        if (totalAmount.signum() > 0) {
            card.setAvailableLimit(newAvailableLimit);
            creditCardRepository.saveAndFlush(card);
        }

        return new InvoicePaymentResponse(
                invoice.getId(),
                totalAmount,
                creditAppliedAmount,
                cashPaidAmount,
                paymentTransactionId,
                invoice.getPaidAt()
        );
    }

    private void validateRequest(PayInvoiceRequest request) {
        if (request == null
                || request.expectedVersion() == null
                || request.expectedVersion() < 0) {
            throw new InvalidInvoicePaymentException("Informe uma versão válida da fatura");
        }
    }

    private void validateInvoice(
            Invoice invoice,
            Long expectedVersion
    ) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidInvoicePaymentException(
                    "A fatura já está quitada"
            );
        }

        if (invoice.getStatus() != InvoiceStatus.CLOSED) {
            throw new InvalidInvoicePaymentException(
                    "Somente faturas fechadas podem ser quitadas"
            );
        }

        if (invoice.getPaidAt() != null) {
            throw new InvalidInvoicePaymentException(
                    "A fatura possui dados de pagamento inconsistentes"
            );
        }

        if (!Objects.equals(invoice.getVersion(), expectedVersion)) {
            throw new OptimisticLockingFailureException(
                    "A fatura foi alterada. Consulte os dados novamente."
            );
        }

        LocalDate today = LocalDate.now(financeClock);

        if (invoice.getClosingDate() == null
                || !invoice.getClosingDate().isBefore(today)) {
            throw new InvalidInvoicePaymentException(
                    "O dia de fechamento da fatura ainda não terminou"
            );
        }

        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().signum() < 0) {
            throw new InvalidInvoicePaymentException(
                    "A fatura possui total inválido"
            );
        }
    }

    private void validateNoPreviousSettlement(
            UUID invoiceId,
            UUID userId
    ) {
        boolean paymentExists = transactionRepository
                .existsByInvoiceIdAndUserIdAndTypeAndStatus(
                        invoiceId,
                        userId,
                        TransactionType.CREDIT_CARD_PAYMENT,
                        TransactionStatus.COMPLETED
                );

        if (paymentExists) {
            throw new InvalidInvoicePaymentException(
                    "A fatura já possui um pagamento concluído"
            );
        }

        BigDecimal appliedAmount =
                applicationRepository.sumAppliedAmount(invoiceId);

        if (appliedAmount == null || appliedAmount.signum() != 0) {
            throw new InvalidInvoicePaymentException(
                    "A fatura possui aplicações de crédito incompatíveis "
                            + "com uma nova quitação integral"
            );
        }
    }

    private void validateAccount(
            UUID accountId,
            UUID userId
    ) {
        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(AccountNotFoundException::new);

        account.ensureActive();
    }

    private BigDecimal calculateAvailableLimit(
            CreditCard card,
            BigDecimal settledAmount
    ) {
        if (card.getCreditLimit() == null
                || card.getCreditLimit().signum() <= 0
                || card.getAvailableLimit() == null
                || card.getAvailableLimit().signum() < 0
                || card.getAvailableLimit()
                .compareTo(card.getCreditLimit()) > 0) {
            throw new InvalidInvoicePaymentException(
                    "O cartão possui limite inconsistente"
            );
        }

        BigDecimal newAvailableLimit =
                card.getAvailableLimit().add(settledAmount);

        if (newAvailableLimit.compareTo(card.getCreditLimit()) > 0) {
            throw new InvalidInvoicePaymentException(
                    "A quitação ultrapassaria o limite total do cartão"
            );
        }

        return newAvailableLimit;
    }

    private Transaction createPaymentTransaction(
            UUID userId,
            Invoice invoice,
            UUID sourceAccountId,
            BigDecimal amount,
            LocalDate paymentDate
    ) {
        Transaction payment = new Transaction();

        payment.setUserId(userId);
        payment.setDescription("Pagamento de fatura");
        payment.setAmount(amount);

        payment.setCompetenceDate(paymentDate);
        payment.setEffectiveDate(paymentDate);
        payment.setDueDate(invoice.getDueDate());

        payment.setType(TransactionType.CREDIT_CARD_PAYMENT);
        payment.setStatus(TransactionStatus.COMPLETED);
        payment.setPaymentMethod(PaymentMethod.OTHER);

        payment.setSourceAccountId(sourceAccountId);
        payment.setCreditCardId(invoice.getCreditCardId());
        payment.setInvoiceId(invoice.getId());

        return transactionRepository.saveAndFlush(payment);
    }
}
