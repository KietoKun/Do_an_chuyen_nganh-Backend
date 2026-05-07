package com.pizzastore.service.unit;

import com.pizzastore.dto.UpdateProfileRequest;
import com.pizzastore.dto.UserProfileResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Customer;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserProfile_ShouldReturnCustomerProfile_WhenRoleIsCustomer() {
        // 1. ARRANGE
        String username = "0987654321";

        Account mockAccount = new Account();
        mockAccount.setId(10L);
        mockAccount.setUsername(username);
        mockAccount.setRole(RoleName.CUSTOMER);

        Customer mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setAccount(mockAccount);
        mockCustomer.setFullName("Nguyễn Văn Khách Hàng");
        mockCustomer.setAddress("TP Hồ Chí Minh");

        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(mockAccount));
        when(customerRepository.findAll()).thenReturn(List.of(mockCustomer));

        // 2. ACT
        UserProfileResponse response = userService.getUserProfile(username);

        // 3. ASSERT
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Nguyễn Văn Khách Hàng", response.getFullName());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("TP Hồ Chí Minh", response.getAddress());

        // Vì đã tìm thấy Customer nên không được gọi sang bảng Employee để tìm nữa (tối ưu hiệu năng)
        verify(employeeRepository, never()).findAll();
    }

    @Test
    void updateProfile_ShouldUpdateCustomerInfo_WhenRequestIsValid() {
        // 1. ARRANGE
        String username = "0911222333";
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Tên Mới Của Tôi");
        request.setAddress("Hà Nội");

        Account mockAccount = new Account();
        mockAccount.setId(5L);
        mockAccount.setRole(RoleName.CUSTOMER);

        Customer mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setAccount(mockAccount);
        mockCustomer.setFullName("Tên Cũ");

        when(accountRepository.findByUsername(username)).thenReturn(Optional.of(mockAccount));
        when(customerRepository.findAll()).thenReturn(List.of(mockCustomer));

        // 2. ACT
        userService.updateProfile(username, request);

        // 3. ASSERT
        assertEquals("Tên Mới Của Tôi", mockCustomer.getFullName());
        assertEquals("Hà Nội", mockCustomer.getAddress());
        verify(customerRepository, times(1)).save(mockCustomer);
    }

    @Test
    void getUserProfile_ShouldThrowException_WhenAccountNotFound() {
        // ARRANGE
        String unknownUser = "ghost";
        when(accountRepository.findByUsername(unknownUser)).thenReturn(Optional.empty());

        // ACT & ASSERT
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserProfile(unknownUser);
        });

        assertEquals("Tài khoản không tồn tại", exception.getMessage());
    }
}
