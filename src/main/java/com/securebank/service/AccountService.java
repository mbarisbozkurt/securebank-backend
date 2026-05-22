package com.securebank.service;

import com.securebank.dto.AccountResponse;
import com.securebank.dto.RecipientPreviewResponse;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.InvalidTransferException;
import com.securebank.model.Account;
import com.securebank.model.AuditAction;
import com.securebank.model.User;
import com.securebank.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {

    private static final String ACCOUNT_NUMBER_PREFIX = "SB";
    private static final int ACCOUNT_NUMBER_RANDOM_DIGITS = 12;
    private static final String IBAN_BANK_CODE = "00062";
    private static final int IBAN_ACCOUNT_DIGITS = 17;

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AuditLogService auditLogService;
    private final IbanService ibanService;

    public AccountService(
            AccountRepository accountRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            IbanService ibanService) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.ibanService = ibanService;
    }

    public AccountResponse createAccountForCurrentUser() {
        User currentUser = currentUserService.getCurrentUser();

        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .iban(generateUniqueIban())
                .user(currentUser)
                .build();

        Account savedAccount = accountRepository.save(account);
        auditLogService.record(
                AuditAction.ACCOUNT_CREATED,
                currentUser.getEmail(),
                "ACCOUNT",
                savedAccount.getId(),
                "Account created");

        return AccountResponse.from(savedAccount);
    }

    public List<AccountResponse> getAccountsForCurrentUser() {
        User currentUser = currentUserService.getCurrentUser();

        return accountRepository.findByUser(currentUser)
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecipientPreviewResponse resolveRecipientByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        return RecipientPreviewResponse.from(account);
    }

    @Transactional(readOnly = true)
    public RecipientPreviewResponse resolveRecipientByIban(String iban) {
        Account account = accountRepository.findByIban(ibanService.normalizeAndValidate(iban))
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        return RecipientPreviewResponse.from(account);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;

        do {
            accountNumber = ACCOUNT_NUMBER_PREFIX + randomDigits();
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }

    private String randomDigits() {
        StringBuilder builder = new StringBuilder(ACCOUNT_NUMBER_RANDOM_DIGITS);

        for (int i = 0; i < ACCOUNT_NUMBER_RANDOM_DIGITS; i++) {
            builder.append(secureRandom.nextInt(10));
        }

        return builder.toString();
    }

    private String generateUniqueIban() {
        String iban;

        do {
            iban = ibanService.buildTurkishIban(IBAN_BANK_CODE, randomIbanAccountDigits());
        } while (accountRepository.findByIban(iban).isPresent());

        return iban;
    }

    private String randomIbanAccountDigits() {
        StringBuilder builder = new StringBuilder(IBAN_ACCOUNT_DIGITS);

        for (int i = 0; i < IBAN_ACCOUNT_DIGITS; i++) {
            builder.append(secureRandom.nextInt(10));
        }

        return builder.toString();
    }
}
