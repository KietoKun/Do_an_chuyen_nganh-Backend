package com.pizzastore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee extends Person { // <--- KẾ THỪA Ở ĐÂY

    // Các thuộc tính riêng của Nhân viên
    private String position;
    private Double salaryPerHour;
    private LocalDateTime lastCheckIn;

    // --- CONSTRUCTOR ---
    public Employee() {
        super(); // Gọi constructor của cha
    }

    // ==========================================
    // 1. CÁC PHƯƠNG THỨC NGHIỆP VỤ (Business Logic)
    // ==========================================

    /**
     * Hành động Chấm công
     */
    public void clockIn() {
        this.lastCheckIn = LocalDateTime.now();
        System.out.println("Nhân viên " + this.getFullName() + " đã chấm công lúc " + this.lastCheckIn);
    }

    /**
     * Hành động Tính lương (Giả sử làm 8 tiếng)
     */
    public Double calculateSalary(int hoursWorked) {
        if (this.salaryPerHour == null) return 0.0;
        return this.salaryPerHour * hoursWorked;
    }

    // ==========================================
    // 2. GETTER & SETTER (Chỉ cho các thuộc tính riêng)
    // ==========================================

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Double getSalaryPerHour() { return salaryPerHour; }
    public void setSalaryPerHour(Double salaryPerHour) { this.salaryPerHour = salaryPerHour; }

    public LocalDateTime getLastCheckIn() { return lastCheckIn; }
    public void setLastCheckIn(LocalDateTime lastCheckIn) { this.lastCheckIn = lastCheckIn; }
}