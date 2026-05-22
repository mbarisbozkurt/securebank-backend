package com.securebank.dto;

import com.securebank.model.Account;
import com.securebank.model.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecipientPreviewResponse {

    private String accountNumber;
    private String iban;
    private String recipientName;
    private String currency;
    private AccountStatus status;

    public static RecipientPreviewResponse from(Account account) {
        return new RecipientPreviewResponse(
                account.getAccountNumber(),
                account.getIban(),
                maskFullName(account.getUser().getFullName()),
                account.getCurrency(),
                account.getStatus()
        );
    }

    public static String maskFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "SecureBank customer";
        }

        String[] parts = fullName.trim().split("\\s+");
        String firstName = parts[0];

        if (parts.length == 1) {
            return firstName;
        }

        String lastName = parts[parts.length - 1];
        return firstName + " " + lastName.charAt(0) + ".";
    }
}
