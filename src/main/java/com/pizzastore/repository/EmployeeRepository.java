package com.pizzastore.repository;

import com.pizzastore.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // Tìm nhân viên dựa trên username của tài khoản liên kết
    Optional<Employee> findByAccount_Username(String username);
}