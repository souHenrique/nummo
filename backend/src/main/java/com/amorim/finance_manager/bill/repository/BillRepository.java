package com.amorim.finance_manager.bill.repository;

import com.amorim.finance_manager.bill.entity.Bill;
import com.amorim.finance_manager.bill.entity.BillStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {
    @Query("""
            select coalesce(sum(bill.amount), 0) from Bill bill
            where bill.userId = :userId and bill.status = :status
              and bill.dueDate between :start and :end
            """)
    BigDecimal sumAmountByDuePeriodAndStatus(@Param("userId") UUID userId,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end,
                                            @Param("status") BillStatus status);

    Optional<Bill> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bill from Bill bill where bill.id = :id and bill.userId = :userId")
    Optional<Bill> findOwnedForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select bill from Bill bill
            where bill.userId = :userId and bill.dueDate between :start and :end
            and (:status is null or bill.status = :status)
            """)
    Page<Bill> findByPeriod(@Param("userId") UUID userId, @Param("start") LocalDate start,
                           @Param("end") LocalDate end, @Param("status") BillStatus status,
                           Pageable pageable);
}
