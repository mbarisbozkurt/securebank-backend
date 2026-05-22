package com.securebank.repository;

import com.securebank.model.Account;
import com.securebank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByIban(String iban);

    @Query("select coalesce(sum(account.balance), 0) from Account account")
    BigDecimal sumBalance();
}
