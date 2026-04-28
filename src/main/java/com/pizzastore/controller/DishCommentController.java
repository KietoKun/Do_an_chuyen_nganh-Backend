package com.pizzastore.controller;

import com.pizzastore.dto.CommentRequest;
import com.pizzastore.dto.CommentResponse;
import com.pizzastore.service.DishCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@Tag(name = "12. Bình luận món ăn", description = "API xem và tạo bình luận cho món ăn khách đã đặt")
public class DishCommentController {
    private final DishCommentService dishCommentService;

    public DishCommentController(DishCommentService dishCommentService) {
        this.dishCommentService = dishCommentService;
    }

    @PostMapping("/dishes/{dishId}")
    @Operation(summary = "Bình luận món ăn đã đặt", description = "Khách hàng chỉ được bình luận món đã từng đặt và tối đa 3 lần cho mỗi món.")
    public ResponseEntity<?> createComment(@PathVariable Long dishId, @RequestBody CommentRequest request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            CommentResponse response = dishCommentService.createComment(currentUsername, dishId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/dishes/{dishId}")
    @Operation(summary = "Xem bình luận theo món ăn")
    public ResponseEntity<?> getCommentsByDish(@PathVariable Long dishId) {
        try {
            List<CommentResponse> comments = dishCommentService.getCommentsByDish(dishId);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "SUPER_ADMIN xem tat ca binh luan", description = "Bao gom ca binh luan dang an va dang hien thi.")
    public ResponseEntity<?> getAllCommentsForAdmin() {
        return ResponseEntity.ok(dishCommentService.getAllComments());
    }

    @GetMapping("/admin/dishes/{dishId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "SUPER_ADMIN xem tat ca binh luan cua mot mon", description = "Bao gom ca binh luan dang an va dang hien thi.")
    public ResponseEntity<?> getAllCommentsByDishForAdmin(@PathVariable Long dishId) {
        try {
            return ResponseEntity.ok(dishCommentService.getAllCommentsByDish(dishId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/admin/{commentId}/visibility")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "SUPER_ADMIN an/hien binh luan", description = "visible=true de hien thi, visible=false de an khoi API public.")
    public ResponseEntity<?> updateCommentVisibility(@PathVariable Long commentId, @RequestParam boolean visible) {
        try {
            return ResponseEntity.ok(dishCommentService.updateCommentVisibility(commentId, visible));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
