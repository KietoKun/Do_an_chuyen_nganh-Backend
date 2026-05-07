package com.pizzastore.service.unit;

import com.pizzastore.dto.CommentRequest;
import com.pizzastore.dto.CommentResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Customer;
import com.pizzastore.entity.Dish;
import com.pizzastore.entity.DishComment;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.DishCommentRepository;
import com.pizzastore.repository.DishRepository;
import com.pizzastore.repository.OrderDetailRepository;
import com.pizzastore.service.DishCommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishCommentServiceTest {

    @Mock
    private DishCommentRepository dishCommentRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @InjectMocks
    private DishCommentService dishCommentService;

    @Test
    void createCommentShouldPersistVisibleComment() {
        CommentRequest request = commentRequest("  Great pizza  ", 5);
        Customer customer = customer(1L, "customer-1", "Customer One");
        Dish dish = dish(10L, "Pizza Hai San");

        when(customerRepository.findByAccount_Username("customer-1")).thenReturn(Optional.of(customer));
        when(dishRepository.findById(10L)).thenReturn(Optional.of(dish));
        when(orderDetailRepository.countPurchasedDishByStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(2L);
        when(dishCommentRepository.countByCustomer_IdAndDish_Id(1L, 10L)).thenReturn(1L);
        when(dishCommentRepository.save(any(DishComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = dishCommentService.createComment("customer-1", 10L, request);

        assertEquals("Great pizza", response.getContent());
        assertEquals(5, response.getRating());
        assertTrue(response.getVisible());
        verify(dishCommentRepository).save(any(DishComment.class));
    }

    @Test
    void createCommentShouldRejectBlankContent() {
        CommentRequest request = commentRequest("   ", 5);
        Customer customer = customer(1L, "customer-1", "Customer One");
        Dish dish = dish(10L, "Pizza Hai San");

        when(customerRepository.findByAccount_Username("customer-1")).thenReturn(Optional.of(customer));
        when(dishRepository.findById(10L)).thenReturn(Optional.of(dish));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dishCommentService.createComment("customer-1", 10L, request));

        assertNotNull(ex.getMessage());
        verify(orderDetailRepository, never()).countPurchasedDishByStatus(any(), any(), any());
        verify(dishCommentRepository, never()).save(any());
    }

    @Test
    void createCommentShouldRejectWhenDishWasNotPurchased() {
        CommentRequest request = commentRequest("Good", 4);
        Customer customer = customer(1L, "customer-1", "Customer One");
        Dish dish = dish(10L, "Pizza Hai San");

        when(customerRepository.findByAccount_Username("customer-1")).thenReturn(Optional.of(customer));
        when(dishRepository.findById(10L)).thenReturn(Optional.of(dish));
        when(orderDetailRepository.countPurchasedDishByStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(0L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dishCommentService.createComment("customer-1", 10L, request));

        assertNotNull(ex.getMessage());
        verify(dishCommentRepository, never()).save(any());
    }

    @Test
    void createCommentShouldRejectWhenCustomerIsMissing() {
        CommentRequest request = commentRequest("Good", 4);
        when(customerRepository.findByAccount_Username("customer-1")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dishCommentService.createComment("customer-1", 10L, request));

        assertNotNull(ex.getMessage());
        verify(dishRepository, never()).findById(10L);
        verify(dishCommentRepository, never()).save(any());
    }

    @Test
    void createCommentShouldRejectInvalidRating() {
        CommentRequest request = commentRequest("Good", 6);
        Customer customer = customer(1L, "customer-1", "Customer One");
        Dish dish = dish(10L, "Pizza Hai San");

        when(customerRepository.findByAccount_Username("customer-1")).thenReturn(Optional.of(customer));
        when(dishRepository.findById(10L)).thenReturn(Optional.of(dish));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dishCommentService.createComment("customer-1", 10L, request));

        assertNotNull(ex.getMessage());
        verify(orderDetailRepository, never()).countPurchasedDishByStatus(any(), any(), any());
        verify(dishCommentRepository, never()).save(any());
    }

    @Test
    void createCommentShouldRejectWhenLimitReached() {
        CommentRequest request = commentRequest("Good", 4);
        Customer customer = customer(1L, "customer-1", "Customer One");
        Dish dish = dish(10L, "Pizza Hai San");

        when(customerRepository.findByAccount_Username("customer-1")).thenReturn(Optional.of(customer));
        when(dishRepository.findById(10L)).thenReturn(Optional.of(dish));
        when(orderDetailRepository.countPurchasedDishByStatus(1L, 10L, OrderStatus.COMPLETED)).thenReturn(1L);
        when(dishCommentRepository.countByCustomer_IdAndDish_Id(1L, 10L)).thenReturn(3L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dishCommentService.createComment("customer-1", 10L, request));

        assertNotNull(ex.getMessage());
        verify(dishCommentRepository, never()).save(any());
    }

    @Test
    void getCommentsByDishShouldReturnVisibleComments() {
        Dish dish = dish(10L, "Pizza Hai San");
        DishComment comment = new DishComment();
        comment.setId(1L);
        comment.setDish(dish);
        comment.setCustomer(customer(2L, "customer-2", "Customer Two"));
        comment.setContent("Nice");
        comment.setRating(5);
        comment.setVisible(true);
        comment.setCreatedAt(LocalDateTime.of(2026, 1, 10, 10, 0));

        when(dishRepository.existsById(10L)).thenReturn(true);
        when(dishCommentRepository.findVisibleByDishIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(comment));

        List<CommentResponse> responses = dishCommentService.getCommentsByDish(10L);

        assertEquals(1, responses.size());
        assertEquals("Nice", responses.get(0).getContent());
        assertEquals("Pizza Hai San", responses.get(0).getDishName());
        assertEquals("Customer Two", responses.get(0).getCustomerName());
    }

    @Test
    void getCommentsByDishShouldThrowWhenDishIsMissing() {
        when(dishRepository.existsById(10L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dishCommentService.getCommentsByDish(10L));

        assertNotNull(ex.getMessage());
        verify(dishCommentRepository, never()).findVisibleByDishIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void getAllCommentsShouldMapRepositoryRows() {
        DishComment comment = comment(
                1L,
                dish(10L, "Pizza Hai San"),
                customer(2L, "customer-2", "Customer Two"),
                "Nice",
                5,
                true
        );
        when(dishCommentRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(comment));

        List<CommentResponse> responses = dishCommentService.getAllComments();

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).getDishId());
        assertEquals(2L, responses.get(0).getCustomerId());
    }

    @Test
    void getAllCommentsByDishShouldThrowWhenDishIsMissing() {
        when(dishRepository.existsById(10L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dishCommentService.getAllCommentsByDish(10L));

        assertNotNull(ex.getMessage());
        verify(dishCommentRepository, never()).findByDish_IdOrderByCreatedAtDesc(10L);
    }

    @Test
    void updateCommentVisibilityShouldPersistNewState() {
        DishComment comment = new DishComment();
        comment.setId(1L);
        comment.setDish(dish(10L, "Pizza Hai San"));
        comment.setCustomer(customer(2L, "customer-2", "Customer Two"));
        comment.setContent("Nice");
        comment.setRating(5);
        comment.setVisible(true);
        comment.setCreatedAt(LocalDateTime.of(2026, 1, 10, 10, 0));

        when(dishCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(dishCommentRepository.save(any(DishComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = dishCommentService.updateCommentVisibility(1L, false);

        assertFalse(response.getVisible());
        verify(dishCommentRepository).save(comment);
    }

    @Test
    void updateCommentVisibilityShouldThrowWhenCommentIsMissing() {
        when(dishCommentRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dishCommentService.updateCommentVisibility(1L, false));

        assertNotNull(ex.getMessage());
        verify(dishCommentRepository, never()).save(any());
    }

    private CommentRequest commentRequest(String content, Integer rating) {
        CommentRequest request = new CommentRequest();
        request.setContent(content);
        request.setRating(rating);
        return request;
    }

    private Customer customer(Long id, String username, String fullName) {
        Account account = new Account();
        account.setUsername(username);

        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName(fullName);
        customer.setAccount(account);
        return customer;
    }

    private Dish dish(Long id, String name) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setName(name);
        return dish;
    }

    private DishComment comment(Long id, Dish dish, Customer customer, String content, Integer rating, boolean visible) {
        DishComment comment = new DishComment();
        comment.setId(id);
        comment.setDish(dish);
        comment.setCustomer(customer);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setVisible(visible);
        comment.setCreatedAt(LocalDateTime.of(2026, 1, 10, 10, 0));
        return comment;
    }
}
