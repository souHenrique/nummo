package com.amorim.finance_manager.transaction.repository;

import com.amorim.finance_manager.budget.projection.BudgetSpendAggregate;
import com.amorim.finance_manager.transaction.projection.InstallmentGroupTotal;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    @Query("select count(bill) > 0 from Bill bill where bill.paymentTransactionId = :transactionId and bill.userId = :userId")
    boolean isBillPayment(@Param("transactionId") UUID transactionId, @Param("userId") UUID userId);

    List<Transaction> findAllByInvoiceIdAndUserIdOrderByCompetenceDateAscCreatedAtAsc(UUID invoiceId, UUID userId);

    List<Transaction> findAllByInstallmentGroupIdAndCreditCardIdAndUserIdOrderByInstallmentNumberAsc(
            UUID installmentGroupId,
            UUID creditCardId,
            UUID userId
    );

    @Query("""
        select new com.amorim.finance_manager.transaction.projection.InstallmentGroupTotal(
            transaction.installmentGroupId,
            sum(transaction.amount)
        )
        from Transaction transaction
        where transaction.userId = :userId
          and transaction.installmentGroupId in :installmentGroupIds
        group by transaction.installmentGroupId
        """)
    List<InstallmentGroupTotal> sumByInstallmentGroupIds(
            @Param("userId") UUID userId,
            @Param("installmentGroupIds") Collection<UUID> installmentGroupIds
    );

    boolean existsByInvoiceIdAndUserIdAndTypeAndStatus(
            UUID invoiceId,
            UUID userId,
            TransactionType type,
            TransactionStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(transaction.amount), 0)
        FROM Transaction transaction
        WHERE transaction.userId = :userId
            AND transaction.categoryId = :categoryId
            AND transaction.competenceDate >= :periodStart
            AND transaction.competenceDate < :periodEndExclusive
            AND transaction.type IN :includedTypes
            AND transaction.status <> :excludedStatus
        """)
    BigDecimal sumForBudget(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEndExclusive") LocalDate periodEndExclusive,
            @Param("includedTypes") Collection<TransactionType> includedTypes,
            @Param("excludedStatus") TransactionStatus excludedStatus
    );

    @Query("""
        select new com.amorim.finance_manager.budget.projection.BudgetSpendAggregate(
            transaction.categoryId,
            sum(transaction.amount)
        )
        from Transaction transaction
        where transaction.userId = :userId
          and transaction.categoryId in :categoryIds
          and transaction.competenceDate >= :periodStart
          and transaction.competenceDate < :periodEndExclusive
          and transaction.type in :includedTypes
          and transaction.status <> :excludedStatus
        group by transaction.categoryId
        """)
    List<BudgetSpendAggregate> sumByCategoryForBudgets(
            @Param("userId") UUID userId,
            @Param("categoryIds") Collection<UUID> categoryIds,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEndExclusive") LocalDate periodEndExclusive,
            @Param("includedTypes") Collection<TransactionType> includedTypes,
            @Param("excludedStatus") TransactionStatus excludedStatus
    );
}
