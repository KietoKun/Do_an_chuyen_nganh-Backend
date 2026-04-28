package com.pizzastore.repository;

import com.pizzastore.entity.Employee;
import com.pizzastore.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByAccount_Username(String username);
    List<Employee> findByBranch_Id(Long branchId);
    List<Employee> findByBranch_IdAndAccount_Role(Long branchId, RoleName role);
}
