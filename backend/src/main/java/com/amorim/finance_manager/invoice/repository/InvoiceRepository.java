package com.amorim.finance_manager.invoice.repository;

import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.report.projection.InvoiceDueMonthAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByCreditCardIdAndReferenceMonthAndReferenceYear(
            UUID creditCardId,
            Integer referenceMonth,
            Integer referenceYear);

    @Query("""
        SELECT invoice
        FROM Invoice invoice
        WHERE invoice.id = :invoiceId
          AND invoice.creditCardId IN (
              SELECT card.id
              FROM CreditCard card
              WHERE card.userId = :userId
          )
        """)
    Optional<Invoice> findOwnedById(
            @Param("invoiceId") UUID invoiceId,
            @Param("userId") UUID userId
    );

    @Query("""
        select coalesce(sum(invoice.totalAmount), 0)
        from Invoice invoice
        where invoice.status in :statuses
          and invoice.creditCardId in (
              select card.id
              from CreditCard card
              where card.userId = :userId
          )
        """)
    BigDecimal sumTotalAmountOwnedByUserIdAndStatusIn(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );

    @Query("""
        select coalesce(sum(invoice.totalAmount), 0)
        from Invoice invoice
        where invoice.status in :statuses
          and invoice.referenceMonth = :referenceMonth
          and invoice.referenceYear = :referenceYear
          and invoice.creditCardId in (
              select card.id
              from CreditCard card
              where card.userId = :userId
          )
        """)
    BigDecimal sumTotalAmountOwnedByUserIdAndReferencePeriodAndStatusIn(
            @Param("userId") UUID userId,
            @Param("referenceMonth") int referenceMonth,
            @Param("referenceYear") int referenceYear,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );

    @Query("""
        select coalesce(sum(invoice.totalAmount), 0)
        from Invoice invoice
        where invoice.status in :statuses
          and invoice.dueDate >= :startDate
          and invoice.dueDate <= :endDate
          and invoice.creditCardId in (select card.id from CreditCard card where card.userId = :userId)
        """)
    BigDecimal sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );

    @Query("""
        select new com.amorim.finance_manager.report.projection.InvoiceDueMonthAggregate(
            month(invoice.dueDate), sum(invoice.totalAmount)
        )
        from Invoice invoice
        where invoice.status in :statuses
          and invoice.dueDate >= :startDate
          and invoice.dueDate <= :endDate
          and invoice.creditCardId in (select card.id from CreditCard card where card.userId = :userId)
        group by month(invoice.dueDate)
        order by month(invoice.dueDate)
        """)
    java.util.List<InvoiceDueMonthAggregate> sumTotalAmountByDueMonthOwnedByUserIdAndStatusIn(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<InvoiceStatus> statuses
    );
}
