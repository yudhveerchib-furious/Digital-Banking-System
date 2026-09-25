package com.banking.transactionservice.service;

import com.banking.transactionservice.DTO.TransactionResponse;
import com.banking.transactionservice.DTO.TransferRequest;
import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.entity.TransactionType;
import com.banking.transactionservice.event.TransactionInitiatedEvent;
import com.banking.transactionservice.repo.TransactionRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepo  transactionRepo;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";

    /**
     * SAGA STEP - 1: Initiate transfer
     * Deducts from sender via feign
     * Saves transaction as PROCESSING.
     * Publish event to kafka for fraud check
     * Returns.
     * @param request
     * @return
     */

    public TransactionResponse transfer( TransferRequest request) {
        log.info("SAGA START - TRANSFER :{} -> {} : AMOUNT :{}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount());
        // s1: saga -> deduct from sender
        accountServiceClient.deductBalance(
               request.getSenderAccountNumber(),
               request.getAmount()
        );

        Transaction transaction =
                new Transaction();
           transaction.setSenderAccountNumber(request.getSenderAccountNumber());
           transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
           transaction.setAmount(request.getAmount());
           transaction.setType(TransactionType.TRANSFER);
           transaction.setStatus(TransactionStatus.PROCESSING);
           transaction.setDescription(request.getDescription());
           transaction.setReferenceNumber(UUID.randomUUID().toString());

           Transaction savedTransaction = transactionRepo.save(transaction);
           log.info("Transaction saved as processing :{}" , savedTransaction.getId());

        TransactionInitiatedEvent event =
                new TransactionInitiatedEvent(
                        savedTransaction.getId(),
                        savedTransaction.getSenderAccountNumber(),
                        savedTransaction.getReceiverAccountNumber(),
                        savedTransaction.getAmount(),
                        savedTransaction.getDescription()
                );

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, savedTransaction.getId(), event);
        log.info("SAGA STEP 2 : PUBLISH FOR FRAUD TEST");

         return mapToResponse(savedTransaction);

    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setSenderAccountNumber(
                transaction.getSenderAccountNumber());
        response.setReceiverAccountNumber(
                transaction.getReceiverAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setFailureReason(transaction.getFailureReason());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());

        return response;
    }

    public TransactionResponse getTransaction(String id) {
        return mapToResponse(transactionRepo.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("Not found id")
                ));
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {

          return transactionRepo.findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                  .stream()
                  .map(this::mapToResponse)
                  .collect(Collectors.toList());


    }
}
