package com.pizzastore.entity;

import jakarta.persistence.*;

// @MappedSuperclass: Báo cho JPA biết đây là lớp cha,
// các thuộc tính ở đây sẽ được nhúng vào bảng của lớp con.
@MappedSuperclass
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String phoneNumber;
    private String address;

    // Quan hệ Account (Mọi Person đều có tài khoản)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    private Account account;

    // --- CONSTRUCTOR ---
    public Person() {}

    // --- GETTERS & SETTERS (Dùng chung cho cả Employee và Customer) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}