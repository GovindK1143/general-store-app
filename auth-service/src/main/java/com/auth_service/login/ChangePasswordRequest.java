
package com.auth_service.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(
                min = 8,
                max = 72,
                message = "New password must contain 8 to 72 characters"
        )
        String newPassword

) {
}
