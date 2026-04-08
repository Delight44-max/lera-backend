package com.lera.dto.request;

import com.lera.model.Role;
import com.lera.model.ResponderType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 2) private String fullName;
    @NotBlank @Email        private String email;
    @NotBlank @Size(min = 10) private String phoneNumber;
    @NotBlank @Size(min = 6)  private String password;
    @NotNull                   private Role role;
    private String certificationId;
    private ResponderType responderType;
}
