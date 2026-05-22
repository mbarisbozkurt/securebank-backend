package com.securebank.dto;

import com.securebank.model.Account;
import com.securebank.model.Beneficiary;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class BeneficiaryResponse {

    private Long id;
    private String accountNumber;
    private String iban;
    private String recipientName;
    private String nickname;
    private String currency;
    private Instant createdAt;

    public static BeneficiaryResponse from(Beneficiary beneficiary) {
        Account account = beneficiary.getDestinationAccount();

        return new BeneficiaryResponse(
                beneficiary.getId(),
                account.getAccountNumber(),
                account.getIban(),
                RecipientPreviewResponse.maskFullName(account.getUser().getFullName()),
                beneficiary.getNickname(),
                account.getCurrency(),
                beneficiary.getCreatedAt()
        );
    }
}
