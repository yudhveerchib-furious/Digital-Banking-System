package com.banking.transactionservice.repo;

import com.banking.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction,String> {

    List<Transaction> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);

}
