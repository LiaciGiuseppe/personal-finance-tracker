package com.portfolio.financetracker.dto;

import java.time.LocalDate;

public class TransactionFilterDto {

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private Long categoryId;

    public TransactionFilterDto() {}

    public TransactionFilterDto(LocalDate dateFrom, LocalDate dateTo, Long categoryId) {
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.categoryId = categoryId;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private Long categoryId;

        public Builder dateFrom(LocalDate dateFrom) {
            this.dateFrom = dateFrom;
            return this;
        }

        public Builder dateTo(LocalDate dateTo) {
            this.dateTo = dateTo;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public TransactionFilterDto build() {
            return new TransactionFilterDto(dateFrom, dateTo, categoryId);
        }
    }
}
