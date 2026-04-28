package com.pizzastore.service;

import com.pizzastore.dto.CommentRequest;
import com.pizzastore.dto.CommentResponse;
import com.pizzastore.entity.Customer;
import com.pizzastore.entity.Dish;
import com.pizzastore.entity.DishComment;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.DishCommentRepository;
import com.pizzastore.repository.DishRepository;
import com.pizzastore.repository.OrderDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishCommentService {
    private static final int MAX_COMMENTS_PER_CUSTOMER_PER_DISH = 3;

    private final DishCommentRepository dishCommentRepository;
    private final DishRepository dishRepository;
    private final CustomerRepository customerRepository;
    private final OrderDetailRepository orderDetailRepository;

    public DishCommentService(DishCommentRepository dishCommentRepository,
                              DishRepository dishRepository,
                              CustomerRepository customerRepository,
                              OrderDetailRepository orderDetailRepository) {
        this.dishCommentRepository = dishCommentRepository;
        this.dishRepository = dishRepository;
        this.customerRepository = customerRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    @Transactional
    public CommentResponse createComment(String username, Long dishId, CommentRequest request) {
        Customer customer = customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("Món ăn không tồn tại"));

        validateRequest(request);

        long purchasedCount = orderDetailRepository.countPurchasedDishByStatus(
                customer.getId(),
                dishId,
                OrderStatus.COMPLETED
        );
        if (purchasedCount == 0) {
            throw new RuntimeException("Bạn chỉ có thể bình luận món ăn trong đơn hàng đã hoàn tất");
        }

        long commentCount = dishCommentRepository.countByCustomer_IdAndDish_Id(customer.getId(), dishId);
        if (commentCount >= MAX_COMMENTS_PER_CUSTOMER_PER_DISH) {
            throw new RuntimeException("Bạn chỉ được bình luận tối đa 3 lần cho mỗi món ăn");
        }

        DishComment comment = new DishComment();
        comment.setCustomer(customer);
        comment.setDish(dish);
        comment.setContent(request.getContent().trim());
        comment.setRating(request.getRating());
        comment.setVisible(true);

        return toResponse(dishCommentRepository.save(comment));
    }

    public List<CommentResponse> getCommentsByDish(Long dishId) {
        if (!dishRepository.existsById(dishId)) {
            throw new RuntimeException("Món ăn không tồn tại");
        }

        return dishCommentRepository.findVisibleByDishIdOrderByCreatedAtDesc(dishId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CommentResponse> getAllComments() {
        return dishCommentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CommentResponse> getAllCommentsByDish(Long dishId) {
        if (!dishRepository.existsById(dishId)) {
            throw new RuntimeException("Món ăn không tồn tại");
        }

        return dishCommentRepository.findByDish_IdOrderByCreatedAtDesc(dishId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse updateCommentVisibility(Long commentId, boolean visible) {
        DishComment comment = dishCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận"));

        comment.setVisible(visible);
        return toResponse(dishCommentRepository.save(comment));
    }

    private void validateRequest(CommentRequest request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new RuntimeException("Nội dung bình luận không được để trống");
        }
        if (request.getContent().trim().length() > 1000) {
            throw new RuntimeException("Nội dung bình luận không được vượt quá 1000 ký tự");
        }
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new RuntimeException("Đánh giá phải nằm trong khoảng từ 1 đến 5");
        }
    }

    private CommentResponse toResponse(DishComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getDish().getId(),
                comment.getDish().getName(),
                comment.getCustomer().getId(),
                comment.getCustomer().getFullName(),
                comment.getContent(),
                comment.getRating(),
                comment.isVisible(),
                comment.getCreatedAt()
        );
    }
}
