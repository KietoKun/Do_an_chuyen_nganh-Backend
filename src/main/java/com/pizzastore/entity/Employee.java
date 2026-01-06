package com.pizzastore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee extends Person {

    // Các thuộc tính riêng của Nhân viên
    private String position;
    private Double salaryPerHour;
    private LocalDateTime lastCheckIn;
    private String email;

    public Employee() {
        super(); // Gọi constructor của cha
    }



    public void clockIn() {
        this.lastCheckIn = LocalDateTime.now();
        System.out.println("Nhân viên " + this.getFullName() + " đã chấm công lúc " + this.lastCheckIn);
    }


    public Double calculateSalary(int hoursWorked) {
        if (this.salaryPerHour == null) return 0.0;
        return this.salaryPerHour * hoursWorked;
    }


    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Double getSalaryPerHour() { return salaryPerHour; }
    public void setSalaryPerHour(Double salaryPerHour) { this.salaryPerHour = salaryPerHour; }

    public LocalDateTime getLastCheckIn() { return lastCheckIn; }
    public void setLastCheckIn(LocalDateTime lastCheckIn) { this.lastCheckIn = lastCheckIn; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}