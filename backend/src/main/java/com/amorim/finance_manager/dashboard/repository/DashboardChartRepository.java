package com.amorim.finance_manager.dashboard.repository;

import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DashboardChartRepository extends Repository<Transaction, UUID> {

    @Query("""
            select new com.amorim.finance_manager.report.projection.CompetenceAggregate(
                transaction.type,
                transaction.categoryId,
                sum(transaction.amount)
            )
            from Transaction transaction
            where transaction.userId = :userId
              and transaction.status = :status
              and transaction.type in :includedTypes
              and transaction.effectiveDate >= :startDate
              and transaction.effectiveDate <= :endDate
            group by transaction.type, transaction.categoryId
            """)
    List<CompetenceAggregate> aggregateDirectTransactions(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") TransactionStatus status,
            @Param("includedTypes") Collection<TransactionType> includedTypes
    );

    @Query("""
            select new com.amorim.finance_manager.report.projection.CompetenceAggregate(
                transaction.type,
                transaction.categoryId,
                sum(transaction.amount)
            )
            from Transaction transaction
            join Invoice invoice on invoice.id = transaction.invoiceId
            where transaction.userId = :userId
              and transaction.status = :status
              and transaction.type = :type
              and invoice.dueDate >= :startDate
              and invoice.dueDate <= :endDate
            group by transaction.type, transaction.categoryId
            """)
    List<CompetenceAggregate> aggregateCreditCardPurchasesByDueDate(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type
    );

    @Query("""
            select new com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate(
                month(transaction.effectiveDate),
                transaction.type,
                sum(transaction.amount)
            )
            from Transaction transaction
            where transaction.userId = :userId
              and transaction.status = :status
              and transaction.type in :includedTypes
              and transaction.effectiveDate >= :startDate
              and transaction.effectiveDate <= :endDate
            group by month(transaction.effectiveDate), transaction.type
            order by month(transaction.effectiveDate)
            """)
    List<AnnualCashFlowAggregate> aggregateDirectTransactionsByMonth(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") TransactionStatus status,
            @Param("includedTypes") Collection<TransactionType> includedTypes
    );

    @Query("""
            select new com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate(
                month(invoice.dueDate),
                transaction.type,
                sum(transaction.amount)
            )
            from Transaction transaction
            join Invoice invoice on invoice.id = transaction.invoiceId
            where transaction.userId = :userId
              and transaction.status = :status
              and transaction.type = :type
              and invoice.dueDate >= :startDate
              and invoice.dueDate <= :endDate
            group by month(invoice.dueDate), transaction.type
            order by month(invoice.dueDate)
            """)
    List<AnnualCashFlowAggregate> aggregateCreditCardPurchasesByDueMonth(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type
    );
}
