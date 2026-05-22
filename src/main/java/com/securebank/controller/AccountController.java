package com.securebank.controller;

import com.securebank.dto.AccountResponse;
import com.securebank.dto.RecipientPreviewResponse;
import com.securebank.dto.TransactionResponse;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.InvalidTransferException;
import com.securebank.service.AccountService;
import com.securebank.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public AccountResponse createAccount() {
        return accountService.createAccountForCurrentUser();
    }

    @GetMapping("/me")
    public List<AccountResponse> getMyAccounts() {
        return accountService.getAccountsForCurrentUser();
    }

    @GetMapping("/resolve")
    public RecipientPreviewResponse resolveRecipient(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String iban
    ) {
        if (iban != null && !iban.isBlank()) {
            return accountService.resolveRecipientByIban(iban);
        }

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidTransferException("Destination account is required");
        }

        return accountService.resolveRecipientByAccountNumber(accountNumber);
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionResponse> getAccountTransactions(@PathVariable Long accountId) {
        return transactionService.getTransactionsForAccount(accountId);
    }
}
