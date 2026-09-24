package com.banking.accountservice.controller;


import com.banking.accountservice.DTO.AccountResponse;
import com.banking.accountservice.DTO.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.AccessType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {


    private final AccountService accountService;

    //create account ,getAcc , getBalance, blockAcc
    // saga (s1: deduct balance -> credit balance -> credit receiver or credit sender(refund))

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest createAccountRequest) {

        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(createAccountRequest));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(@PathVariable String accountNumber) {
        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account blocked");
    }

    //saga step-1
    // deduct balance Called by transaction service when transfer is initiated

     @PutMapping("/{accountNumber}/deduct")
     public ResponseEntity<String> deductBalance(@PathVariable String accountNumber
     ,@RequestParam BigDecimal amount) {
       accountService.deductBalance(accountNumber, amount);
       return ResponseEntity.ok("Balance deducted");
     }

    /*
     * SAGA STEP 4 - Compensating transaction endpoint
     *
     * CALLED BY TRANSACTION SERVICE IN TWO SCENARIOS:
     *
     * 1. Fraud detected -> refund sender (undo step 1)
     * 2. Transaction completed -> Credit receiver
     */

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ) {
        accountService.creditBalance(accountNumber, amount);
        return ResponseEntity.ok("Balance credited");
    }


}
