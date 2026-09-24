package com.banking.accountservice.service;

import com.banking.accountservice.DTO.AccountResponse;
import com.banking.accountservice.DTO.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repo.AccountRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepo accountRepo;

    private static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(@Valid CreateAccountRequest createAccountRequest) {
     log.info("Creating account for : {}" , createAccountRequest.getEmail());

      if(accountRepo.existsByEmail(createAccountRequest.getEmail())) {
          throw new RuntimeException("Account already exists");
      }

        Account account = new Account();

        account.setAccountHolderName(createAccountRequest.getAccountHolderName());
        account.setEmail(createAccountRequest.getEmail());
        account.setPhone(createAccountRequest.getPhone());
        account.setAccountType(createAccountRequest.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(createAccountRequest.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber()); // we need to generate it like must be of 12 and unique
        account.setDailyTransactionLimit(
                createAccountRequest.getAccountType() == AccountType.SAVINGS
                        ? new BigDecimal("100000")
                        : new BigDecimal("500000")
        );

        Account savedAccount = accountRepo.save(account);
        log.info("Saved account : {}" , savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);
    }

    private String generateAccountNumber() {
         String accountNumber;
         do {
             long number = secureRandom.nextLong(1_000_000_000_000L);
             accountNumber = String.format("%012d", number);
         }while(accountRepo.existsByAccountNumber(accountNumber));

         return accountNumber;
    }

    private AccountResponse mapToResponse(Account savedAccount) {
        AccountResponse response = new AccountResponse();
        response.setId(savedAccount.getId());
        response.setAccountNumber(savedAccount.getAccountNumber());
        response.setAccountHolderName(savedAccount.getAccountHolderName());
        response.setEmail(savedAccount.getEmail());
        response.setPhone(savedAccount.getPhone());
        response.setAccountType(savedAccount.getAccountType());
        response.setStatus(savedAccount.getStatus());
        response.setBalance(savedAccount.getBalance());
        response.setDailyTransactionLimit(savedAccount.getDailyTransactionLimit());
        response.setCreatedAt(savedAccount.getCreatedAt());

        return response;
    }

    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepo.findByAccountNumber(accountNumber).orElseThrow(
                () -> new RuntimeException("Account not found")
        );

        return  mapToResponse(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepo.findByAccountNumber(accountNumber).orElseThrow(
                () -> new RuntimeException("Account not found")
        );
        return account.getBalance();
    }

    public void blockAccount(String accountNumber) {
       log.info("Blocking account : {}" , accountNumber);
       Account account = accountRepo.findByAccountNumber(accountNumber).orElseThrow(
               () -> new RuntimeException("Account not found")
       );

       account.setStatus(AccountStatus.BLOCKED);
       accountRepo.save(account);
       log.info("Account blocked : {}" , account.getAccountNumber());
    }

    public void deductBalance(String accountNumber, BigDecimal amount) {
       log.info("Deducting balance from  account : {}" , accountNumber);
        Account account = accountRepo.findByAccountNumber(accountNumber).orElseThrow(
                () -> new RuntimeException("Account not found")
        );

        if(account.getStatus() !=  AccountStatus.ACTIVE) {
            throw new  RuntimeException("Account not active" + accountNumber);
        }

        if(account.getBalance().compareTo(amount) <= 0) {
            throw new RuntimeException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);



        log.info("Balance updated : {}" , account.getBalance());
    }
  /*
    credit balance called by transaction service via kafka
   */
    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("Credit balance from  account : {}" , accountNumber);
        Account account = accountRepo.findByAccountNumber(accountNumber).orElseThrow(
                () -> new RuntimeException("Account not found")
        );

        account.setBalance(account.getBalance().add(amount));

        log.info("Credit balance updated : {}" , account.getBalance());
    }
}
