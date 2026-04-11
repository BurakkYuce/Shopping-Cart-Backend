package com.datapulse.model.enums;

public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    WALLET,
    BANK_TRANSFER,
    COD;

    public static PaymentMethod fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return CREDIT_CARD;
        }
        String normalized = raw.trim().toLowerCase().replace("-", " ").replace("_", " ");
        return switch (normalized) {
            case "credit card", "creditcard", "credit", "card" -> CREDIT_CARD;
            case "debit card", "debitcard", "debit" -> DEBIT_CARD;
            case "wallet", "e wallet", "ewallet" -> WALLET;
            case "bank transfer", "banktransfer", "net banking", "netbanking", "eft", "havale" -> BANK_TRANSFER;
            case "cash on delivery", "cod", "cash" -> COD;
            case "emi", "credit card emi" -> CREDIT_CARD;
            default -> {
                try {
                    yield PaymentMethod.valueOf(raw.trim().toUpperCase().replace(" ", "_"));
                } catch (IllegalArgumentException e) {
                    yield CREDIT_CARD;
                }
            }
        };
    }
}
