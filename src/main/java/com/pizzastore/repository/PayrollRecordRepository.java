package com.pizzastore.repository;

import com.pizzastore.entity.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    Optional<PayrollRecord> findByEmployee_IdAndPeriodStartAndPeriodEnd(Long employeeId, LocalDate periodStart, LocalDate periodEnd);

    List<PayrollRecord> findByBranch_IdAndPeriodStartAndPeriodEnd(
            Long branchId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    List<PayrollRecord> findByPeriodStartAndPeriodEnd(
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
