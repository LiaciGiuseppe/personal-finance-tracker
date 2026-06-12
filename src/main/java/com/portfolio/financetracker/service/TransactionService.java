package com.portfolio.financetracker.service;

import com.portfolio.financetracker.dto.TransactionFilterDto;
import com.portfolio.financetracker.exception.ResourceNotFoundException;
import com.portfolio.financetracker.model.Transaction;
import com.portfolio.financetracker.model.TransactionType;
import com.portfolio.financetracker.model.User;
import com.portfolio.financetracker.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
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
    public Transaction save(Transaction transaction, User currentUser) {
        transaction.setUser(currentUser);
        return transactionRepository.save(transaction);
    }

    /**
     * Aggiorna una transazione esistente dopo aver verificato la proprietà.
     *
     * @param id          id transazione
     * @param updatedData nuovi dati
     * @param currentUser utente corrente
     * @return la transazione aggiornata
     */
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
        return transactionRepository.save(existing);
    }

    /**
     * Elimina una transazione dopo aver verificato la proprietà.
     *
     * @param id          id transazione
     * @param currentUser utente corrente
     */
    public void delete(Long id, User currentUser) {
        Transaction transaction = findById(id, currentUser);
        transactionRepository.delete(transaction);
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
