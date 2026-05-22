package com.securebank.repository;

import com.securebank.model.Account;
import com.securebank.model.Transaction;
import com.securebank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccount_UserOrToAccount_UserOrderByCreatedAtDesc(User fromUser, User toUser);

    List<Transaction> findByFromAccountOrToAccountOrderByCreatedAtDesc(Account fromAccount, Account toAccount);
}
