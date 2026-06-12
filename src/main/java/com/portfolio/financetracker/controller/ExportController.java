package com.portfolio.financetracker.controller;

import com.portfolio.financetracker.dto.TransactionFilterDto;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.CategoryRepository;
import com.portfolio.financetracker.repository.UserRepository;
import com.portfolio.financetracker.service.ExportService;
import com.portfolio.financetracker.service.TransactionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class ExportController {

    private final ExportService exportService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public ExportController(ExportService exportService, TransactionService transactionService,
                            UserRepository userRepository, CategoryRepository categoryRepository) {
        this.exportService = exportService;
        this.transactionService = transactionService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute TransactionFilterDto filter,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        var transactions = transactionService.findAll(currentUser, filter);
        byte[] excelBytes = exportService.generateExcel(transactions);

        String filename = buildFilename(currentUser.getId(), filter);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".xlsx\"")
                .body(excelBytes);
    }

    private String buildFilename(Long userId, TransactionFilterDto filter) {
        StringBuilder sb = new StringBuilder();
        sb.append(userId).append("_transaction");

        boolean hasCriteria = false;

        if (filter != null) {
            if (filter.getDateFrom() != null) {
                sb.append("_da_").append(filter.getDateFrom().toString());
                hasCriteria = true;
            }
            if (filter.getDateTo() != null) {
                sb.append("_a_").append(filter.getDateTo().toString());
                hasCriteria = true;
            }
            if (filter.getCategoryId() != null) {
                categoryRepository.findById(filter.getCategoryId()).ifPresent(cat -> {
                    String catName = cat.getName().toLowerCase().replaceAll("[^a-z0-9]", "_");
                    sb.append("_cat_").append(catName);
                });
                hasCriteria = true;
            }
        }

        if (!hasCriteria) {
            sb.append("_riepilogo_totale");
        }

        return sb.toString();
    }
}
