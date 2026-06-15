package com.portfolio.financetracker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.financetracker.model.Transaction;
import com.portfolio.financetracker.model.TransactionType;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
            loadDashboardData(userDetails, model);
            return "dashboard/index";
        } catch (Exception e) {
            log.error("Dashboard error", e);
            throw e;
        }
    }

    private void loadDashboardData(UserDetails userDetails, Model model) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        List<Transaction> allTransactions = transactionService.findAll(currentUser, null);

        List<Transaction> limited = allTransactions.stream()
                .limit(5)
                .toList();

        Map<String, BigDecimal> summary = transactionService.calculateSummary(allTransactions);

        model.addAttribute("transactions", limited);
        model.addAttribute("summary", summary);

        // Running balance over time for the chart
        List<Transaction> ascending = new ArrayList<>(allTransactions);
        Collections.reverse(ascending);

        List<Map<String, Object>> balancePoints = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        for (Transaction t : ascending) {
            if (t.getType() == TransactionType.INCOME) {
                runningBalance = runningBalance.add(t.getAmount());
            } else {
                runningBalance = runningBalance.subtract(t.getAmount());
            }
            Map<String, Object> point = new HashMap<>();
            point.put("d", t.getDate().toString());
            point.put("b", runningBalance);
            balancePoints.add(point);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            model.addAttribute("chartData", mapper.writeValueAsString(balancePoints));
        } catch (JsonProcessingException e) {
            log.error("Error serializing chart data", e);
            model.addAttribute("chartData", "[]");
        }
    }
}
