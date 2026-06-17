package com.portfolio.financetracker.service;

import com.portfolio.financetracker.dto.TransactionFilterDto;
import com.portfolio.financetracker.exception.ResourceNotFoundException;
import com.portfolio.financetracker.model.Role;
import com.portfolio.financetracker.model.Transaction;
import com.portfolio.financetracker.model.TransactionType;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.TransactionRepository;
import com.portfolio.financetracker.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              PushNotificationService pushNotificationService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Recupera le transazioni dell'utente applicando i filtri ricevuti.
     *
     * @param currentUser utente corrente
     * @param filter      DTO con i filtri opzionali
     * @return lista di transazioni filtrate e ordinate per data decrescente
     */
    public List<Transaction> findAll(User currentUser, TransactionFilterDto filter) {
        Specification<Transaction> spec = buildSpecification(currentUser, filter);
        return transactionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "date"));
    }

    public Page<Transaction> findAll(User currentUser, TransactionFilterDto filter, Pageable pageable) {
        Specification<Transaction> spec = buildSpecification(currentUser, filter);
        return transactionRepository.findAll(spec, pageable);
    }

    /**
     * Recupera una transazione per id, verificando che appartenga all'utente.
     *
     * @param id          id transazione
     * @param currentUser utente corrente
     * @return la transazione
     * @throws ResourceNotFoundException se non trovata o non appartiene all'utente
     */
    public Transaction findById(Long id, User currentUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transazione non trovata con id: " + id));
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Transazione non trovata con id: " + id);
        }
        return transaction;
    }

    /**
     * Salva una nuova transazione associandola all'utente corrente.
     *
     * @param transaction i dati della transazione
     * @param currentUser utente corrente
     * @return la transazione salvata
     */
    @Transactional
    public Transaction save(Transaction transaction, User currentUser) {
        transaction.setUser(currentUser);
        Transaction saved = transactionRepository.save(transaction);
        checkAndNotifyNegativeBalance(currentUser);
        return saved;
    }

    /**
     * Aggiorna una transazione esistente dopo aver verificato la proprietà.
     *
     * @param id          id transazione
     * @param updatedData nuovi dati
     * @param currentUser utente corrente
     * @return la transazione aggiornata
     */
    @Transactional
    public Transaction update(Long id, Transaction updatedData, User currentUser) {
        Transaction existing = findById(id, currentUser);
        existing.setAmount(updatedData.getAmount());
        existing.setType(updatedData.getType());
        existing.setDate(updatedData.getDate());
        existing.setDescription(updatedData.getDescription());
        existing.setCategory(updatedData.getCategory());
        existing.setPaymentMethod(updatedData.getPaymentMethod());
        existing.setIsPlanned(updatedData.getIsPlanned());
        existing.setRecurrence(updatedData.getRecurrence());
        existing.setPlannedDate(updatedData.getPlannedDate());
        Transaction saved = transactionRepository.save(existing);
        checkAndNotifyNegativeBalance(currentUser);
        return saved;
    }

    /**
     * Elimina una transazione dopo aver verificato la proprietà.
     *
     * @param id          id transazione
     * @param currentUser utente corrente
     */
    @Transactional
    public void delete(Long id, User currentUser) {
        Transaction transaction = findById(id, currentUser);
        transactionRepository.delete(transaction);
        checkAndNotifyNegativeBalance(currentUser);
    }

    /**
     * Calcola il riepilogo delle transazioni: totale entrate, totale uscite e saldo.
     *
     * @param transactions lista di transazioni
     * @return mappa con chiavi "totalIncome", "totalExpense", "balance"
     */
    public Map<String, BigDecimal> calculateSummary(List<Transaction> transactions) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else {
                totalExpense = totalExpense.add(t.getAmount());
            }
        }

        Map<String, BigDecimal> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("balance", totalIncome.subtract(totalExpense));
        return summary;
    }

    private void checkAndNotifyNegativeBalance(User user) {
        boolean hasRoleUser = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch("ROLE_USER"::equals);
        if (!hasRoleUser) {
            return;
        }

        List<Transaction> allTransactions = transactionRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("user").get("id"), user.getId())
        );

        BigDecimal balance = allTransactions.stream()
                .map(t -> t.getType() == TransactionType.INCOME ? t.getAmount() : t.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            if (!user.getNegativeAlertSent()) {
                pushNotificationService.sendNegativeBalanceAlert(user, balance);
                user.setNegativeAlertSent(true);
                userRepository.save(user);
            }
        } else {
            if (user.getNegativeAlertSent()) {
                user.setNegativeAlertSent(false);
                userRepository.save(user);
            }
        }
    }

    /**
     * Costruisce la Specification JPA per i filtri dinamici.
     */
    private Specification<Transaction> buildSpecification(User user, TransactionFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), user.getId()));

            if (filter != null) {
                if (filter.getDateFrom() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), filter.getDateFrom()));
                }
                if (filter.getDateTo() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), filter.getDateTo()));
                }
                if (filter.getCategoryId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("category").get("id"), filter.getCategoryId()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
