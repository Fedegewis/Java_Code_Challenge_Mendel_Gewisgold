package com.challenge.mendel.repository;

import com.challenge.mendel.domain.Transaction;
import java.util.List;

public interface TransactionRepository {

    void save(Transaction transaction);

    Transaction findById(Long id);

    List<Transaction> findAll();

    boolean existsById(Long id);

    List<Transaction> findByType(String type);
}