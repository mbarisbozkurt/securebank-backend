package com.securebank.dto;

import com.securebank.model.Transaction;
import com.securebank.model.TransactionStatus;
import com.securebank.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private Long fromAccountId;
    private String fromAccountNumber;
    private String fromAccountHolderName;
    private Long toAccountId;
    private String toAccountNumber;
    private String toAccountHolderName;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private Instant createdAt;

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccount().getId(),
                transaction.getFromAccount().getAccountNumber(),
                RecipientPreviewResponse.maskFullName(transaction.getFromAccount().getUser().getFullName()),
                transaction.getToAccount().getId(),
                transaction.getToAccount().getAccountNumber(),
                RecipientPreviewResponse.maskFullName(transaction.getToAccount().getUser().getFullName()),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
