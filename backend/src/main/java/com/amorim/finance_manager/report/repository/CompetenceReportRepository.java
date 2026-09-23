package com.amorim.finance_manager.report.repository;

import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CompetenceReportRepository extends Repository<Transaction, UUID> {

    @Query("""
            select new com.amorim.finance_manager.report.projection.CompetenceAggregate(
                transaction.type,
                transaction.categoryId,
                sum(transaction.amount)
            )
            from Transaction transaction
            where transaction.userId = :userId
              and transaction.status = :status
              and transaction.competenceDate >= :startDate
              and transaction.competenceDate <= :endDate
              and transaction.type in :includedTypes
            group by transaction.type, transaction.categoryId
            """)
    List<CompetenceAggregate> aggregate(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") TransactionStatus status,
            @Param("includedTypes") Collection<TransactionType> includedTypes
    );

    @Query("""
            select new com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate(
                month(transaction.competenceDate),
                transaction.type,
                sum(transaction.amount)
            )
            from Transaction transaction
            where transaction.userId = :userId
              and transaction.status = :status
              and transaction.competenceDate >= :startDate
              and transaction.competenceDate <= :endDate
              and transaction.type in :includedTypes
            group by month(transaction.competenceDate), transaction.type
            order by month(transaction.competenceDate)
            """)
    List<AnnualCashFlowAggregate> aggregateByMonth(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") TransactionStatus status,
            @Param("includedTypes") Collection<TransactionType> includedTypes
    );

    @Query("""
            select coalesce(sum(transaction.amount), 0)
            from Transaction transaction
            join Invoice invoice on invoice.id = transaction.invoiceId
            where transaction.userId = :userId
              and invoice.creditCardId = transaction.creditCardId
              and transaction.status = :status
              and transaction.type = :type
              and invoice.referenceYear = :referenceYear
              and invoice.referenceMonth = :referenceMonth
            """)
    BigDecimal sumCreditCardPurchasesByInvoicePeriod(
            @Param("userId") UUID userId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("referenceYear") Integer referenceYear,
            @Param("referenceMonth") Integer referenceMonth
    );
}
