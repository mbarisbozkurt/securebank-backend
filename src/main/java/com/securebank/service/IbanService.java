package com.securebank.service;

import com.securebank.exception.InvalidTransferException;
import org.springframework.stereotype.Service;

@Service
public class IbanService {

    private static final String TURKEY_COUNTRY_CODE = "TR";
    private static final int TURKEY_IBAN_LENGTH = 26;
    private static final String TURKEY_COUNTRY_NUMERIC = "2927";

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.replaceAll("\\s+", "").toUpperCase();
    }

    public String normalizeAndValidate(String value) {
        String iban = normalize(value);

        if (!isValidTurkishIban(iban)) {
            throw new InvalidTransferException("Invalid IBAN");
        }

        return iban;
    }

    public String buildTurkishIban(String bankCode, String accountDigits) {
        String bban = bankCode + accountDigits;
        int checkDigits = 98 - mod97(bban + TURKEY_COUNTRY_NUMERIC + "00");

        return TURKEY_COUNTRY_CODE + "%02d".formatted(checkDigits) + bban;
    }

    private boolean isValidTurkishIban(String iban) {
        if (iban == null) {
            return false;
        }

        if (iban.length() != TURKEY_IBAN_LENGTH) {
            return false;
        }

        if (!iban.startsWith(TURKEY_COUNTRY_CODE)) {
            return false;
        }

        if (!iban.matches("[A-Z0-9]+")) {
            return false;
        }

        return mod97(iban.substring(4) + TURKEY_COUNTRY_NUMERIC + iban.substring(2, 4)) == 1;
    }

    private int mod97(String numericValue) {
        int remainder = 0;

        for (int i = 0; i < numericValue.length(); i++) {
            char character = numericValue.charAt(i);

            if (!Character.isDigit(character)) {
                throw new InvalidTransferException("Invalid IBAN");
            }

            remainder = (remainder * 10 + Character.digit(character, 10)) % 97;
        }

        return remainder;
    }
}
