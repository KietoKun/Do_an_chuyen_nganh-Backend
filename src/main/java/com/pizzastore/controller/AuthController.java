package com.pizzastore.controller;

import com.pizzastore.dto.ApiMessageResponse;
import com.pizzastore.dto.ChangePasswordRequest;
import com.pizzastore.dto.RegisterOtpRequest;
import com.pizzastore.dto.RegisterRequest;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Customer;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.RefreshToken;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.security.JwtUtils;
import com.pizzastore.security.UserDetailsImpl;
import com.pizzastore.service.RefreshTokenService;
import com.pizzastore.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Xac thuc & Phan quyen (Auth)", description = "API dang ky, dang nhap va quan ly tai khoan")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final RegistrationService registrationService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils,
                          CustomerRepository customerRepository,
                          EmployeeRepository employeeRepository,
                          BranchRepository branchRepository,
                          RegistrationService registrationService,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.registrationService = registrationService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register/request-otp")
    @Operation(
            summary = "Buoc 1 - Gui ma OTP dang ky den email",
            description = """
                    Endpoint nay la buoc dau tien trong quy trinh dang ky tai khoan khach hang.

                    Quy trinh:
                    1. Frontend gui thong tin co ban cua nguoi dung gom ho ten, so dien thoai, dia chi, email.
                    2. He thong kiem tra so dien thoai da ton tai hay chua.
                    3. Neu hop le, he thong sinh ma OTP va gui den email nguoi dung.
                    4. OTP duoc luu tam de xac thuc cho buoc tao tai khoan.

                    Luu y:
                    - Chua tao tai khoan o buoc nay.
                    - Sau khi nhan OTP, frontend tiep tuc goi /api/auth/register kem otpCode de hoan tat dang ky.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gui OTP thanh cong"),
            @ApiResponse(responseCode = "400", description = "Du lieu khong hop le hoac so dien thoai da ton tai"),
            @ApiResponse(responseCode = "429", description = "Gui OTP qua nhanh, can cho het thoi gian cooldown"),
            @ApiResponse(responseCode = "503", description = "Khong gui duoc OTP do he thong email chua san sang")
    })
    public ResponseEntity<?> requestRegisterOtp(@RequestBody RegisterOtpRequest request) {
        registrationService.sendRegistrationOtp(request);
        return ResponseEntity.ok(new ApiMessageResponse("Mã xác thực đã được gửi đến email của bạn."));
    }

    @PostMapping("/register")
    @Operation(
            summary = "Buoc 2 - Xac thuc OTP va tao tai khoan khach hang",
            description = """
                    Endpoint nay la buoc thu hai trong quy trinh dang ky.

                    Quy trinh:
                    1. Frontend gui lai day du thong tin dang ky kem otpCode ma nguoi dung nhan duoc qua email.
                    2. He thong kiem tra OTP va han su dung OTP.
                    3. Neu hop le, he thong tao tai khoan CUSTOMER voi username la so dien thoai.
                    4. Sau khi tao tai khoan thanh cong, he thong gui email thong bao den email nguoi dung da dang ky.

                    Ket qua:
                    - Tai khoan duoc tao thanh cong ngay ca khi email thong bao gui that bai.
                    - Neu OTP sai, het han, hoac chua gui OTP truoc do thi he thong tu choi dang ky.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dang ky thanh cong"),
            @ApiResponse(responseCode = "400", description = "OTP sai, het han, chua gui OTP hoac du lieu khong hop le")
    })
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest signUpRequest) {
        boolean emailSent = registrationService.registerCustomer(signUpRequest);
        if (emailSent) {
            return ResponseEntity.ok(new ApiMessageResponse(
                    "Đăng ký thành công. Email thông báo đã được gửi, hãy đăng nhập bằng số điện thoại."
            ));
        }
        return ResponseEntity.ok(new ApiMessageResponse(
                "Đăng ký thành công. Tài khoản đã được tạo nhưng email thông báo chưa gửi được."
        ));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Dang nhap he thong",
            description = """
                    Dang nhap thanh cong se tra ve:
                    - accessToken co hieu luc 5 phut de goi cac API duoc bao ve
                    - refreshToken dung de xin access token moi ma khong can dang nhap lai

                    Khi access token het han, frontend goi /api/auth/refresh-token voi refreshToken hien tai.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dang nhap thanh cong"),
            @ApiResponse(responseCode = "401", description = "Sai tai khoan hoac mat khau")
    })
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String accessToken = jwtUtils.generateAccessToken(userDetails);
            Account account = accountRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
            RefreshToken refreshToken = refreshTokenService.createOrRotateRefreshToken(account);

            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .get()
                    .getAuthority();

            String username = userDetails.getUsername();
            String fullName;
            Long branchId = null;
            String branchName = null;

            if (role.contains("CUSTOMER")) {
                fullName = customerRepository.findByAccount_Username(username)
                        .map(Customer::getFullName)
                        .orElse("Khách hàng");
            } else {
                fullName = employeeRepository.findByAccount_Username(username)
                        .map(Employee::getFullName)
                        .orElse("Nhân viên");
            }

            Employee loginEmployee = role.contains("CUSTOMER")
                    ? null
                    : employeeRepository.findByAccount_Username(username).orElse(null);
            if (loginEmployee != null && loginEmployee.getBranch() != null) {
                branchId = loginEmployee.getBranch().getId();
                branchName = loginEmployee.getBranch().getName();
            }

            return ResponseEntity.ok(new JwtResponse(
                    accessToken,
                    refreshToken.getToken(),
                    "Bearer",
                    jwtUtils.getJwtExpirationMs(),
                    username,
                    role,
                    fullName,
                    branchId,
                    branchName
            ));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Lam moi access token bang refresh token",
            description = """
                    Endpoint nay duoc dung khi access token da het han.

                    Quy trinh:
                    1. Frontend gui refreshToken da nhan khi login.
                    2. He thong kiem tra refresh token co ton tai va con han hay khong.
                    3. Neu hop le, he thong cap access token moi va dong thoi xoay refresh token moi.

                    Neu refresh token khong hop le hoac da het han, nguoi dung can dang nhap lai.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lam moi token thanh cong"),
            @ApiResponse(responseCode = "400", description = "Thieu refresh token"),
            @ApiResponse(responseCode = "401", description = "Refresh token khong hop le hoac da het han")
    })
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        RefreshToken currentRefreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        Account account = currentRefreshToken.getAccount();
        UserDetailsImpl userDetails = UserDetailsImpl.build(account);
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        RefreshToken newRefreshToken = refreshTokenService.createOrRotateRefreshToken(account);

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .get()
                .getAuthority();

        String fullName = getFullNameByRole(account.getUsername(), role);
        Employee refreshEmployee = role.contains("CUSTOMER")
                ? null
                : employeeRepository.findByAccount_Username(account.getUsername()).orElse(null);
        Long branchId = refreshEmployee != null && refreshEmployee.getBranch() != null
                ? refreshEmployee.getBranch().getId()
                : null;
        String branchName = refreshEmployee != null && refreshEmployee.getBranch() != null
                ? refreshEmployee.getBranch().getName()
                : null;

        return ResponseEntity.ok(new JwtResponse(
                accessToken,
                newRefreshToken.getToken(),
                "Bearer",
                jwtUtils.getJwtExpirationMs(),
                account.getUsername(),
                role,
                fullName,
                branchId,
                branchName
        ));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Dang xuat",
            description = "Xoa refresh token dang hoat dong cua tai khoan hien tai. Sau do nguoi dung can dang nhap lai de nhan bo token moi."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dang xuat thanh cong"),
            @ApiResponse(responseCode = "401", description = "Chua dang nhap hoac access token khong hop le")
    })
    public ResponseEntity<?> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        refreshTokenService.deleteByAccount(account);
        return ResponseEntity.ok(new ApiMessageResponse("Đăng xuất thành công"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Doi mat khau")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu hiện tại không đúng");
        }

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        if (account.isFirstLogin()) {
            account.setFirstLogin(false);
        }
        accountRepository.save(account);

        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @PostMapping("/init-admin")
    @Operation(
            summary = "Khoi tao SUPER_ADMIN va manager mac dinh cho cac chi nhanh",
            description = """
                    Tao hoac cap nhat tai khoan quan ly tong va cac tai khoan manager mac dinh cho chi nhanh.

                    Tai khoan SUPER_ADMIN:
                    - Username/SDT: 0900000000
                    - Password: 123456

                    Tai khoan manager chi nhanh mac dinh:
                    - Chi nhanh thu 1: username/SDT 0910000001, password 123456
                    - Chi nhanh thu 2: username/SDT 0910000002, password 123456
                    - Chi nhanh thu 3: username/SDT 0910000003, password 123456

                    Luu y: manager mac dinh duoc gan theo thu tu danh sach chi nhanh hien co trong database.
                    """
    )
    public ResponseEntity<?> initAdmin() {
        String adminPhone = "0900000000";

        if (accountRepository.existsByUsername(adminPhone)) {
            Account existingAdmin = accountRepository.findByUsername(adminPhone)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy SUPER_ADMIN"));
            existingAdmin.setRole(RoleName.SUPER_ADMIN);
            accountRepository.save(existingAdmin);
            createDefaultBranchManagers();
            return ResponseEntity.ok("SUPER_ADMIN đã tồn tại, đã cập nhật role và tạo manager chi nhánh còn thiếu.");
        }

        Account account = new Account();
        account.setUsername(adminPhone);
        account.setPassword(passwordEncoder.encode("123456"));
        account.setRole(RoleName.SUPER_ADMIN);
        account.setFirstLogin(false);

        Employee admin = new Employee();
        admin.setFullName("Super Administrator");
        admin.setPhoneNumber(adminPhone);
        admin.setPosition("Chu chuoi cua hang");
        admin.setAccount(account);
        employeeRepository.save(admin);

        createDefaultBranchManagers();

        return ResponseEntity.ok("Tạo SUPER_ADMIN thành công. SĐT: " + adminPhone + " / Pass: 123456. Manager mặc định: 0910000001.. với pass 123456.");
    }

    private void createDefaultBranchManagers() {
        java.util.List<Branch> branches = branchRepository.findAll();
        for (int i = 0; i < branches.size(); i++) {
            Branch branch = branches.get(i);
            String managerPhone = "091000000" + (i + 1);
            if (accountRepository.existsByUsername(managerPhone)) {
                continue;
            }

            Account managerAccount = new Account();
            managerAccount.setUsername(managerPhone);
            managerAccount.setPassword(passwordEncoder.encode("123456"));
            managerAccount.setRole(RoleName.MANAGER);
            managerAccount.setFirstLogin(true);

            Employee manager = new Employee();
            manager.setFullName("Manager " + branch.getName());
            manager.setPhoneNumber(managerPhone);
            manager.setPosition("Branch Manager");
            manager.setBranch(branch);
            manager.setAccount(managerAccount);
            employeeRepository.save(manager);
        }
    }

    private String getFullNameByRole(String username, String role) {
        if (role.contains("CUSTOMER")) {
            return customerRepository.findByAccount_Username(username)
                    .map(Customer::getFullName)
                    .orElse("Khách hàng");
        }
        return employeeRepository.findByAccount_Username(username)
                .map(Employee::getFullName)
                .orElse("Nhân viên");
    }
}
