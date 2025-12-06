package com.pizzastore.controller;

import com.pizzastore.entity.Topping;
import com.pizzastore.repository.ToppingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/toppings")
public class ToppingController {

    @Autowired
    private ToppingRepository toppingRepository;

    // API Công khai: Lấy tất cả Topping để hiển thị lên Menu
    @GetMapping
    public ResponseEntity<List<Topping>> getAllToppings() {
        return ResponseEntity.ok(toppingRepository.findAll());
    }
}