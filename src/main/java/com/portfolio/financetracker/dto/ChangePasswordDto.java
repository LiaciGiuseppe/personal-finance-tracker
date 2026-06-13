package com.portfolio.financetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordDto {

    @NotBlank(message = "La password corrente è obbligatoria")
    private String currentPassword;

    @NotBlank(message = "La nuova password è obbligatoria")
    @Size(min = 8, max = 100, message = "La nuova password deve essere tra 8 e 100 caratteri")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
             message = "La nuova password deve contenere almeno una maiuscola, una minuscola e un carattere speciale")
    private String newPassword;

    @NotBlank(message = "La conferma è obbligatoria")
    private String confirmNewPassword;
}
