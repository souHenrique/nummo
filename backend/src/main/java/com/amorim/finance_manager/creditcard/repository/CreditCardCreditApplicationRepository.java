package com.amorim.finance_manager.creditcard.repository;

import com.amorim.finance_manager.creditcard.entity.CreditCardCredit;
import com.amorim.finance_manager.creditcard.entity.CreditCardCreditApplication;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CreditCardCreditApplicationRepository extends JpaRepository<CreditCardCreditApplication, UUID> {

    @Query("""
            SELECT COALESCE(SUM(application.amount), 0)
            FROM CreditCardCreditApplication application
            WHERE application.invoiceId = :invoiceId
            """)
    BigDecimal sumAppliedAmount(
            @Param("invoiceId") UUID invoiceId
    );

    List<CreditCardCreditApplication> findAllByInvoiceId(UUID invoiceId);
}
