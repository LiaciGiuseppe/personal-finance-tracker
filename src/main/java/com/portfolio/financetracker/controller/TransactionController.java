package com.portfolio.financetracker.controller;

import com.portfolio.financetracker.dto.TransactionFilterDto;
import com.portfolio.financetracker.model.RecurrenceType;
import com.portfolio.financetracker.model.Transaction;
import com.portfolio.financetracker.model.TransactionType;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.CategoryRepository;
import com.portfolio.financetracker.repository.PaymentMethodRepository;
import com.portfolio.financetracker.repository.UserRepository;
import com.portfolio.financetracker.service.TransactionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public TransactionController(TransactionService transactionService, UserRepository userRepository,
                                 CategoryRepository categoryRepository, PaymentMethodRepository paymentMethodRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @GetMapping
    public String listTransactions(@ModelAttribute TransactionFilterDto filter,
                                    @RequestParam(defaultValue = "0") int page,
                                    @AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = getUser(userDetails);
        PageRequest pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "date"));
        Page<Transaction> transactionPage = transactionService.findAll(currentUser, filter, pageable);
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categoryRepository.findAll());
        return "transactions/list";
    }

    @GetMapping("/new")
    public String newTransactionForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("paymentMethods", paymentMethodRepository.findAll());
        return "transactions/form";
    }

    @PostMapping("/new")
    public String createTransaction(
            @RequestParam BigDecimal amount,
            @RequestParam TransactionType type,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(defaultValue = "false") boolean isPlanned,
            @RequestParam(required = false) RecurrenceType recurrence,
            @RequestParam(required = false) LocalDate plannedDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getUser(userDetails);

        Transaction transaction = Transaction.builder()
                .amount(amount)
                .type(type)
                .date(date)
                .description(description)
                .isPlanned(isPlanned)
                .recurrence(isPlanned ? recurrence : null)
                .plannedDate(isPlanned ? plannedDate : null)
                .build();

        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(transaction::setCategory);
        }
        if (paymentMethodId != null) {
            paymentMethodRepository.findById(paymentMethodId).ifPresent(transaction::setPaymentMethod);
        }

        transactionService.save(transaction, currentUser);
        return "redirect:/transactions";
    }

    @GetMapping("/{id}/edit")
    public String editTransactionForm(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = getUser(userDetails);
        model.addAttribute("transaction", transactionService.findById(id, currentUser));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("paymentMethods", paymentMethodRepository.findAll());
        return "transactions/form";
    }

    @PostMapping("/{id}/edit")
    public String updateTransaction(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam TransactionType type,
            @RequestParam LocalDate date,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(defaultValue = "false") boolean isPlanned,
            @RequestParam(required = false) RecurrenceType recurrence,
            @RequestParam(required = false) LocalDate plannedDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getUser(userDetails);

        Transaction transaction = Transaction.builder()
                .amount(amount)
                .type(type)
                .date(date)
                .description(description)
                .isPlanned(isPlanned)
                .recurrence(isPlanned ? recurrence : null)
                .plannedDate(isPlanned ? plannedDate : null)
                .build();

        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(transaction::setCategory);
        }
        if (paymentMethodId != null) {
            paymentMethodRepository.findById(paymentMethodId).ifPresent(transaction::setPaymentMethod);
        }

        transactionService.update(id, transaction, currentUser);
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/delete")
    public String deleteTransaction(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getUser(userDetails);
        transactionService.delete(id, currentUser);
        return "redirect:/transactions";
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));
    }
}
