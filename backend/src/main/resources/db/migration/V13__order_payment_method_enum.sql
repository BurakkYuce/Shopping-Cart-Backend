-- V13: Normalize orders.payment_method to PaymentMethod enum values
-- Must run before JPA @Enumerated(STRING) validation kicks in, otherwise any legacy
-- lowercase or CSV-seeded value will fail entity load.
UPDATE orders
SET payment_method = CASE
    WHEN payment_method IS NULL OR TRIM(payment_method) = '' THEN 'CREDIT_CARD'
    WHEN LOWER(TRIM(payment_method)) IN ('credit card', 'creditcard', 'credit', 'card', 'emi', 'credit card emi') THEN 'CREDIT_CARD'
    WHEN LOWER(TRIM(payment_method)) IN ('debit card', 'debitcard', 'debit') THEN 'DEBIT_CARD'
    WHEN LOWER(TRIM(payment_method)) IN ('wallet', 'e wallet', 'ewallet', 'e-wallet') THEN 'WALLET'
    WHEN LOWER(TRIM(payment_method)) IN ('bank transfer', 'banktransfer', 'net banking', 'netbanking', 'eft', 'havale') THEN 'BANK_TRANSFER'
    WHEN LOWER(TRIM(payment_method)) IN ('cash on delivery', 'cod', 'cash') THEN 'COD'
    ELSE 'CREDIT_CARD'
END;
