package com.securebank.controller;

import com.securebank.dto.AccountResponse;
import com.securebank.dto.FundAccountRequest;
import com.securebank.service.AdminAccountService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @PostMapping("/{accountId}/fund")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse fundAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody FundAccountRequest request
    ) {
        return adminAccountService.fundAccount(accountId, request);
    }

    @PostMapping("/by-account-number/{accountNumber}/fund")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse fundAccountByAccountNumber(
            @PathVariable String accountNumber,
            @Valid @RequestBody FundAccountRequest request
    ) {
        return adminAccountService.fundAccountByAccountNumber(accountNumber, request);
    }
}
