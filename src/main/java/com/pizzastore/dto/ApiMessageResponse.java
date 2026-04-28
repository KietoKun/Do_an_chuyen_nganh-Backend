package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiMessageResponse", description = "Thong diep tra ve don gian tu he thong")
public class ApiMessageResponse {
    @Schema(description = "Noi dung thong bao ket qua xu ly", example = "Dang ky thanh cong. Email thong bao da duoc gui.")
    private final String message;

    public ApiMessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
