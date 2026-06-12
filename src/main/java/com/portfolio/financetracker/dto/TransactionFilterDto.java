package com.portfolio.financetracker.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFilterDto {

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private Long categoryId;
}
