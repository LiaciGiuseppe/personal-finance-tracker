package com.portfolio.financetracker.controller;

import com.portfolio.financetracker.model.Transaction;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.UserRepository;
import com.portfolio.financetracker.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public DashboardController(UserRepository userRepository, TransactionService transactionService) {
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        try {
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utente non trovato"));

            log.debug("User found: {}", currentUser.getUsername());

            List<Transaction> recentTransactions = transactionService.findAll(currentUser, null);

            log.debug("Transactions found: {}", recentTransactions.size());

            List<Transaction> limited = recentTransactions.stream()
                    .limit(5)
                    .toList();

            Map<String, BigDecimal> summary = transactionService.calculateSummary(limited);

            log.debug("Summary: income={}, expense={}, balance={}",
                    summary.get("totalIncome"), summary.get("totalExpense"), summary.get("balance"));

            model.addAttribute("transactions", limited);
            model.addAttribute("summary", summary);

            return "dashboard/index";
        } catch (Exception e) {
            log.error("Dashboard error", e);
            throw e;
        }
    }
}
