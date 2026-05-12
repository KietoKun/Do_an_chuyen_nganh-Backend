package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class StaffOrderRequest extends OrderRequest {

    @Schema(description = "ID khach hang da co trong he thong. Neu co, he thong uu tien truong nay.", example = "12")
    private Long customerId;

    @Schema(description = "So dien thoai khach hang. Dung de tim hoac tao khach hang khi khong truyen customerId.", example = "0901234567")
    private String customerPhoneNumber;

    @Schema(description = "Ho ten khach hang. Bat buoc khi tao khach hang moi.", example = "Nguyen Van A")
    private String customerFullName;

    @Schema(description = "Email khach hang khi tao moi.", example = "customer@example.com")
    private String customerEmail;

    @Schema(description = "ID chi nhanh tao don. SUPER_ADMIN co the truyen, nhan vien/quan ly chi duoc dung chi nhanh cua minh.", example = "1")
    private Long branchId;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerPhoneNumber() {
        return customerPhoneNumber;
    }

    public void setCustomerPhoneNumber(String customerPhoneNumber) {
        this.customerPhoneNumber = customerPhoneNumber;
    }

    public String getCustomerFullName() {
        return customerFullName;
    }

    public void setCustomerFullName(String customerFullName) {
        this.customerFullName = customerFullName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }
}
