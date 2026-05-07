package com.pizzastore.service.unit;

import com.pizzastore.dto.CustomerResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Customer;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void getAllCustomersShouldMapAllRows() {
        when(customerRepository.findAll()).thenReturn(List.of(
                customer(1L, "Nguyen Van A", "0911", "HCM", "a@example.com", "0911"),
                customer(2L, "Nguyen Van B", "0922", "HN", "b@example.com", "0922")
        ));

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertEquals(2, responses.size());
        assertEquals("Nguyen Van A", responses.get(0).getFullName());
        assertEquals("0911", responses.get(0).getUsername());
    }

    @Test
    void getAllCustomersShouldReturnEmptyListWhenRepositoryIsEmpty() {
        when(customerRepository.findAll()).thenReturn(List.of());

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getCustomerByIdShouldReturnMappedDto() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(
                customer(1L, "Nguyen Van A", "0911", "HCM", "a@example.com", "0911")
        ));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Nguyen Van A", response.getFullName());
    }

    @Test
    void getCustomerByIdShouldThrowWhenMissing() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> customerService.getCustomerById(1L));

        assertTrue(ex.getMessage().contains("1"));
    }

    @Test
    void deleteCustomerShouldDeleteWhenExists() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        customerService.deleteCustomer(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteCustomerShouldThrowWhenMissing() {
        when(customerRepository.existsById(1L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> customerService.deleteCustomer(1L));

        assertNotNull(ex.getMessage());
        verify(customerRepository, never()).deleteById(anyLong());
    }

    private Customer customer(Long id, String fullName, String phoneNumber, String address, String email, String username) {
        Account account = new Account();
        account.setUsername(username);

        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName(fullName);
        customer.setPhoneNumber(phoneNumber);
        customer.setAddress(address);
        customer.setEmail(email);
        customer.setAccount(account);
        return customer;
    }
}
