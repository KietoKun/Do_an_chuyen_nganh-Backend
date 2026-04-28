package com.pizzastore.service;

import com.pizzastore.dto.DailyRevenueResponse;
import com.pizzastore.dto.RevenueSummaryResponse;
import com.pizzastore.dto.TopSellingDishResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Employee;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.OrderDetailRepository;
import com.pizzastore.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private static final LocalDateTime MIN_FILTER_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_FILTER_DATE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final OrderDetailRepository orderDetailRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;

    public StatisticsService(OrderDetailRepository orderDetailRepository,
                             OrderRepository orderRepository,
                             AccountRepository accountRepository,
                             EmployeeRepository employeeRepository) {
        this.orderDetailRepository = orderDetailRepository;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<TopSellingDishResponse> getTopSellingDishes(String username,
                                                            Long branchId,
                                                            LocalDate fromDate,
                                                            LocalDate toDate,
                                                            Integer limit) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        Long allowedBranchId = resolveAllowedBranchId(account, username, branchId);
        LocalDateTime from = fromDate == null ? MIN_FILTER_DATE : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? MAX_FILTER_DATE : toDate.atTime(LocalTime.MAX);
        validateDateRange(from, to);

        PageRequest pageable = PageRequest.of(0, normalizeLimit(limit));
        if (allowedBranchId == null) {
            return orderDetailRepository.findTopSellingDishesAllBranches(
                    from,
                    to,
                    OrderStatus.COMPLETED,
                    pageable
            );
        }

        return orderDetailRepository.findTopSellingDishesByBranch(
                allowedBranchId,
                from,
                to,
                OrderStatus.COMPLETED,
                pageable
        );
    }

    public RevenueSummaryResponse getRevenueSummary(String username,
                                                    Long branchId,
                                                    LocalDate fromDate,
                                                    LocalDate toDate) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        Long allowedBranchId = resolveAllowedBranchId(account, username, branchId);
        LocalDateTime from = fromDate == null ? MIN_FILTER_DATE : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? MAX_FILTER_DATE : toDate.atTime(LocalTime.MAX);
        validateDateRange(from, to);

        if (allowedBranchId == null) {
            return orderRepository.getRevenueSummaryAllBranches(
                    from,
                    to,
                    OrderStatus.COMPLETED
            );
        }

        return orderRepository.getRevenueSummaryByBranch(
                allowedBranchId,
                from,
                to,
                OrderStatus.COMPLETED
        );
    }

    public List<DailyRevenueResponse> getDailyRevenue(String username,
                                                      Long branchId,
                                                      LocalDate fromDate,
                                                      LocalDate toDate) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        Long allowedBranchId = resolveAllowedBranchId(account, username, branchId);
        LocalDate normalizedToDate = toDate == null ? LocalDate.now() : toDate;
        LocalDate normalizedFromDate = fromDate == null
                ? normalizedToDate.withDayOfMonth(1)
                : fromDate;
        LocalDateTime from = normalizedFromDate.atStartOfDay();
        LocalDateTime to = normalizedToDate.atTime(LocalTime.MAX);
        validateDateRange(from, to);

        List<Object[]> rows;
        if (allowedBranchId == null) {
            rows = orderRepository.getDailyRevenueAllBranches(
                    from,
                    to,
                    OrderStatus.COMPLETED.name()
            );
        } else {
            rows = orderRepository.getDailyRevenueByBranch(
                    allowedBranchId,
                    from,
                    to,
                    OrderStatus.COMPLETED.name()
            );
        }

        return fillMissingDates(normalizedFromDate, normalizedToDate, rows);
    }

    private Long resolveAllowedBranchId(Account account, String username, Long requestedBranchId) {
        if (account.getRole() == RoleName.SUPER_ADMIN) {
            return requestedBranchId;
        }

        if (account.getRole() == RoleName.MANAGER) {
            Employee manager = employeeRepository.findByAccount_Username(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin quản lý"));
            if (manager.getBranch() == null) {
                throw new RuntimeException("Quản lý chưa được gán chi nhánh");
            }

            Long managerBranchId = manager.getBranch().getId();
            if (requestedBranchId != null && !managerBranchId.equals(requestedBranchId)) {
                throw new RuntimeException("Quản lý chỉ được xem thống kê của chi nhánh mình");
            }
            return managerBranchId;
        }

        throw new RuntimeException("Bạn không có quyền xem thống kê");
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new RuntimeException("fromDate phải nhỏ hơn hoặc bằng toDate");
        }
    }

    private List<DailyRevenueResponse> fillMissingDates(LocalDate fromDate,
                                                        LocalDate toDate,
                                                        List<Object[]> rows) {
        Map<LocalDate, DailyRevenueResponse> revenueByDate = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            Long orderCount = ((Number) row[1]).longValue();
            Double revenue = ((Number) row[2]).doubleValue();
            revenueByDate.put(date, new DailyRevenueResponse(date, revenue, orderCount));
        }

        List<DailyRevenueResponse> result = new ArrayList<>();
        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            result.add(revenueByDate.getOrDefault(
                    current,
                    new DailyRevenueResponse(current, 0.0, 0L)
            ));
            current = current.plusDays(1);
        }
        return result;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        throw new RuntimeException("Không đọc được ngày thống kê doanh thu");
    }
}
