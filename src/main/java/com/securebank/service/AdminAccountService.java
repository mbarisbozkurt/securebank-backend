package com.securebank.service;

import com.securebank.dto.AccountResponse;
import com.securebank.dto.FundAccountRequest;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.InvalidTransferException;
import com.securebank.model.Account;
import com.securebank.model.AccountStatus;
import com.securebank.model.AuditAction;
import com.securebank.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public AdminAccountService(
            AccountRepository accountRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AccountResponse fundAccount(Long accountId, FundAccountRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        return fundAccount(account, request);
    }

    @Transactional
    public AccountResponse fundAccountByAccountNumber(String accountNumber, FundAccountRequest request) {
        String normalizedAccountNumber = accountNumber.trim().toUpperCase();
        Account account = accountRepository.findByAccountNumber(normalizedAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        return fundAccount(account, request);
    }

    private AccountResponse fundAccount(Account account, FundAccountRequest request) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransferException("Account must be active");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        auditLogService.record(
                AuditAction.ADMIN_ACCOUNT_FUNDED,
                currentUserService.getCurrentUserEmail(),
                "ACCOUNT",
                account.getId(),
                "amount=%s".formatted(request.getAmount()));

        return AccountResponse.from(account);
    }
}
