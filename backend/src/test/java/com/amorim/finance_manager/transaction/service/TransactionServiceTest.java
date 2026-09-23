package com.amorim.finance_manager.transaction.service;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.account.service.AccountBalanceService;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.AccountNotFoundException;
import com.amorim.finance_manager.shared.exception.CategoryNotFoundException;
import com.amorim.finance_manager.shared.exception.InactiveAccountException;
import com.amorim.finance_manager.shared.exception.IncompatibleCategoryTypeException;
import com.amorim.finance_manager.shared.exception.InvalidTransactionException;
import com.amorim.finance_manager.shared.exception.TransactionNotFoundException;
import com.amorim.finance_manager.testsupport.LogCapture;
import com.amorim.finance_manager.transaction.dto.CreateTransactionRequest;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.dto.UpdateTransactionRequest;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ACCOUNT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID CATEGORY_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID TRANSACTION_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final BigDecimal AMOUNT =
            new BigDecimal("75.50");

    private static final LocalDate COMPETENCE_DATE =
            LocalDate.of(2026, 8, 31);

    private static final LocalDate EFFECTIVE_DATE =
            LocalDate.of(2026, 8, 31);

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountBalanceService accountBalanceService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TransactionImpactService transactionImpactService;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);
    }

    @Test
    void shouldCreateCompletedIncomeAndCreditDestinationAccount() {
        CreateTransactionRequest request = completedIncome(PaymentMethod.CASH);
        Category category = activeCategory(CategoryType.INCOME);
        Transaction transaction = transactionFrom(request);
        TransactionResponse response = responseFrom(request);

        mockCategory(category);
        mockSuccessfulPersistence(request, transaction, response);

        TransactionResponse result = transactionService.create(request);

        verify(accountBalanceService).credit(USER_ID, ACCOUNT_ID, AMOUNT);
        verify(accountBalanceService, never()).debit(any(), any(), any());
        assertThat(transaction.getUserId()).isEqualTo(USER_ID);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldLogTransactionCreationWithoutFinancialDetails() {
        CreateTransactionRequest request = completedExpense(PaymentMethod.DEBIT);
        Transaction transaction = transactionFrom(request);
        TransactionResponse response = responseFrom(request);

        mockCategory(activeCategory(CategoryType.EXPENSE));
        mockSuccessfulPersistence(request, transaction, response);

        try (LogCapture logs = LogCapture.forClass(TransactionService.class)) {
            transactionService.create(request);

            assertThat(logs.messages())
                    .contains(
                            "event=transaction.created transactionId=" + TRANSACTION_ID
                                    + " userId=" + USER_ID
                                    + " type=EXPENSE status=COMPLETED"
                    )
                    .allSatisfy(message -> {
                        assertThat(message).doesNotContain(AMOUNT.toPlainString());
                        assertThat(message).doesNotContain(request.description());
                    });
        }
    }

    @Test
    void shouldLogTransactionUpdate() {
        CreateTransactionRequest originalRequest = completedExpense(PaymentMethod.DEBIT);
        Transaction transaction = transactionFrom(originalRequest);
        transaction.setUserId(USER_ID);
        UpdateTransactionRequest updateRequest = new UpdateTransactionRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(transaction));
        mockCategory(activeCategory(CategoryType.EXPENSE));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(activeAccount()));
        when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);

        try (LogCapture logs = LogCapture.forClass(TransactionService.class)) {
            transactionService.update(TRANSACTION_ID, updateRequest);

            assertThat(logs.messages()).contains(
                    "event=transaction.updated transactionId=" + TRANSACTION_ID
                            + " userId=" + USER_ID
                            + " previousStatus=COMPLETED currentStatus=COMPLETED"
            );
        }
    }

    @Test
    void shouldLogTransactionCancellation() {
        CreateTransactionRequest request = completedExpense(PaymentMethod.DEBIT);
        Transaction transaction = transactionFrom(request);
        transaction.setUserId(USER_ID);

        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);

        try (LogCapture logs = LogCapture.forClass(TransactionService.class)) {
            transactionService.cancel(TRANSACTION_ID);

            assertThat(logs.messages())
                    .contains(
                            "event=transaction.cancelled transactionId=" + TRANSACTION_ID
                                    + " userId=" + USER_ID
                                    + " type=EXPENSE status=CANCELLED"
                    )
                    .allSatisfy(message ->
                            assertThat(message).doesNotContain(AMOUNT.toPlainString())
                    );
        }
    }

    @Test
    void shouldUpdateDescriptionAndCategoryForCreditCardPurchaseWithoutChangingBalance() {
        Transaction purchase = transactionFrom(completedExpense(PaymentMethod.DEBIT));
        purchase.setUserId(USER_ID);
        purchase.setType(TransactionType.CREDIT_CARD_PURCHASE);
        purchase.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        purchase.setCreditCardId(UUID.randomUUID());
        purchase.setInstallmentGroupId(null);

        UUID newCategoryId = UUID.randomUUID();
        Category newCategory = activeCategory(CategoryType.EXPENSE);
        newCategory.setId(newCategoryId);
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                "Compra do Jesse Pinkman",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                newCategoryId
        );
        TransactionResponse response = responseFrom(completedExpense(PaymentMethod.DEBIT));

        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(purchase));
        when(categoryRepository.findByIdAndUserId(newCategoryId, USER_ID))
                .thenReturn(Optional.of(newCategory));
        when(transactionMapper.toResponse(purchase)).thenReturn(response);

        TransactionResponse result = transactionService.update(TRANSACTION_ID, request);

        assertThat(result).isEqualTo(response);
        assertThat(purchase.getDescription()).isEqualTo("Compra do Jesse Pinkman");
        assertThat(purchase.getCategoryId()).isEqualTo(newCategoryId);
        verify(transactionRepository).saveAllAndFlush(List.of(purchase));
        verifyNoInteractions(transactionImpactService);
    }

    @Test
    void shouldRejectFinancialChangesToCreditCardPurchase() {
        Transaction purchase = transactionFrom(completedExpense(PaymentMethod.DEBIT));
        purchase.setUserId(USER_ID);
        purchase.setType(TransactionType.CREDIT_CARD_PURCHASE);
        purchase.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        UpdateTransactionRequest request = new UpdateTransactionRequest(
                null,
                new BigDecimal("80.00"),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(purchase));

        assertThatThrownBy(() -> transactionService.update(TRANSACTION_ID, request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("somente as informações descritivas");

        verifyNoInteractions(transactionImpactService);
        verify(transactionRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void shouldCreateCompletedExpenseAndDebitSourceAccount() {
        CreateTransactionRequest request = completedExpense(PaymentMethod.DEBIT);
        Category category = activeCategory(CategoryType.EXPENSE);
        Transaction transaction = transactionFrom(request);
        TransactionResponse response = responseFrom(request);

        mockCategory(category);
        mockSuccessfulPersistence(request, transaction, response);

        TransactionResponse result = transactionService.create(request);

        verify(accountBalanceService).debit(USER_ID, ACCOUNT_ID, AMOUNT);
        verify(accountBalanceService, never()).credit(any(), any(), any());
        assertThat(transaction.getUserId()).isEqualTo(USER_ID);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldCreditDestinationAccountForReceivedPix() {
        CreateTransactionRequest request = completedIncome(PaymentMethod.PIX);
        Transaction transaction = transactionFrom(request);
        TransactionResponse response = responseFrom(request);

        mockCategory(activeCategory(CategoryType.INCOME));
        mockSuccessfulPersistence(request, transaction, response);

        transactionService.create(request);

        verify(accountBalanceService).credit(USER_ID, ACCOUNT_ID, AMOUNT);
        verify(accountBalanceService, never()).debit(any(), any(), any());
    }

    @Test
    void shouldDebitSourceAccountForSentPix() {
        CreateTransactionRequest request = completedExpense(PaymentMethod.PIX);
        Transaction transaction = transactionFrom(request);
        TransactionResponse response = responseFrom(request);

        mockCategory(activeCategory(CategoryType.EXPENSE));
        mockSuccessfulPersistence(request, transaction, response);

        transactionService.create(request);

        verify(accountBalanceService).debit(USER_ID, ACCOUNT_ID, AMOUNT);
        verify(accountBalanceService, never()).credit(any(), any(), any());
    }

    @Test
    void shouldPersistPendingTransactionWithoutChangingBalance() {
        CreateTransactionRequest request = pendingExpense();
        Account account = activeAccount();
        Transaction transaction = transactionFrom(request);
        TransactionResponse response = responseFrom(request);

        mockCategory(activeCategory(CategoryType.EXPENSE));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(account));
        mockSuccessfulPersistence(request, transaction, response);

        TransactionResponse result = transactionService.create(request);

        verifyNoInteractions(accountBalanceService);
        verify(transactionRepository).saveAndFlush(transaction);
        assertThat(result.status()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void shouldRejectCategoryWithIncompatibleType() {
        CreateTransactionRequest request = completedIncome(PaymentMethod.PIX);

        mockCategory(activeCategory(CategoryType.EXPENSE));

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(IncompatibleCategoryTypeException.class);

        verifyNoInteractions(accountBalanceService);
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectInactiveAccount() {
        CreateTransactionRequest request = completedExpense(PaymentMethod.DEBIT);

        mockCategory(activeCategory(CategoryType.EXPENSE));
        doThrow(new InactiveAccountException())
                .when(accountBalanceService)
                .debit(USER_ID, ACCOUNT_ID, AMOUNT);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InactiveAccountException.class);

        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectAccountOwnedByAnotherUser() {
        CreateTransactionRequest request = pendingExpense();

        mockCategory(activeCategory(CategoryType.EXPENSE));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(AccountNotFoundException.class);

        verifyNoInteractions(accountBalanceService);
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectCategoryOwnedByAnotherUser() {
        CreateTransactionRequest request = completedExpense(PaymentMethod.DEBIT);

        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(CategoryNotFoundException.class);

        verifyNoInteractions(accountBalanceService);
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectCreditCardPaymentMethodInThisTask() {
        CreateTransactionRequest request = completedExpense(PaymentMethod.CREDIT_CARD);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Transações de cartão de crédito ainda não são suportadas");

        verifyNoInteractions(categoryRepository, accountBalanceService);
    }

    @Test
    void shouldRejectTransferTransactionTypeInThisTask() {
        CreateTransactionRequest request = request(
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                PaymentMethod.TRANSFER,
                ACCOUNT_ID,
                UUID.randomUUID(),
                EFFECTIVE_DATE
        );

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InvalidTransactionException.class);

        verifyNoInteractions(categoryRepository, accountBalanceService);
    }

    @Test
    void shouldRejectCompletedTransactionWithoutEffectiveDate() {
        CreateTransactionRequest request = request(
                TransactionType.EXPENSE,
                TransactionStatus.COMPLETED,
                PaymentMethod.DEBIT,
                ACCOUNT_ID,
                null,
                null
        );

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Transação concluída deve possuir data efetiva");

        verifyNoInteractions(categoryRepository, accountBalanceService);
    }

    @Test
    void shouldRejectPendingTransactionWithEffectiveDate() {
        CreateTransactionRequest request = request(
                TransactionType.EXPENSE,
                TransactionStatus.PENDING,
                PaymentMethod.PIX,
                ACCOUNT_ID,
                null,
                EFFECTIVE_DATE
        );

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Transação pendente não deve possuir data efetiva");

        verifyNoInteractions(categoryRepository, accountBalanceService);
    }

    @Test
    void shouldFindTransactionOwnedByAuthenticatedUser() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setUserId(USER_ID);
        TransactionResponse response = responseFrom(completedExpense(PaymentMethod.PIX));

        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponse result = transactionService.findById(TRANSACTION_ID);

        assertThat(result).isEqualTo(response);
        verify(transactionRepository).findByIdAndUserId(TRANSACTION_ID, USER_ID);
    }

    @Test
    void shouldHideTransactionOwnedByAnotherUser() {
        when(transactionRepository.findByIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(TRANSACTION_ID))
                .isInstanceOf(TransactionNotFoundException.class);

        verifyNoInteractions(transactionMapper);
    }

    private CreateTransactionRequest completedIncome(PaymentMethod paymentMethod) {
        return request(
                TransactionType.INCOME,
                TransactionStatus.COMPLETED,
                paymentMethod,
                null,
                ACCOUNT_ID,
                EFFECTIVE_DATE
        );
    }

    private CreateTransactionRequest completedExpense(PaymentMethod paymentMethod) {
        return request(
                TransactionType.EXPENSE,
                TransactionStatus.COMPLETED,
                paymentMethod,
                ACCOUNT_ID,
                null,
                EFFECTIVE_DATE
        );
    }

    private CreateTransactionRequest pendingExpense() {
        return request(
                TransactionType.EXPENSE,
                TransactionStatus.PENDING,
                PaymentMethod.PIX,
                ACCOUNT_ID,
                null,
                null
        );
    }

    private CreateTransactionRequest request(
            TransactionType type,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            UUID sourceAccountId,
            UUID destinationAccountId,
            LocalDate effectiveDate
    ) {
        return new CreateTransactionRequest(
                "Transaction test",
                AMOUNT,
                COMPETENCE_DATE,
                effectiveDate,
                null,
                type,
                status,
                paymentMethod,
                sourceAccountId,
                destinationAccountId,
                CATEGORY_ID,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Category activeCategory(CategoryType type) {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setUserId(USER_ID);
        category.setType(type);
        category.setStatus(CategoryStatus.ACTIVE);
        return category;
    }

    private Account activeAccount() {
        Account account = new Account();
        account.setId(ACCOUNT_ID);
        account.setUserId(USER_ID);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private Transaction transactionFrom(CreateTransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setCompetenceDate(request.competenceDate());
        transaction.setEffectiveDate(request.effectiveDate());
        transaction.setType(request.type());
        transaction.setStatus(request.status());
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setSourceAccountId(request.sourceAccountId());
        transaction.setDestinationAccountId(request.destinationAccountId());
        transaction.setCategoryId(request.categoryId());
        return transaction;
    }

    private TransactionResponse responseFrom(CreateTransactionRequest request) {
        return new TransactionResponse(
                TRANSACTION_ID,
                request.description(),
                request.amount(),
                request.competenceDate(),
                request.effectiveDate(),
                request.dueDate(),
                request.type(),
                request.status(),
                request.paymentMethod(),
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.categoryId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private void mockCategory(Category category) {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(category));
    }

    private void mockSuccessfulPersistence(
            CreateTransactionRequest request,
            Transaction transaction,
            TransactionResponse response
    ) {
        when(transactionMapper.toEntity(request)).thenReturn(transaction);
        when(transactionRepository.saveAndFlush(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);
    }
}
