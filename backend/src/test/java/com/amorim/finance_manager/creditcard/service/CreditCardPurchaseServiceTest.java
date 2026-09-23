package com.amorim.finance_manager.creditcard.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.creditcard.dto.CreateCreditCardPurchaseRequest;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.creditcard.repository.CreditCardRefundItemRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.invoice.service.InvoiceCycle;
import com.amorim.finance_manager.invoice.service.InvoiceCycleService;
import com.amorim.finance_manager.shared.exception.CategoryNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditCardNotFoundException;
import com.amorim.finance_manager.shared.exception.CreditLimitConflictException;
import com.amorim.finance_manager.shared.exception.IncompatibleCategoryTypeException;
import com.amorim.finance_manager.shared.exception.InvalidCreditCardStatusException;
import com.amorim.finance_manager.shared.exception.InvalidInvoiceStatusException;
import com.amorim.finance_manager.shared.exception.InvalidTransactionException;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.mapper.TransactionMapper;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardPurchaseServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CARD_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2026, 9, 11);

    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceCycleService invoiceCycleService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private CreditCardRefundItemRepository refundItemRepository;

    private CreditCardPurchaseService service;

    @BeforeEach
    void setUp() {
        service = new CreditCardPurchaseService(
                creditCardRepository,
                categoryRepository,
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper,
                currentUserService,
                new InstallmentCalculator(),
                refundItemRepository
        );
    }

    @Test
    void shouldCreateOneIdentifiedInstallmentAndUpdateCardAndInvoice() {
        CreateCreditCardPurchaseRequest request = request("100.00", 1);
        CreditCard card = card(CreditCardStatus.ACTIVE, "100.00");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        InvoiceCycle cycle = cycle(10, 2026);
        Invoice invoice = invoice(UUID.randomUUID(), cycle, InvoiceStatus.OPEN, "20.00");
        TransactionResponse response = mock(TransactionResponse.class);
        List<Transaction> persisted = new ArrayList<>();

        stubOwnedResources(card, category, cycle);
        when(creditCardRepository.saveAndFlush(card)).thenReturn(card);
        when(invoiceCycleService.shift(cycle, 0, 10, 17)).thenReturn(cycle);
        when(invoiceCycleService.findOrCreate(card, cycle)).thenReturn(invoice);
        when(invoiceRepository.saveAndFlush(invoice)).thenReturn(invoice);
        stubTransactionPersistence(persisted);
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(response);

        List<TransactionResponse> result = service.create(CARD_ID, request);

        assertThat(result).containsExactly(response);
        assertThat(card.getAvailableLimit()).isEqualByComparingTo("0.00");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("120.00");
        assertThat(persisted).hasSize(1);

        Transaction transaction = persisted.getFirst();
        assertThat(transaction.getUserId()).isEqualTo(USER_ID);
        assertThat(transaction.getDescription()).isEqualTo("Supermercado");
        assertThat(transaction.getAmount()).isEqualByComparingTo("100.00");
        assertThat(transaction.getCompetenceDate()).isEqualTo(PURCHASE_DATE);
        assertThat(transaction.getEffectiveDate()).isNull();
        assertThat(transaction.getDueDate()).isEqualTo(cycle.dueDate());
        assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT_CARD_PURCHASE);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(transaction.getSourceAccountId()).isNull();
        assertThat(transaction.getDestinationAccountId()).isNull();
        assertThat(transaction.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(transaction.getCreditCardId()).isEqualTo(CARD_ID);
        assertThat(transaction.getInvoiceId()).isEqualTo(invoice.getId());
        assertThat(transaction.getInstallmentGroupId()).isNotNull();
        assertThat(transaction.getInstallmentNumber()).isEqualTo(1);
        assertThat(transaction.getInstallmentCount()).isEqualTo(1);

        InOrder persistenceOrder = inOrder(
                creditCardRepository,
                invoiceCycleService,
                invoiceRepository,
                transactionRepository
        );
        persistenceOrder.verify(invoiceCycleService).calculate(PURCHASE_DATE, 10, 17);
        persistenceOrder.verify(creditCardRepository).saveAndFlush(card);
        persistenceOrder.verify(invoiceCycleService).shift(cycle, 0, 10, 17);
        persistenceOrder.verify(invoiceCycleService).findOrCreate(card, cycle);
        persistenceOrder.verify(invoiceRepository).saveAndFlush(invoice);
        persistenceOrder.verify(transactionRepository).saveAllAndFlush(anyList());
    }

    @Test
    void shouldCreateThreeInstallmentsInConsecutiveInvoicesAndConsumeTheTotalLimitOnce() {
        CreateCreditCardPurchaseRequest request = request("100.00", 3);
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        InvoiceCycle firstCycle = cycle(10, 2026);
        InvoiceCycle secondCycle = cycle(11, 2026);
        InvoiceCycle thirdCycle = cycle(12, 2026);
        Invoice firstInvoice = invoice(UUID.randomUUID(), firstCycle, InvoiceStatus.OPEN, "0.00");
        Invoice secondInvoice = invoice(UUID.randomUUID(), secondCycle, InvoiceStatus.OPEN, "0.00");
        Invoice thirdInvoice = invoice(UUID.randomUUID(), thirdCycle, InvoiceStatus.OPEN, "0.00");
        List<Transaction> persisted = new ArrayList<>();

        stubOwnedResources(card, category, firstCycle);
        when(creditCardRepository.saveAndFlush(card)).thenReturn(card);
        when(invoiceCycleService.shift(firstCycle, 0, 10, 17)).thenReturn(firstCycle);
        when(invoiceCycleService.shift(firstCycle, 1, 10, 17)).thenReturn(secondCycle);
        when(invoiceCycleService.shift(firstCycle, 2, 10, 17)).thenReturn(thirdCycle);
        when(invoiceCycleService.findOrCreate(card, firstCycle)).thenReturn(firstInvoice);
        when(invoiceCycleService.findOrCreate(card, secondCycle)).thenReturn(secondInvoice);
        when(invoiceCycleService.findOrCreate(card, thirdCycle)).thenReturn(thirdInvoice);
        when(invoiceRepository.saveAndFlush(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubTransactionPersistence(persisted);
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenAnswer(invocation -> mock(TransactionResponse.class));

        List<TransactionResponse> result = service.create(CARD_ID, request);

        assertThat(result).hasSize(3);
        assertThat(card.getAvailableLimit()).isEqualByComparingTo("400.00");
        assertThat(firstInvoice.getTotalAmount()).isEqualByComparingTo("33.33");
        assertThat(secondInvoice.getTotalAmount()).isEqualByComparingTo("33.33");
        assertThat(thirdInvoice.getTotalAmount()).isEqualByComparingTo("33.34");

        assertThat(persisted).extracting(Transaction::getAmount).containsExactly(
                new BigDecimal("33.33"),
                new BigDecimal("33.33"),
                new BigDecimal("33.34")
        );
        assertThat(persisted).extracting(Transaction::getInstallmentNumber)
                .containsExactly(1, 2, 3);
        assertThat(persisted).extracting(Transaction::getInstallmentCount)
                .containsOnly(3);
        assertThat(persisted).extracting(Transaction::getCompetenceDate).containsExactly(
                LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 10, 11),
                LocalDate.of(2026, 11, 11)
        );
        assertThat(persisted).extracting(Transaction::getInvoiceId).containsExactly(
                firstInvoice.getId(),
                secondInvoice.getId(),
                thirdInvoice.getId()
        );

        UUID installmentGroupId = persisted.getFirst().getInstallmentGroupId();
        assertThat(installmentGroupId).isNotNull();
        assertThat(persisted).allSatisfy(transaction ->
                assertThat(transaction.getInstallmentGroupId()).isEqualTo(installmentGroupId));

        verify(creditCardRepository).saveAndFlush(card);
        verify(invoiceCycleService).shift(firstCycle, 0, 10, 17);
        verify(invoiceCycleService).shift(firstCycle, 1, 10, 17);
        verify(invoiceCycleService).shift(firstCycle, 2, 10, 17);
    }

    @Test
    void shouldHideMissingOrForeignCardsAndStopBeforeReadingTheCategory() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00", 1)))
                .isInstanceOf(CreditCardNotFoundException.class);

        verifyNoInteractions(
                categoryRepository,
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper
        );
    }

    @ParameterizedTest
    @EnumSource(value = CreditCardStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectCardsThatAreNotActive(CreditCardStatus status) {
        CreditCard card = card(status, "500.00");
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00", 1)))
                .isInstanceOf(InvalidCreditCardStatusException.class);

        verifyNoInteractions(
                categoryRepository,
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper
        );
        verify(creditCardRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldHideMissingOrForeignCategoriesAndLeaveTheCardUntouched() {
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00", 1)))
                .isInstanceOf(CategoryNotFoundException.class);

        assertThat(card.getAvailableLimit()).isEqualByComparingTo("500.00");
        verify(creditCardRepository, never()).saveAndFlush(any());
        verifyNoInteractions(
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper
        );
    }

    @Test
    void shouldRejectAnIncomeCategory() {
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        Category category = category(CategoryType.INCOME, CategoryStatus.ACTIVE);
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00", 1)))
                .isInstanceOf(IncompatibleCategoryTypeException.class);

        verify(creditCardRepository, never()).saveAndFlush(any());
        verifyNoInteractions(
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper
        );
    }

    @Test
    void shouldRejectAnInactiveExpenseCategory() {
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.INACTIVE);
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(CARD_ID, request("10.00", 1)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Categoria inativa não pode receber novas compras");

        verify(creditCardRepository, never()).saveAndFlush(any());
        verifyNoInteractions(
                invoiceRepository,
                invoiceCycleService,
                transactionRepository,
                transactionMapper
        );
    }

    @Test
    void shouldRejectInsufficientTotalLimitBeforeCreatingInvoicesOrTransactions() {
        CreditCard card = card(CreditCardStatus.ACTIVE, "99.99");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        InvoiceCycle cycle = cycle(10, 2026);
        stubOwnedResources(card, category, cycle);

        assertThatThrownBy(() -> service.create(CARD_ID, request("100.00", 12)))
                .isInstanceOf(CreditLimitConflictException.class)
                .hasMessage("Limite disponível insuficiente para realizar a compra");

        assertThat(card.getAvailableLimit()).isEqualByComparingTo("99.99");
        verify(creditCardRepository, never()).saveAndFlush(any());
        verify(invoiceCycleService, never()).findOrCreate(
                any(CreditCard.class),
                any(InvoiceCycle.class)
        );
        verifyNoInteractions(invoiceRepository, transactionRepository, transactionMapper);
    }

    @ParameterizedTest
    @EnumSource(value = InvoiceStatus.class, names = "OPEN", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectAnInvoiceThatIsNotOpen(InvoiceStatus status) {
        CreditCard card = card(CreditCardStatus.ACTIVE, "500.00");
        Category category = category(CategoryType.EXPENSE, CategoryStatus.ACTIVE);
        InvoiceCycle cycle = cycle(10, 2026);
        Invoice invoice = invoice(UUID.randomUUID(), cycle, status, "20.00");
        stubOwnedResources(card, category, cycle);
        when(creditCardRepository.saveAndFlush(card)).thenReturn(card);
        when(invoiceCycleService.shift(cycle, 0, 10, 17)).thenReturn(cycle);
        when(invoiceCycleService.findOrCreate(card, cycle)).thenReturn(invoice);

        assertThatThrownBy(() -> service.create(CARD_ID, request("100.00", 1)))
                .isInstanceOf(InvalidInvoiceStatusException.class);

        verify(invoiceRepository, never()).saveAndFlush(any());
        verifyNoInteractions(transactionRepository, transactionMapper);
    }

    private void stubOwnedResources(CreditCard card, Category category, InvoiceCycle cycle) {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(creditCardRepository.findByIdAndUserId(CARD_ID, USER_ID))
                .thenReturn(Optional.of(card));
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));
        when(invoiceCycleService.calculate(PURCHASE_DATE, 10, 17)).thenReturn(cycle);
    }

    private void stubTransactionPersistence(List<Transaction> persisted) {
        when(transactionRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> {
                    List<Transaction> transactions = invocation.getArgument(0);
                    persisted.addAll(transactions);
                    return transactions;
                });
    }

    private CreateCreditCardPurchaseRequest request(String amount, int installmentCount) {
        return new CreateCreditCardPurchaseRequest(
                "Supermercado",
                new BigDecimal(amount),
                PURCHASE_DATE,
                CATEGORY_ID,
                installmentCount
        );
    }

    private CreditCard card(CreditCardStatus status, String availableLimit) {
        CreditCard card = new CreditCard();
        card.setId(CARD_ID);
        card.setUserId(USER_ID);
        card.setCreditLimit(new BigDecimal("1000.00"));
        card.setAvailableLimit(new BigDecimal(availableLimit));
        card.setClosingDay(10);
        card.setDueDay(17);
        card.setStatus(status);
        card.setVersion(0L);
        return card;
    }

    private Category category(CategoryType type, CategoryStatus status) {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setUserId(USER_ID);
        category.setType(type);
        category.setStatus(status);
        return category;
    }

    private InvoiceCycle cycle(int referenceMonth, int referenceYear) {
        return new InvoiceCycle(
                referenceMonth,
                referenceYear,
                LocalDate.of(referenceYear, referenceMonth, 10),
                LocalDate.of(referenceYear, referenceMonth, 17)
        );
    }

    private Invoice invoice(
            UUID invoiceId,
            InvoiceCycle cycle,
            InvoiceStatus status,
            String totalAmount
    ) {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setCreditCardId(CARD_ID);
        invoice.setReferenceMonth(cycle.referenceMonth());
        invoice.setReferenceYear(cycle.referenceYear());
        invoice.setClosingDate(cycle.closingDate());
        invoice.setDueDate(cycle.dueDate());
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setStatus(status);
        invoice.setVersion(0L);
        return invoice;
    }
}
