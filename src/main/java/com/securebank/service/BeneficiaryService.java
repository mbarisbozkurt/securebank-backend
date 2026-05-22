package com.securebank.service;

import com.securebank.dto.BeneficiaryRequest;
import com.securebank.dto.BeneficiaryResponse;
import com.securebank.dto.UpdateBeneficiaryRequest;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.InvalidTransferException;
import com.securebank.model.Account;
import com.securebank.model.Beneficiary;
import com.securebank.model.User;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.BeneficiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;
    private final IbanService ibanService;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            AccountRepository accountRepository,
            CurrentUserService currentUserService,
            IbanService ibanService
    ) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
        this.ibanService = ibanService;
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiariesForCurrentUser() {
        User owner = currentUserService.getCurrentUser();

        return beneficiaryRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(BeneficiaryResponse::from)
                .toList();
    }

    @Transactional
    public BeneficiaryResponse createBeneficiary(BeneficiaryRequest request) {
        User owner = currentUserService.getCurrentUser();
        Account destinationAccount = findDestinationAccount(request);

        if (destinationAccount.getUser().getId().equals(owner.getId())) {
            throw new InvalidTransferException("Choose a different destination account");
        }

        if (beneficiaryRepository.existsByOwnerAndDestinationAccount(owner, destinationAccount)) {
            throw new InvalidTransferException("Recipient is already saved");
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .owner(owner)
                .destinationAccount(destinationAccount)
                .nickname(normalizeNickname(request.getNickname()))
                .build();

        return BeneficiaryResponse.from(beneficiaryRepository.save(beneficiary));
    }

    @Transactional
    public void deleteBeneficiary(Long beneficiaryId) {
        User owner = currentUserService.getCurrentUser();
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndOwner(beneficiaryId, owner)
                .orElseThrow(() -> new AccountNotFoundException("Saved recipient not found"));

        beneficiaryRepository.delete(beneficiary);
    }

    @Transactional
    public BeneficiaryResponse updateBeneficiary(Long beneficiaryId, UpdateBeneficiaryRequest request) {
        User owner = currentUserService.getCurrentUser();
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndOwner(beneficiaryId, owner)
                .orElseThrow(() -> new AccountNotFoundException("Saved recipient not found"));

        beneficiary.setNickname(normalizeNickname(request.getNickname()));

        return BeneficiaryResponse.from(beneficiary);
    }

    private Account findDestinationAccount(BeneficiaryRequest request) {
        if (request.getIban() != null && !request.getIban().isBlank()) {
            String normalizedIban = ibanService.normalizeAndValidate(request.getIban());

            return accountRepository.findByIban(normalizedIban)
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));
        }

        if (request.getAccountNumber() != null && !request.getAccountNumber().isBlank()) {
            return accountRepository.findByAccountNumber(request.getAccountNumber().trim())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));
        }

        throw new InvalidTransferException("Destination account is required");
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }

        return nickname.trim();
    }
}
