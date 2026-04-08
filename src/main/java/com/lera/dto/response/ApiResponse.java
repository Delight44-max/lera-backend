package com.lera.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String status;
    private T data;
    private String message;
    private Integer results;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().status("success").data(data).build();
    }
    public static <T> ApiResponse<T> success(T data, int results) {
        return ApiResponse.<T>builder().status("success").data(data).results(results).build();
    }
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().status("error").message(message).build();
    }
}
