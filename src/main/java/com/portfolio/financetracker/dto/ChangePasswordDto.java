package com.portfolio.financetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordDto {

    @NotBlank(message = "La password corrente e' obbligatoria")
    private String currentPassword;

    @NotBlank(message = "La nuova password e' obbligatoria")
    @Size(min = 8, max = 100, message = "La nuova password deve essere tra 8 e 100 caratteri")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
             message = "La nuova password deve contenere almeno una maiuscola, una minuscola e un carattere speciale")
    private String newPassword;

    @NotBlank(message = "La conferma e' obbligatoria")
    private String confirmNewPassword;

    public ChangePasswordDto() {}

    public ChangePasswordDto(String currentPassword, String newPassword, String confirmNewPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String currentPassword;
        private String newPassword;
        private String confirmNewPassword;

        public Builder currentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
            return this;
        }

        public Builder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        public Builder confirmNewPassword(String confirmNewPassword) {
            this.confirmNewPassword = confirmNewPassword;
            return this;
        }

        public ChangePasswordDto build() {
            return new ChangePasswordDto(currentPassword, newPassword, confirmNewPassword);
        }
    }
}
