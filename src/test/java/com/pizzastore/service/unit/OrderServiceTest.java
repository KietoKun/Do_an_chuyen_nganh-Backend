package com.pizzastore.service.unit;

import com.pizzastore.entity.Account;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.Order;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.repository.*;
import com.pizzastore.service.BranchAccessService;
import com.pizzastore.service.DishService;
import com.pizzastore.service.OrderRealtimeService;
import com.pizzastore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private BranchAccessService branchAccessService;
    @Mock private OrderRealtimeService orderRealtimeService;

    // Khai báo các dependency phụ để InjectMocks không bị văng lỗi (có thể không xài)
    @Mock private CustomerRepository customerRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private DishVariantRepository dishVariantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ToppingRepository toppingRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryBatchRepository inventoryBatchRepository;
    @Mock private InventoryBatchConsumptionRepository inventoryBatchConsumptionRepository;
    @Mock private DishService dishService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void approveOrder_ShouldUpdateStatusToConfirmed_WhenOrderIsPending() {
        // 1. ARRANGE
        Long orderId = 1L;
        String staffUsername = "staff_01";

        Order pendingOrder = new Order();
        pendingOrder.setId(orderId);
        pendingOrder.setStatus(OrderStatus.PENDING); // Đơn đang chờ duyệt

        Employee staff = new Employee();
        Account account = new Account();
        account.setUsername(staffUsername);
        staff.setAccount(account);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        // Không mock branchAccessService.assertCanAccessOrder vì hàm trả về void, mặc định nó sẽ pass (không throw Exception)
        when(employeeRepository.findByAccount_Username(staffUsername)).thenReturn(Optional.of(staff));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        // 2. ACT
        orderService.approveOrder(orderId, staffUsername);

        // 3. ASSERT
        assertEquals(OrderStatus.CONFIRMED, pendingOrder.getStatus());
        assertEquals(staff, pendingOrder.getHandledBy());
        assertNotNull(pendingOrder.getAcceptedAt());

        // Kiểm tra xem dịch vụ Realtime (WebSocket) có được gọi để báo tin không
        verify(orderRealtimeService, times(1)).publishOrderConfirmed(eq(pendingOrder), eq(OrderStatus.PENDING));
    }

    @Test
    void approveOrder_ShouldThrowException_WhenOrderStatusIsAlreadyCompleted() {
        // 1. ARRANGE
        Long orderId = 2L;
        String staffUsername = "staff_02";

        Order completedOrder = new Order();
        completedOrder.setId(orderId);
        completedOrder.setStatus(OrderStatus.COMPLETED); // Đơn đã giao xong

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));

        // 2 & 3. ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.approveOrder(orderId, staffUsername);
        });

        assertEquals("Đơn hàng này không thể duyệt", exception.getMessage());

        // Đảm bảo không có dòng dữ liệu nào bị sửa đổi
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderRealtimeService, never()).publishOrderConfirmed(any(), any());
    }
    @Test
    void createOrder_ShouldThrowException_WhenDishIsUnavailable() {
        // 1. ARRANGE
        String username = "customer_01";

        // Cần đảm bảo OrderRequest được import từ: com.pizzastore.dto.OrderRequest
        com.pizzastore.dto.OrderRequest request = new com.pizzastore.dto.OrderRequest();
        com.pizzastore.dto.OrderRequest.CartItem item = new com.pizzastore.dto.OrderRequest.CartItem();
        item.setVariantId(10L);
        item.setQuantity(2);
        request.setItems(java.util.List.of(item));
        request.setDeliveryAddress("123 Đường Test, Quận 1");

        // Cần đảm bảo Account được import từ: com.pizzastore.entity.Account
        com.pizzastore.entity.Account account = new com.pizzastore.entity.Account();
        account.setId(1L);
        account.setUsername(username);

        // TUYỆT CHIÊU: Dùng đối tượng Customer thật và dùng Reflection để ép giá trị (Không cần Setter, không cần Mock)
        com.pizzastore.entity.Customer realCustomer = new com.pizzastore.entity.Customer();
        org.springframework.test.util.ReflectionTestUtils.setField(realCustomer, "account", account);

        com.pizzastore.entity.Dish unavailableDish = new com.pizzastore.entity.Dish();
        unavailableDish.setName("Pizza Hải Sản");
        unavailableDish.setAvailable(false); // BỊ TẮT

        com.pizzastore.entity.DishVariant variant = new com.pizzastore.entity.DishVariant();
        variant.setId(10L);
        variant.setDish(unavailableDish);

        when(accountRepository.findByUsername(username)).thenReturn(java.util.Optional.of(account));
        // Trả về Customer thật vừa tạo
        when(customerRepository.findAll()).thenReturn(java.util.List.of(realCustomer));
        when(dishVariantRepository.findById(10L)).thenReturn(java.util.Optional.of(variant));

        // 2 & 3. ACT & ASSERT
        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(username, request);
        });

        org.junit.jupiter.api.Assertions.assertEquals("Món Pizza Hải Sản hiện đã ngừng phục vụ", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void startCooking_ShouldUpdateStatusToCooking_WhenOrderIsConfirmed() {
        // 1. ARRANGE
        Long orderId = 1L;
        String chefUsername = "chef_01";

        Order confirmedOrder = new Order();
        confirmedOrder.setId(orderId);
        confirmedOrder.setStatus(OrderStatus.CONFIRMED); // Hợp lệ để nấu

        Account account = new Account();
        account.setUsername(chefUsername);
        Employee chef = new Employee();
        chef.setAccount(account);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(confirmedOrder));
        when(employeeRepository.findByAccount_Username(chefUsername)).thenReturn(Optional.of(chef));
        when(orderRepository.save(any(Order.class))).thenReturn(confirmedOrder);

        // 2. ACT
        orderService.startCooking(orderId, chefUsername);

        // 3. ASSERT
        assertEquals(OrderStatus.COOKING, confirmedOrder.getStatus());
        assertEquals(chef, confirmedOrder.getCookedBy());
        assertNotNull(confirmedOrder.getCookingStartedAt());
        verify(orderRealtimeService, times(1)).publishOrderStatusChanged(eq(confirmedOrder), eq(OrderStatus.CONFIRMED));
    }

    @Test
    void startCooking_ShouldThrowException_WhenOrderIsNotConfirmed() {
        // 1. ARRANGE
        Long orderId = 2L;
        String chefUsername = "chef_02";

        Order pendingOrder = new Order();
        pendingOrder.setId(orderId);
        pendingOrder.setStatus(OrderStatus.PENDING); // Đơn chưa duyệt không được phép nấu

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        // 2 & 3. ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.startCooking(orderId, chefUsername);
        });

        assertEquals("Chi co the bat dau nau don da duoc xac nhan", exception.getMessage());
    }

    @Test
    void cancelOrder_ShouldRestoreInventoryAndCancel_WhenOrderIsCancellable() {
        // 1. ARRANGE (Test hoàn trả nguyên liệu (Inventory) khi hủy đơn)
        Long orderId = 5L;
        String reason = "Khách đổi ý";

        com.pizzastore.entity.Order orderToCancel = new com.pizzastore.entity.Order();
        orderToCancel.setId(orderId);
        orderToCancel.setStatus(com.pizzastore.enums.OrderStatus.PENDING); // Hợp lệ để hủy
        orderToCancel.setNote("Đơn hàng test");

        // Giả lập đơn hàng này đã tiêu thụ 1 lô nguyên liệu (Batch)
        com.pizzastore.entity.InventoryBatchConsumption consumption = new com.pizzastore.entity.InventoryBatchConsumption();
        consumption.setQuantity(5.0); // Đã trừ 5 đơn vị

        com.pizzastore.entity.InventoryBatch batch = new com.pizzastore.entity.InventoryBatch();
        batch.setQuantityRemaining(10.0);
        consumption.setBatch(batch);

        when(orderRepository.findById(orderId)).thenReturn(java.util.Optional.of(orderToCancel));
        when(inventoryBatchConsumptionRepository.findByOrderDetail_Order_IdAndReturnedFalse(orderId))
                .thenReturn(java.util.List.of(consumption)); // Báo là có 1 list nguyên liệu cần hoàn trả
        when(orderRepository.save(any(com.pizzastore.entity.Order.class))).thenReturn(orderToCancel);

        // 2. ACT
        orderService.cancelOrder(orderId, reason);

        // 3. ASSERT
        org.junit.jupiter.api.Assertions.assertEquals(com.pizzastore.enums.OrderStatus.CANCELLED, orderToCancel.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(orderToCancel.getNote().contains("Da huy: Khách đổi ý"));

        // KIỂM TRA LOGIC KHO: Đảm bảo số lượng nguyên liệu trong Lô đã được cộng lại (+5)
        org.junit.jupiter.api.Assertions.assertEquals(15.0, batch.getQuantityRemaining());

        // TUYỆT CHIÊU: Tránh lỗi đỏ giữa isReturned() và getReturned()
        Object isReturned = org.springframework.test.util.ReflectionTestUtils.getField(consumption, "returned");
        org.junit.jupiter.api.Assertions.assertEquals(true, isReturned);
        org.junit.jupiter.api.Assertions.assertNotNull(consumption.getReturnedAt());

        // Đảm bảo kho và tiêu thụ lô đã được gọi hàm Save để lưu cập nhật
        verify(inventoryBatchRepository, times(1)).save(batch);
        verify(inventoryBatchConsumptionRepository, times(1)).save(consumption);
        verify(orderRealtimeService, times(1)).publishOrderCancelled(eq(orderToCancel), eq(com.pizzastore.enums.OrderStatus.PENDING));
    }
}
