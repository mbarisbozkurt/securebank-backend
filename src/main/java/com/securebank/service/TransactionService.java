package com.securebank.service;

import com.securebank.dto.TransactionResponse;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.model.Account;
import com.securebank.model.User;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            CurrentUserService currentUserService
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsForCurrentUser() {
        User currentUser = currentUserService.getCurrentUser();

        return transactionRepository.findByFromAccount_UserOrToAccount_UserOrderByCreatedAtDesc(currentUser, currentUser)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsForAccount(Long accountId) {
        User currentUser = currentUserService.getCurrentUser();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can view transactions only for your own accounts");
        }

        return transactionRepository.findByFromAccountOrToAccountOrderByCreatedAtDesc(account, account)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
