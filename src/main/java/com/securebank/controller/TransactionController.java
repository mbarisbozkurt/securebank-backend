package com.securebank.controller;

import com.securebank.dto.TransactionResponse;
import com.securebank.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/me")
    public List<TransactionResponse> getMyTransactions() {
        return transactionService.getTransactionsForCurrentUser();
    }
}
