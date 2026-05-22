package com.securebank.service;

import com.securebank.dto.TransactionResponse;
import com.securebank.dto.TransferRequest;
import com.securebank.event.TransferCompletedEvent;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.InsufficientBalanceException;
import com.securebank.exception.InvalidTransferException;
import com.securebank.model.Account;
import com.securebank.model.AccountStatus;
import com.securebank.model.AuditAction;
import com.securebank.model.Transaction;
import com.securebank.model.User;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.TransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final IbanService ibanService;
    private final TransferEventPublisher transferEventPublisher;

    public TransferService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            IbanService ibanService,
            TransferEventPublisher transferEventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.ibanService = ibanService;
        this.transferEventPublisher = transferEventPublisher;
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        Account fromAccount = findAccount(request.getFromAccountId());
        Account toAccount = findDestinationAccount(request);

        validateTransfer(request, currentUser, fromAccount, toAccount);

        BigDecimal amount = request.getAmount();
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        Transaction transaction = Transaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(amount)
                .currency(fromAccount.getCurrency())
                .description(normalizeOptionalText(request.getDescription()))
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        auditLogService.record(
                AuditAction.TRANSFER_COMPLETED,
                currentUser.getEmail(),
                "TRANSACTION",
                savedTransaction.getId(),
                "fromAccountId=%d,toAccountId=%d,amount=%s".formatted(
                        fromAccount.getId(),
                        toAccount.getId(),
                        amount));
        publishTransferCompletedAfterCommit(savedTransaction);

        return TransactionResponse.from(savedTransaction);
    }

    private void publishTransferCompletedAfterCommit(Transaction transaction) {
        TransferCompletedEvent event = new TransferCompletedEvent(
                transaction.getId(),
                transaction.getFromAccount().getId(),
                transaction.getToAccount().getId(),
                transaction.getFromAccount().getUser().getEmail(),
                transaction.getToAccount().getUser().getEmail(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            transferEventPublisher.publishTransferCompleted(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                transferEventPublisher.publishTransferCompleted(event);
            }
        });
    }

    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    private Account findDestinationAccount(TransferRequest request) {
        String iban = ibanService.normalize(request.getToIban());
        String accountNumber = normalizeOptionalText(request.getToAccountNumber());

        if (iban != null) {
            iban = ibanService.normalizeAndValidate(iban);
            return accountRepository.findByIban(iban)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        }

        if (accountNumber != null) {
            return accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        }

        if (request.getToAccountId() != null) {
            return findAccount(request.getToAccountId());
        }

        throw new InvalidTransferException("Destination account is required");
    }

    private void validateTransfer(TransferRequest request, User currentUser, Account fromAccount, Account toAccount) {
        if (!fromAccount.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can transfer only from your own accounts");
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE || toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransferException("Both accounts must be active");
        }

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new InvalidTransferException("Currency mismatch");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
