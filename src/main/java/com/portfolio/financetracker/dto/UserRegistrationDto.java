package com.portfolio.financetracker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationDto {

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Inserire un indirizzo email valido")
    private String username;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, max = 100, message = "La password deve essere tra 8 e 100 caratteri")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
             message = "La password deve contenere almeno una maiuscola, una minuscola e un carattere speciale")
    private String password;

    @NotBlank(message = "La conferma password è obbligatoria")
    private String confirmPassword;
}
