package com.pizzastore.repository;

import com.pizzastore.entity.AttendanceRecord;
import com.pizzastore.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findFirstByEmployee_IdAndStatusOrderByCheckInTimeDesc(Long employeeId, AttendanceStatus status);

    List<AttendanceRecord> findByEmployee_IdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
            Long employeeId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<AttendanceRecord> findByBranch_IdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
            Long branchId,
            LocalDateTime from,
            LocalDateTime to
    );
}
