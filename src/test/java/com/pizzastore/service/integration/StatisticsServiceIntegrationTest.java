package com.pizzastore.service.integration;

import com.pizzastore.dto.DailyRevenueResponse;
import com.pizzastore.dto.RevenueSummaryResponse;
import com.pizzastore.dto.TopSellingDishResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Customer;
import com.pizzastore.entity.Dish;
import com.pizzastore.entity.DishVariant;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.Order;
import com.pizzastore.entity.OrderDetail;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.DishRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.OrderDetailRepository;
import com.pizzastore.repository.OrderRepository;
import com.pizzastore.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(StatisticsService.class)
class StatisticsServiceIntegrationTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Test
    void getRevenueSummaryShouldAggregateCompletedOrdersAcrossAllBranchesForSuperAdmin() {
        Fixture fixture = seedFixture();

        RevenueSummaryResponse summary = statisticsService.getRevenueSummary(
                fixture.superAdminUsername,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertEquals(3L, summary.getOrderCount());
        assertEquals(335.0, summary.getRevenue(), 0.0001);
    }

    @Test
    void getTopSellingDishesShouldRespectManagerBranchScope() {
        Fixture fixture = seedFixture();

        List<TopSellingDishResponse> responses = statisticsService.getTopSellingDishes(
                fixture.managerUsername,
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                10
        );

        assertEquals(1, responses.size());
        assertEquals("Pizza Hai San", responses.get(0).getDishName());
        assertEquals(3L, responses.get(0).getQuantitySold());
        assertEquals(135.0, responses.get(0).getRevenue(), 0.0001);
    }

    @Test
    void getDailyRevenueShouldFillMissingDatesForManagerBranch() {
        Fixture fixture = seedFixture();

        List<DailyRevenueResponse> responses = statisticsService.getDailyRevenue(
                fixture.managerUsername,
                null,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 12)
        );

        assertEquals(3, responses.size());
        assertEquals(LocalDate.of(2026, 1, 10), responses.get(0).getDate());
        assertEquals(90.0, responses.get(0).getRevenue(), 0.0001);
        assertEquals(1L, responses.get(0).getOrderCount());
        assertEquals(LocalDate.of(2026, 1, 11), responses.get(1).getDate());
        assertEquals(0.0, responses.get(1).getRevenue(), 0.0001);
        assertEquals(0L, responses.get(1).getOrderCount());
        assertEquals(LocalDate.of(2026, 1, 12), responses.get(2).getDate());
        assertEquals(45.0, responses.get(2).getRevenue(), 0.0001);
        assertEquals(1L, responses.get(2).getOrderCount());
    }

    @Test
    void getRevenueSummaryShouldRejectManagerRequestingAnotherBranch() {
        Fixture fixture = seedFixture();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> statisticsService.getRevenueSummary(
                        fixture.managerUsername,
                        fixture.branchB.getId(),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)
                ));

        assertNotNull(ex.getMessage());
    }

    private Fixture seedFixture() {
        Fixture fixture = new Fixture();

        fixture.branchA = persistBranch("Branch A");
        fixture.branchB = persistBranch("Branch B");

        fixture.superAdminUsername = "admin";
        Account superAdmin = new Account();
        superAdmin.setUsername(fixture.superAdminUsername);
        superAdmin.setPassword("secret");
        superAdmin.setRole(RoleName.SUPER_ADMIN);
        entityManager.persistAndFlush(superAdmin);

        fixture.managerUsername = "manager-a";
        Employee manager = new Employee();
        manager.setFullName("Manager A");
        manager.setPhoneNumber(fixture.managerUsername);
        manager.setAddress("HCM");
        manager.setEmail("manager@example.com");
        manager.setPosition("Manager");
        manager.setBranch(fixture.branchA);
        Account managerAccount = new Account();
        managerAccount.setUsername(fixture.managerUsername);
        managerAccount.setPassword("secret");
        managerAccount.setRole(RoleName.MANAGER);
        manager.setAccount(managerAccount);
        entityManager.persistAndFlush(manager);

        fixture.customerA = persistCustomer("customer-a", "Customer A");
        fixture.customerB = persistCustomer("customer-b", "Customer B");

        fixture.dishA = persistDish("Pizza Hai San");
        fixture.dishB = persistDish("Pizza Pepperoni");

        fixture.variantA = fixture.dishA.getVariants().get(0);
        fixture.variantB = fixture.dishB.getVariants().get(0);

        persistCompletedOrder(fixture.branchA, fixture.customerA, fixture.variantA,
                LocalDateTime.of(2026, 1, 10, 10, 0), 2, 90.0);
        persistCompletedOrder(fixture.branchB, fixture.customerB, fixture.variantB,
                LocalDateTime.of(2026, 1, 11, 11, 0), 1, 200.0);
        persistCompletedOrder(fixture.branchA, fixture.customerA, fixture.variantA,
                LocalDateTime.of(2026, 1, 12, 12, 0), 1, 45.0);

        entityManager.flush();
        entityManager.clear();
        return fixture;
    }

    private Branch persistBranch(String name) {
        Branch branch = new Branch();
        branch.setName(name);
        branch.setAddress(name + " address");
        branch.setLatitude(10.0);
        branch.setLongitude(20.0);
        return entityManager.persistAndFlush(branch);
    }

    private Customer persistCustomer(String username, String fullName) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("secret");
        account.setRole(RoleName.CUSTOMER);

        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setPhoneNumber(username);
        customer.setAddress("Address");
        customer.setEmail(username + "@example.com");
        customer.setAccount(account);
        return entityManager.persistAndFlush(customer);
    }

    private Dish persistDish(String name) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(name + " description");
        dish.setAvailable(true);

        DishVariant variant = new DishVariant();
        variant.setSize("M");
        variant.setPrice(name.equals("Pizza Hai San") ? 120000.0 : 200000.0);
        dish.addVariant(variant);

        return entityManager.persistAndFlush(dish);
    }

    private Order persistCompletedOrder(Branch branch,
                                        Customer customer,
                                        DishVariant variant,
                                        LocalDateTime orderTime,
                                        int quantity,
                                        double subTotal) {
        OrderDetail detail = new OrderDetail();
        detail.setDishVariant(variant);
        detail.setQuantity(quantity);
        detail.setUnitPrice(subTotal / quantity);
        detail.setSubTotal(subTotal);

        Order order = new Order();
        order.setBranch(branch);
        order.setCustomer(customer);
        order.setOrderTime(orderTime);
        order.setStatus(OrderStatus.COMPLETED);
        order.setTotalPrice(subTotal);
        order.setFinalTotalPrice(subTotal);
        order.addDetail(detail);

        return entityManager.persistAndFlush(order);
    }

    private static class Fixture {
        private Branch branchA;
        private Branch branchB;
        private Customer customerA;
        private Customer customerB;
        private Dish dishA;
        private Dish dishB;
        private DishVariant variantA;
        private DishVariant variantB;
        private String superAdminUsername;
        private String managerUsername;
    }
}
