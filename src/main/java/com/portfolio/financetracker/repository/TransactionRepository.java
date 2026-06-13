package com.portfolio.financetracker.repository;

import com.portfolio.financetracker.model.Transaction;
import com.portfolio.financetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    void deleteByUser(User user);
}
