package com.banking.transactionservice.entity;
/*
  Transaction lifecycle
  c1:  pending ->processing -> completed (when clean)
  c2: pending -> processing -> sus found -> bank verify with user via otp or phone call else flag it and block the account and saga refund
  ->failed
  ->flagged
 */


public enum TransactionStatus {
    PENDING,
    PROCESSING,
    PENDING_VERIFICATION,
    COMPLETED,
    FAILED,
    FLAGGED
}
