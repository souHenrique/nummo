package com.amorim.finance_manager.bill.service;

import com.amorim.finance_manager.bill.dto.*;
import com.amorim.finance_manager.bill.entity.Bill;
import com.amorim.finance_manager.bill.entity.BillStatus;
import com.amorim.finance_manager.bill.repository.BillRepository;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.shared.exception.*;
import com.amorim.finance_manager.transaction.dto.CreateTransactionRequest;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.service.TransactionService;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillService {
    private final BillRepository billRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    @Transactional
    public List<BillResponse> create(CreateBillRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        validateCategory(request.categoryId(), userId);
        validateDate(request.firstDueDate());
        if (request.installmentCount() == null || request.installmentCount() < 1
                || request.installmentCount() > 600) {
            throw new InvalidBillException("Informe entre 1 e 600 parcelas");
        }
        LocalDate lastDueDate = request.firstDueDate().plusMonths(request.installmentCount() - 1L);
        validateDate(lastDueDate);
        UUID seriesId = UUID.randomUUID();
        List<Bill> bills = new ArrayList<>();
        for (int index = 0; index < request.installmentCount(); index++) {
            Bill bill = new Bill();
            bill.setUserId(userId);
            bill.setCategoryId(request.categoryId());
            bill.setDescription(request.description());
            bill.setAmount(request.amount());
            // Always use the original day: Jan 31 -> Feb 28 -> Mar 31.
            bill.setDueDate(request.firstDueDate().plusMonths(index));
            bill.setSeriesId(seriesId);
            bill.setInstallmentNumber(index + 1);
            bill.setInstallmentCount(request.installmentCount());
            bills.add(bill);
        }
        return billRepository.saveAllAndFlush(bills).stream().map(BillResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<BillResponse> list(int year, int month, BillStatus status, int page, int size) {
        if (year < 1900 || year > 9999 || month < 1 || month > 12 || page < 0 || size < 1) {
            throw new InvalidBillException("Período ou paginação inválidos");
        }
        YearMonth period = YearMonth.of(year, month);
        return billRepository.findByPeriod(currentUserService.getCurrentUserId(),
                period.atDay(1), period.atEndOfMonth(), status,
                PageRequest.of(page, Math.min(size, 100), Sort.by("dueDate").and(Sort.by("id"))))
                .map(BillResponse::from);
    }

    @Transactional(readOnly = true)
    public BillResponse findById(UUID id) {
        return BillResponse.from(billRepository.findByIdAndUserId(id, currentUserService.getCurrentUserId())
                .orElseThrow(BillNotFoundException::new));
    }

    @Transactional
    public BillResponse update(UUID id, UpdateBillRequest request) {
        Bill bill = pendingBill(id, request.expectedVersion());
        validateCategory(request.categoryId(), bill.getUserId());
        validateDate(request.dueDate());
        bill.setDescription(request.description());
        bill.setAmount(request.amount());
        bill.setDueDate(request.dueDate());
        bill.setCategoryId(request.categoryId());
        return BillResponse.from(billRepository.saveAndFlush(bill));
    }

    @Transactional
    public BillResponse cancel(UUID id, BillVersionRequest request) {
        Bill bill = pendingBill(id, request.expectedVersion());
        bill.setStatus(BillStatus.CANCELLED);
        return BillResponse.from(billRepository.saveAndFlush(bill));
    }

    @Transactional
    public BillResponse pay(UUID id, PayBillRequest request) {
        Bill bill = pendingBill(id, request.expectedVersion());
        if (request.paymentDate() == null || request.paymentDate().isAfter(LocalDate.now())) {
            throw new InvalidBillException("A data de pagamento não pode estar no futuro");
        }
        String description = bill.getDescription();
        if (bill.getInstallmentCount() > 1) {
            String suffix = " (" + bill.getInstallmentNumber() + "/" + bill.getInstallmentCount() + ")";
            description = description.substring(0, Math.min(description.length(), 255 - suffix.length())) + suffix;
        }
        // Reuse the normal expense flow so balances and reports follow the payment date.
        var payment = transactionService.create(new CreateTransactionRequest(
                description, bill.getAmount(), bill.getDueDate(), request.paymentDate(), bill.getDueDate(),
                TransactionType.EXPENSE, TransactionStatus.COMPLETED, PaymentMethod.BOLETO,
                request.sourceAccountId(), null, bill.getCategoryId(), null, null, null, null, null));
        bill.setPaymentTransactionId(payment.id());
        bill.setStatus(BillStatus.PAID);
        return BillResponse.from(billRepository.saveAndFlush(bill));
    }

    private Bill pendingBill(UUID id, Long expectedVersion) {
        Bill bill = billRepository.findOwnedForUpdate(id, currentUserService.getCurrentUserId())
                .orElseThrow(BillNotFoundException::new);
        if (!Objects.equals(bill.getVersion(), expectedVersion)) {
            throw new BillConflictException("Este boleto foi alterado. Recarregue os dados e confirme novamente.");
        }
        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BillConflictException("Somente boletos pendentes podem ser alterados ou pagos");
        }
        return bill;
    }

    private void validateCategory(UUID categoryId, UUID userId) {
        var category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
        if (category.getType() != CategoryType.EXPENSE || category.getStatus() != CategoryStatus.ACTIVE) {
            throw new InvalidBillException("Selecione uma categoria de despesa ativa");
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null || date.getYear() < 1900 || date.getYear() > 9999) {
            throw new InvalidBillException("Informe um vencimento entre 1900 e 9999");
        }
    }
}
