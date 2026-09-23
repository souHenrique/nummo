package com.amorim.finance_manager.invoice.service;

import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardCredit;
import com.amorim.finance_manager.creditcard.entity.CreditCardCreditApplication;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditApplicationRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardCreditRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.mapper.InvoiceMapper;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceClosingServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CreditCardRepository creditCardRepository;
    @Mock private CreditCardCreditRepository creditRepository;
    @Mock private CreditCardCreditApplicationRepository applicationRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountBalanceService accountBalanceService;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private CurrentUserService currentUserService;
    @Mock private Clock financeClock;

    @InjectMocks private InvoiceClosingService service;

    @Test
    void reopenPaidInvoiceReversesPaymentAndRestoresCredit() {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID creditId = UUID.randomUUID();

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setCreditCardId(cardId);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.parse("2026-09-23T10:00:00Z"));
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setVersion(4L);

        CreditCard card = new CreditCard();
        card.setId(cardId);
        card.setUserId(userId);
        card.setCreditLimit(new BigDecimal("1000.00"));
        card.setAvailableLimit(new BigDecimal("800.00"));

        Transaction payment = new Transaction();
        payment.setType(TransactionType.CREDIT_CARD_PAYMENT);
        payment.setStatus(TransactionStatus.COMPLETED);
        payment.setSourceAccountId(accountId);
        payment.setAmount(new BigDecimal("50.00"));

        CreditCardCredit credit = new CreditCardCredit();
        credit.setId(creditId);
        credit.setUserId(userId);
        credit.setCreditCardId(cardId);
        credit.setOriginalAmount(new BigDecimal("50.00"));
        credit.setRemainingAmount(BigDecimal.ZERO);

        CreditCardCreditApplication application = new CreditCardCreditApplication();
        application.setCreditId(creditId);
        application.setInvoiceId(invoiceId);
        application.setAmount(new BigDecimal("50.00"));

        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(invoiceRepository.findOwnedById(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(creditCardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(transactionRepository.findAllByInvoiceIdAndUserIdOrderByCompetenceDateAscCreatedAtAsc(
                invoiceId, userId)).thenReturn(List.of(payment));
        when(applicationRepository.findAllByInvoiceId(invoiceId)).thenReturn(List.of(application));
        when(creditRepository.findAllById(List.of(creditId))).thenReturn(List.of(credit));
        when(invoiceRepository.saveAndFlush(invoice)).thenReturn(invoice);

        service.reopen(invoiceId, 4L);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(invoice.getPaidAt()).isNull();
        assertThat(payment.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        assertThat(card.getAvailableLimit()).isEqualByComparingTo("700.00");
        assertThat(credit.getRemainingAmount()).isEqualByComparingTo("50.00");
        verify(accountBalanceService).reverseDebit(userId, accountId, new BigDecimal("50.00"));
        verify(applicationRepository).deleteAll(List.of(application));
        verify(creditCardRepository).saveAndFlush(card);
    }
}
