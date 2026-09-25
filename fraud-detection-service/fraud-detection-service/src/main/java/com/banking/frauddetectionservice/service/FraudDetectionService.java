package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.client.AccountServiceClient;
import com.banking.frauddetectionservice.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {


    @Value("${fraud.max-transaction-per-minute}")
    private int maxTransPerMin;

    @Value("${fraud.suspicious-amount-multiplier}")
    private double susAmountMultiplier;

    @Value("${fraud.max-balance-percentage}")
    private double maxBalancePercentage;

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC = "fraud.check.clean";

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final AccountServiceClient accountServiceClient;

    public void checkTransaction(Map<String, Object> payload) {

        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("senderAccountNumber");
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        //fetch real balance from account service
        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);

        log.info("Checking transaction : {} account balance : {}", transactionId, senderBalance);

      FraudCheckResult result = performFraudChecks(accountNumber,amount,senderBalance);

      if(result.isFraud()) {

          log.info("Suspicious activity detected :{}" +
                  "reason: {} - requesting otp verification",
                  accountNumber, result.getReason());

          Map<String, Object> verificationEvent = new HashMap<>();

          verificationEvent.put("transactionId", transactionId);
          verificationEvent.put("accountNumber", accountNumber);
          verificationEvent.put("amount", amount);
          verificationEvent.put("reason", result.getReason());

          kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, transactionId,verificationEvent);
      } else {
          //Transaction is clean
          log.info("Cleaned transaction");

          Map<String, Object> transactionCleanEvent = new HashMap<>();

          transactionCleanEvent.put("transactionId", transactionId);
          transactionCleanEvent.put("isFraud", false);
          transactionCleanEvent.put("reason", null);

          kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC, transactionId, transactionCleanEvent);

      }


    }

    private FraudCheckResult performFraudChecks(
            String accountNumber,
            BigDecimal amount,
            BigDecimal senderBalance) {

        // pattern 1: velocity check
        if(isVelocityExceeded(accountNumber)) {
            return new FraudCheckResult(
                    true,
                    "Too many transaction in 60 seconds" +
                            " velocity limit is exceeded"
            );
        }

        // pattern 2: Amount check
        if(isAmountSus(accountNumber, amount)) {
            return new FraudCheckResult(
                    true,
                    "Unusual transaction amount" +
                            " - exceeds 3x your average"
            );
        }

        // pattern 3: balance check
        if(senderBalance.compareTo(BigDecimal.ZERO) > 0 &&
                isBalanceCheckFailed(senderBalance, amount)) {

            return new FraudCheckResult(
                    true,
                    "Transaction exceeds 90% of account balance"
            );
        }

        return new FraudCheckResult(false, null);
    }

    private boolean isBalanceCheckFailed(BigDecimal senderBalance, BigDecimal amount) {
        BigDecimal maxAllowed =
                senderBalance.multiply(BigDecimal.valueOf(maxBalancePercentage));

        log.info("Sus");
        return amount.compareTo(maxAllowed) > 0;
    }

    private boolean isAmountSus(String accountNumber, BigDecimal amount) {
        String avgKey = "fraud:avg_amount" + accountNumber;
        String avgStr = redisTemplate.opsForValue().get(avgKey);

        if(avgStr == null) {
             redisTemplate.opsForValue().set(avgKey, amount.toString());
             return false;
        }

        BigDecimal avg = new BigDecimal(avgStr);
        BigDecimal threshold = avg.multiply(BigDecimal.valueOf(susAmountMultiplier));

        //update running avg
        BigDecimal newAvg =
                avg.add(amount).divide(BigDecimal.valueOf(2),2, RoundingMode.HALF_UP);

        redisTemplate.opsForValue().set(avgKey, newAvg.toString());

        log.info("Amount check - amount : {} threshold : {} sus: {}" ,
                amount, threshold, amount.compareTo(threshold) > 0);

        return amount.compareTo(threshold) > 0;
    }

    //we gonna use redis in order to maintain the count
    private boolean isVelocityExceeded(String accountNumber) {
         String key = "fraud:velocity" + accountNumber;
         Long count = redisTemplate.opsForValue().increment(key);

         if(count != null && count == 1) {
             redisTemplate.expire(key,60, TimeUnit.SECONDS);
         }

         log.info("Velocity check - account : {} count :{}/{} ", accountNumber, count, maxTransPerMin);

         return count != null && count > maxTransPerMin;
    }
}
