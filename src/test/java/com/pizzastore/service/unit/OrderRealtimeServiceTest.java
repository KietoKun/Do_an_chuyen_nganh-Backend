package com.pizzastore.service.unit;

import com.pizzastore.dto.OrderRealtimeEvent;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Customer;
import com.pizzastore.entity.Dish;
import com.pizzastore.entity.DishVariant;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.Order;
import com.pizzastore.entity.OrderDetail;
import com.pizzastore.entity.Topping;
import com.pizzastore.enums.DeliveryMethod;
import com.pizzastore.enums.OrderRealtimeEventType;
import com.pizzastore.enums.OrderSource;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.service.OrderRealtimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderRealtimeServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void publishOrderCreatedShouldSendKitchenOrderBranchAndCustomerEvents() {
        OrderRealtimeService service = new OrderRealtimeService(messagingTemplate);
        Order order = order();

        service.publishOrderCreated(order);

        ArgumentCaptor<OrderRealtimeEvent> captor = ArgumentCaptor.forClass(OrderRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/kitchen"), captor.capture());
        OrderRealtimeEvent event = captor.getValue();

        assertEquals(OrderRealtimeEventType.ORDER_CREATED, event.getEvent());
        assertEquals(99L, event.getOrderId());
        assertEquals(OrderStatus.PENDING, event.getStatus());
        assertEquals(1L, event.getBranchId());
        assertEquals("customer-phone", event.getCustomerUsername());
        assertEquals("Pizza Margherita", event.getItems().get(0).getDishName());
        assertEquals(List.of("Cheese"), event.getItems().get(0).getToppings());

        verify(messagingTemplate).convertAndSend("/topic/orders/99", event);
        verify(messagingTemplate).convertAndSend("/topic/orders/branches/1", event);
        verify(messagingTemplate).convertAndSendToUser("customer-phone", "/queue/orders", event);
    }

    @Test
    void publishOrderStatusChangedShouldIncludePreviousStatus() {
        OrderRealtimeService service = new OrderRealtimeService(messagingTemplate);
        Order order = order();
        order.setStatus(OrderStatus.COOKING);

        service.publishOrderStatusChanged(order, OrderStatus.CONFIRMED);

        ArgumentCaptor<OrderRealtimeEvent> captor = ArgumentCaptor.forClass(OrderRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/kitchen"), captor.capture());

        assertEquals(OrderRealtimeEventType.ORDER_STATUS_CHANGED, captor.getValue().getEvent());
        assertEquals(OrderStatus.CONFIRMED, captor.getValue().getPreviousStatus());
        assertEquals(OrderStatus.COOKING, captor.getValue().getStatus());
    }

    private Order order() {
        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Branch A");

        Account customerAccount = new Account();
        customerAccount.setUsername("customer-phone");
        Customer customer = new Customer();
        customer.setId(2L);
        customer.setFullName("Customer A");
        customer.setAccount(customerAccount);

        Account employeeAccount = new Account();
        employeeAccount.setUsername("staff-phone");
        Employee employee = new Employee();
        employee.setId(3L);
        employee.setFullName("Staff A");
        employee.setAccount(employeeAccount);

        Dish dish = new Dish();
        dish.setName("Pizza Margherita");
        DishVariant variant = new DishVariant();
        variant.setId(4L);
        variant.setSize("M");
        variant.setDish(dish);

        Topping topping = new Topping();
        topping.setName("Cheese");

        OrderDetail detail = new OrderDetail();
        detail.setId(5L);
        detail.setDishVariant(variant);
        detail.setQuantity(2);
        detail.setUnitPrice(100000.0);
        detail.setSubTotal(200000.0);
        detail.setToppings(List.of(topping));

        Order order = new Order();
        order.setId(99L);
        order.setOrderTime(LocalDateTime.of(2026, 5, 1, 12, 0));
        order.setTotalPrice(200000.0);
        order.setDiscountAmount(0.0);
        order.setFinalTotalPrice(200000.0);
        order.setDeliveryMethod(DeliveryMethod.DELIVERY);
        order.setOrderSource(OrderSource.CUSTOMER_APP);
        order.setDeliveryAddress("HCM");
        order.setStatus(OrderStatus.PENDING);
        order.setCustomer(customer);
        order.setHandledBy(employee);
        order.setBranch(branch);
        order.setOrderDetails(List.of(detail));
        return order;
    }
}
