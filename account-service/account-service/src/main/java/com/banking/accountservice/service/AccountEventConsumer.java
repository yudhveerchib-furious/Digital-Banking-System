package com.banking.accountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    /*
       Consume transaction.completed event from Kafka

     */

   // no fraud detected now we are credits to the receiver
   @KafkaListener(
           topics = "transaction.completed"
   )
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload) {
             try {
                 String receiverAccount =
                         (String) payload.get("receiverAccountNumber");
                 BigDecimal amount = new BigDecimal(payload.get("amount").toString());

                 log.info("Crediting account: {} amount: {}",  receiverAccount, amount);

                  accountService.creditBalance(receiverAccount, amount);

             } catch (Exception e) {
                 log.error("Error while credit account: {}" , e.getMessage());
                 throw new RuntimeException(e);
             }
    }

    /*
     Consume fraud.detected event from kafka and
     blocks the flagged account
     */
   @KafkaListener(
           topics = "fraud.detected"
   )
    public void consumeFraudDetected(@Payload Map<String, Object> payload) {
       try {
           String accountNumber =
                   payload.get("accountNumber").toString();

           log.info("Fraud detected - blocking account : {}" , accountNumber);
           accountService.blockAccount(accountNumber);

       } catch (Exception e) {
//           throw new RuntimeException(e);
           log.error("Error while blocking account : {}" , e.getMessage());
       }
    }
}
