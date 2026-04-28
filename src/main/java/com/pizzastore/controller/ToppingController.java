package com.pizzastore.controller;

import com.pizzastore.entity.Topping;
import com.pizzastore.repository.ToppingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/toppings")
@Tag(name = "9. Tùy chọn món thêm (Topping)", description = "Quản lý danh sách các topping (Viền phô mai, thêm xúc xích...)")
public class ToppingController {

    @Autowired
    private ToppingRepository toppingRepository;

    @GetMapping
    @Operation(summary = "Xem danh sách Topping (Public)", description = "Hiển thị tất cả Topping kèm giá tiền để khách hàng chọn thêm khi đặt Pizza.")
    public ResponseEntity<List<Topping>> getAllToppings() {
        return ResponseEntity.ok(toppingRepository.findAll());
    }
}