package com.banking.accountservice.repo;

import com.banking.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepo extends JpaRepository<Account,String> {
     Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);
}
