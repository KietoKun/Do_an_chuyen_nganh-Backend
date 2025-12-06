package com.pizzastore.controller;

import com.pizzastore.entity.Account;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.pizzastore.entity.Employee;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.entity.Customer;
import com.pizzastore.repository.CustomerRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;


    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils,
                          CustomerRepository customerRepository, EmployeeRepository employeeRepository) {
        this.authenticationManager = authenticationManager;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    // --- 2. API ĐĂNG KÝ (Dành cho Khách hàng) ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest signUpRequest) {

        // 1. Kiểm tra Số điện thoại (chính là Username) đã tồn tại chưa
        if (accountRepository.existsByUsername(signUpRequest.getPhoneNumber())) {
            return ResponseEntity.badRequest().body("Lỗi: Số điện thoại này đã được đăng ký!");
        }

        // 2. Tạo Account
        Account account = new Account();
        // QUAN TRỌNG: Dùng SĐT làm Username đăng nhập
        account.setUsername(signUpRequest.getPhoneNumber());
        account.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        account.setRole(RoleName.CUSTOMER);
        account.setFirstLogin(false); // Khách tự đăng ký thì không cần đổi pass lần đầu

        // 3. Tạo Customer (Thông tin cá nhân)
        Customer customer = new Customer();
        customer.setFullName(signUpRequest.getFullName());
        customer.setPhoneNumber(signUpRequest.getPhoneNumber());
        customer.setAddress(signUpRequest.getAddress());
        customer.setEmail(signUpRequest.getEmail());

        // 4. Liên kết Account vào Customer
        customer.setAccount(account);

        // 5. Lưu xuống DB (Cascade sẽ tự lưu Account)
        customerRepository.save(customer);

        return ResponseEntity.ok("Đăng ký thành công! Hãy đăng nhập bằng Số điện thoại.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            // --- LẤY THÔNG TIN USER VÀ ROLE ---
            // Ép kiểu principal về UserDetailsImpl (Class chúng ta tự viết)
            com.pizzastore.security.UserDetailsImpl userDetails =
                    (com.pizzastore.security.UserDetailsImpl) authentication.getPrincipal();

            // Lấy role đầu tiên (vì mỗi user chỉ có 1 role trong hệ thống này)
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .get().getAuthority();

            // Trả về Full thông tin
            return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), role));

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // Bắt lỗi sai tài khoản/mật khẩu
            return ResponseEntity.status(401).body("Lỗi đăng nhập: Sai tài khoản hoặc mật khẩu!");
        } catch (Exception e) {
            // Bắt các lỗi khác
            e.printStackTrace(); // In lỗi ra Console IntelliJ để debug
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()") // Bắt buộc phải đăng nhập rồi
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {

        // 1. Lấy User hiện tại đang đăng nhập từ Token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName(); // Lấy username từ token

        // 2. Tìm tài khoản trong DB
        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản!"));

        // 3. Kiểm tra mật khẩu cũ có đúng không
        // passwordEncoder.matches(pass_nhập_vào, pass_đã_mã_hóa_trong_db)
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu hiện tại không đúng!");
        }

        // 4. Mã hóa và Lưu mật khẩu mới
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // 5. Nếu đây là lần đầu đăng nhập, tắt cờ FirstLogin đi
        if (account.isFirstLogin()) {
            account.setFirstLogin(false);
        }

        accountRepository.save(account);

        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }
    @PostMapping("/init-admin")
    public ResponseEntity<?> initAdmin() {
        // Giả sử SĐT của chủ quán là 0900000000
        String adminPhone = "0900000000";

        if (accountRepository.existsByUsername(adminPhone)) {
            return ResponseEntity.badRequest().body("Admin đã tồn tại!");
        }

        // 1. Tạo Account
        Account account = new Account();
        account.setUsername(adminPhone); // Username là SĐT
        account.setPassword(passwordEncoder.encode("123456")); // Mật khẩu mặc định
        account.setRole(RoleName.MANAGER);
        account.setFirstLogin(false); // Admin trùm thì khỏi cần đổi pass lần đầu cũng được

        // 2. Tạo thông tin Employee (Admin)
        Employee admin = new Employee();
        admin.setFullName("Administrator");
        admin.setPhoneNumber(adminPhone);
        admin.setPosition("Chủ cửa hàng");

        // 3. Liên kết (Quan trọng)
        admin.setAccount(account); // account sẽ được lưu tự động nhờ Cascade

        employeeRepository.save(admin);

        return ResponseEntity.ok("Tạo Admin thành công! SĐT: " + adminPhone + " / Pass: 123456");
    }
}

// DTO Đăng ký (Đã bỏ trường username)
class RegisterRequest {
    // private String username; <--- XÓA DÒNG NÀY
    private String password;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String email;

    // --- Getters & Setters ---
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}


class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;

    // Getter & Setter (Viết tay)
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
