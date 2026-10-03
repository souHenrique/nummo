package com.amorim.finance_manager.bill.service;

import com.amorim.finance_manager.bill.dto.*;
import com.amorim.finance_manager.bill.entity.Bill;
import com.amorim.finance_manager.bill.entity.BillStatus;
import com.amorim.finance_manager.bill.repository.BillRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.*;
import com.amorim.finance_manager.transaction.dto.CreateTransactionRequest;
import com.amorim.finance_manager.transaction.dto.TransactionResponse;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.service.TransactionService;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {
    @Mock BillRepository repository;
    @Mock CategoryRepository categories;
    @Mock TransactionService transactions;
    @Mock CurrentUserService currentUser;
    BillService service;
    UUID userId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BillService(repository, categories, transactions, currentUser);
        when(currentUser.getCurrentUserId()).thenReturn(userId);
    }

    private void activeCategory() {
        Category category = new Category();
        category.setType(CategoryType.EXPENSE);
        category.setStatus(CategoryStatus.ACTIVE);
        when(categories.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));
    }

    private Bill pending() {
        Bill bill = new Bill();
        bill.setId(UUID.randomUUID());
        bill.setUserId(userId);
        bill.setCategoryId(categoryId);
        bill.setDescription("Financiamento de Jesse");
        bill.setAmount(new BigDecimal("100.25"));
        bill.setDueDate(LocalDate.of(2026, 9, 30));
        bill.setSeriesId(UUID.randomUUID());
        bill.setInstallmentNumber(1);
        bill.setInstallmentCount(36);
        bill.setVersion(0L);
        when(repository.findOwnedForUpdate(bill.getId(), userId)).thenReturn(Optional.of(bill));
        return bill;
    }

    @Test
    void creates36IndependentMonthlyBillsWithoutTransactions() {
        activeCategory();
        when(repository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(new CreateBillRequest("Moto de Jesse", new BigDecimal("400.50"),
                LocalDate.of(2026, 1, 31), 36, categoryId));
        assertThat(result).hasSize(36);
        assertThat(result.get(0).dueDate()).isEqualTo("2026-01-31");
        assertThat(result.get(1).dueDate()).isEqualTo("2026-02-28");
        assertThat(result.get(2).dueDate()).isEqualTo("2026-03-31");
        assertThat(result.get(35).dueDate()).isEqualTo("2028-12-31");
        assertThat(result).extracting(BillResponse::seriesId).containsOnly(result.getFirst().seriesId());
        assertThat(result).extracting(BillResponse::amount).containsOnly(new BigDecimal("400.50"));
        assertThat(result).allMatch(bill -> bill.status() == BillStatus.PENDING && bill.paymentTransactionId() == null);
        verifyNoInteractions(transactions);
    }

    @Test
    void createsSingleBillInSpecifiedMonth() {
        activeCategory();
        when(repository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(new CreateBillRequest("Boleto de Walter", new BigDecimal("90.01"),
                LocalDate.of(2027, 3, 10), 1, categoryId));
        assertThat(result).singleElement().satisfies(bill -> {
            assertThat(bill.dueDate()).isEqualTo("2027-03-10");
            assertThat(bill.installmentNumber()).isEqualTo(1);
        });
    }

    @Test
    void preservesDayAcrossLeapYear() {
        activeCategory();
        when(repository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(new CreateBillRequest("Boleto", BigDecimal.TEN,
                LocalDate.of(2028, 1, 31), 3, categoryId));
        assertThat(result.get(1).dueDate()).isEqualTo("2028-02-29");
        assertThat(result.get(2).dueDate()).isEqualTo("2028-03-31");
    }

    @Test
    void rejectsForeignCategory() {
        when(categories.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(new CreateBillRequest("Boleto", BigDecimal.TEN,
                LocalDate.now(), 1, categoryId))).isInstanceOf(CategoryNotFoundException.class);
        verifyNoInteractions(repository, transactions);
    }

    @Test
    void rejectsIncomeCategory() {
        Category category = new Category();
        category.setType(CategoryType.INCOME);
        category.setStatus(CategoryStatus.ACTIVE);
        when(categories.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));
        assertThatThrownBy(() -> service.create(new CreateBillRequest("Boleto", BigDecimal.TEN,
                LocalDate.now(), 1, categoryId))).isInstanceOf(InvalidBillException.class);
    }

    @Test
    void limitsNumberOfInstallments() {
        activeCategory();
        assertThatThrownBy(() -> service.create(new CreateBillRequest("Boleto", BigDecimal.TEN,
                LocalDate.now(), 601, categoryId))).isInstanceOf(InvalidBillException.class);
        verifyNoInteractions(repository, transactions);
    }

    @Test
    void listsOwnedBillsInDueMonthWithBoundedPagination() {
        when(repository.findByPeriod(eq(userId), any(), any(), eq(BillStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of()));
        service.list(2026, 2, BillStatus.PENDING, 0, 999);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByPeriod(eq(userId), eq(LocalDate.of(2026, 2, 1)),
                eq(LocalDate.of(2026, 2, 28)), eq(BillStatus.PENDING), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void paysAsNormalExpenseOnPaymentDateWithoutCardOrInvoice() {
        Bill bill = pending();
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        TransactionResponse response = mock(TransactionResponse.class);
        when(response.id()).thenReturn(transactionId);
        when(transactions.create(any())).thenReturn(response);
        when(repository.saveAndFlush(bill)).thenReturn(bill);
        var result = service.pay(bill.getId(), new PayBillRequest(accountId, LocalDate.of(2026, 9, 29), 0L));
        ArgumentCaptor<CreateTransactionRequest> request = ArgumentCaptor.forClass(CreateTransactionRequest.class);
        verify(transactions).create(request.capture());
        assertThat(request.getValue().amount()).isEqualByComparingTo("100.25");
        assertThat(request.getValue().sourceAccountId()).isEqualTo(accountId);
        assertThat(request.getValue().effectiveDate()).isEqualTo("2026-09-29");
        assertThat(request.getValue().competenceDate()).isEqualTo(bill.getDueDate());
        assertThat(request.getValue().type()).isEqualTo(TransactionType.EXPENSE);
        assertThat(request.getValue().status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(request.getValue().paymentMethod()).isEqualTo(PaymentMethod.BOLETO);
        assertThat(request.getValue().invoiceId()).isNull();
        assertThat(request.getValue().creditCardId()).isNull();
        assertThat(request.getValue().installmentGroupId()).isNull();
        assertThat(result.status()).isEqualTo(BillStatus.PAID);
        assertThat(result.paymentTransactionId()).isEqualTo(transactionId);
    }

    @Test
    void rejectsAlreadyPaidBillWithoutCreatingAnotherExpense() {
        Bill bill = pending();
        bill.setStatus(BillStatus.PAID);
        assertThatThrownBy(() -> service.pay(bill.getId(), new PayBillRequest(UUID.randomUUID(), LocalDate.now(), 0L)))
                .isInstanceOf(BillConflictException.class);
        verifyNoInteractions(transactions);
    }

    @Test
    void rejectsStaleVersionWithoutCreatingExpense() {
        Bill bill = pending();
        assertThatThrownBy(() -> service.pay(bill.getId(), new PayBillRequest(UUID.randomUUID(), LocalDate.now(), 1L)))
                .isInstanceOf(BillConflictException.class);
        verifyNoInteractions(transactions);
    }

    @Test
    void rejectsFuturePaymentDate() {
        Bill bill = pending();
        assertThatThrownBy(() -> service.pay(bill.getId(), new PayBillRequest(UUID.randomUUID(), LocalDate.now().plusDays(1), 0L)))
                .isInstanceOf(InvalidBillException.class);
        verifyNoInteractions(transactions);
    }

    @Test
    void failedExpenseLeavesBillPending() {
        Bill bill = pending();
        when(transactions.create(any())).thenThrow(new AccountNotFoundException());
        assertThatThrownBy(() -> service.pay(bill.getId(), new PayBillRequest(UUID.randomUUID(), LocalDate.now(), 0L)))
                .isInstanceOf(AccountNotFoundException.class);
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PENDING);
        assertThat(bill.getPaymentTransactionId()).isNull();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void editsOnlyOnePendingInstallment() {
        Bill bill = pending();
        activeCategory();
        when(repository.saveAndFlush(bill)).thenReturn(bill);
        var result = service.update(bill.getId(), new UpdateBillRequest("Boleto de Saul", new BigDecimal("120.25"),
                LocalDate.of(2026, 10, 10), categoryId, 0L));
        assertThat(result.amount()).isEqualByComparingTo("120.25");
        assertThat(result.dueDate()).isEqualTo("2026-10-10");
        assertThat(result.installmentCount()).isEqualTo(36);
        verify(repository).saveAndFlush(bill);
        verifyNoInteractions(transactions);
    }

    @Test
    void cancelsWithoutRemovingHistoryOrDebitingAccount() {
        Bill bill = pending();
        when(repository.saveAndFlush(bill)).thenReturn(bill);
        assertThat(service.cancel(bill.getId(), new BillVersionRequest(0L)).status()).isEqualTo(BillStatus.CANCELLED);
        verify(repository, never()).delete(any());
        verifyNoInteractions(transactions);
    }

    @Test
    void mutationsCannotAccessAnotherUsersBill() {
        UUID id = UUID.randomUUID();
        when(repository.findOwnedForUpdate(id, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(id, new BillVersionRequest(0L))).isInstanceOf(BillNotFoundException.class);
        verifyNoInteractions(transactions);
    }
}
