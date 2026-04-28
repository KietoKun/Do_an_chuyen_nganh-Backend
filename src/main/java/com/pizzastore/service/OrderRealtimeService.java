package com.pizzastore.service;

import com.pizzastore.dto.OrderRealtimeEvent;
import com.pizzastore.entity.Order;
import com.pizzastore.entity.OrderDetail;
import com.pizzastore.entity.Topping;
import com.pizzastore.enums.OrderRealtimeEventType;
import com.pizzastore.enums.OrderStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishOrderCreated(Order order) {
        publish(order, OrderRealtimeEventType.ORDER_CREATED, null, "Đơn hàng mới đã được tạo");
    }

    public void publishOrderPaymentUpdated(Order order, OrderStatus previousStatus, String message) {
        publish(order, OrderRealtimeEventType.ORDER_PAYMENT_UPDATED, previousStatus, message);
    }

    public void publishOrderConfirmed(Order order, OrderStatus previousStatus) {
        publish(order, OrderRealtimeEventType.ORDER_CONFIRMED, previousStatus, "Đơn hàng đã được xác nhận");
    }

    public void publishOrderStatusChanged(Order order, OrderStatus previousStatus) {
        publish(order, OrderRealtimeEventType.ORDER_STATUS_CHANGED, previousStatus, "Trạng thái đơn hàng đã được cập nhật");
    }

    public void publishOrderCancelled(Order order, OrderStatus previousStatus) {
        publish(order, OrderRealtimeEventType.ORDER_CANCELLED, previousStatus, "Đơn hàng đã bị hủy");
    }

    private void publish(Order order, OrderRealtimeEventType eventType, OrderStatus previousStatus, String message) {
        OrderRealtimeEvent event = toEvent(order, eventType, previousStatus, message);

        messagingTemplate.convertAndSend("/topic/orders/kitchen", event);
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), event);

        if (order.getBranch() != null && order.getBranch().getId() != null) {
            messagingTemplate.convertAndSend("/topic/orders/branches/" + order.getBranch().getId(), event);
        }

        if (order.getCustomer() != null
                && order.getCustomer().getAccount() != null
                && order.getCustomer().getAccount().getUsername() != null) {
            messagingTemplate.convertAndSendToUser(
                    order.getCustomer().getAccount().getUsername(),
                    "/queue/orders",
                    event
            );
        }
    }

    private OrderRealtimeEvent toEvent(Order order, OrderRealtimeEventType eventType, OrderStatus previousStatus, String message) {
        OrderRealtimeEvent event = new OrderRealtimeEvent();
        event.setEvent(eventType);
        event.setOrderId(order.getId());
        event.setStatus(order.getStatus());
        event.setPreviousStatus(previousStatus);
        event.setOrderTime(order.getOrderTime());
        event.setAcceptedAt(order.getAcceptedAt());
        event.setCookingStartedAt(order.getCookingStartedAt());
        event.setCompletedAt(order.getCompletedAt());
        event.setTotalPrice(order.getTotalPrice());
        event.setDiscountAmount(order.getDiscountAmount());
        event.setFinalTotalPrice(order.getFinalTotalPrice());
        event.setDeliveryMethod(order.getDeliveryMethod());
        event.setDeliveryAddress(order.getDeliveryAddress());
        event.setNote(order.getNote());
        event.setMessage(message);

        if (order.getCustomer() != null) {
            event.setCustomerId(order.getCustomer().getId());
            event.setCustomerName(order.getCustomer().getFullName());
            if (order.getCustomer().getAccount() != null) {
                event.setCustomerUsername(order.getCustomer().getAccount().getUsername());
            }
        }

        if (order.getBranch() != null) {
            event.setBranchId(order.getBranch().getId());
            event.setBranchName(order.getBranch().getName());
        }

        if (order.getHandledBy() != null) {
            event.setHandledById(order.getHandledBy().getId());
            event.setHandledByName(order.getHandledBy().getFullName());
            if (order.getHandledBy().getAccount() != null) {
                event.setHandledByUsername(order.getHandledBy().getAccount().getUsername());
            }
        }

        if (order.getCookedBy() != null) {
            event.setCookedById(order.getCookedBy().getId());
            event.setCookedByName(order.getCookedBy().getFullName());
            if (order.getCookedBy().getAccount() != null) {
                event.setCookedByUsername(order.getCookedBy().getAccount().getUsername());
            }
        }

        event.setItems(mapItems(order.getOrderDetails()));
        return event;
    }

    private List<OrderRealtimeEvent.OrderItem> mapItems(List<OrderDetail> orderDetails) {
        if (orderDetails == null) {
            return Collections.emptyList();
        }

        return orderDetails.stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());
    }

    private OrderRealtimeEvent.OrderItem toOrderItem(OrderDetail orderDetail) {
        OrderRealtimeEvent.OrderItem item = new OrderRealtimeEvent.OrderItem();
        item.setOrderDetailId(orderDetail.getId());
        item.setQuantity(orderDetail.getQuantity());
        item.setUnitPrice(orderDetail.getUnitPrice());
        item.setSubTotal(orderDetail.getSubTotal());

        if (orderDetail.getDishVariant() != null) {
            item.setDishVariantId(orderDetail.getDishVariant().getId());
            item.setSize(orderDetail.getDishVariant().getSize());
            if (orderDetail.getDishVariant().getDish() != null) {
                item.setDishName(orderDetail.getDishVariant().getDish().getName());
            }
        }

        if (orderDetail.getToppings() == null) {
            item.setToppings(Collections.emptyList());
        } else {
            item.setToppings(orderDetail.getToppings().stream()
                    .map(Topping::getName)
                    .collect(Collectors.toList()));
        }

        return item;
    }
}
