package com.pizzastore.service;

import com.pizzastore.dto.CustomerResponse;
import com.pizzastore.entity.Customer;
import com.pizzastore.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerResponse> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();

        return customers.stream().map(cust -> new CustomerResponse(
                cust.getId(),
                cust.getFullName(),
                cust.getPhoneNumber(),
                cust.getAddress(),
                cust.getEmail(),
                cust.getAccount().getUsername()
        )).collect(Collectors.toList());
    }
    public CustomerResponse getCustomerById(Long id) {
        Customer cust = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        return new CustomerResponse(
                cust.getId(),
                cust.getFullName(),
                cust.getPhoneNumber(),
                cust.getAddress(),
                cust.getEmail(),
                cust.getAccount().getUsername()
        );
    }


    @Transactional
    public void deleteCustomer(Long id) {

        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Khách hàng không tồn tại!");
        }
        customerRepository.deleteById(id);
    }
}