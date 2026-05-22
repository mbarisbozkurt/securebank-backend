package com.securebank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalUsers;
    private long totalAccounts;
    private long totalTransactions;
    private BigDecimal totalSystemBalance;
    private String currency;
}
