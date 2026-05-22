package com.securebank.repository;

import com.securebank.model.Account;
import com.securebank.model.Beneficiary;
import com.securebank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<Beneficiary> findByIdAndOwner(Long id, User owner);

    boolean existsByOwnerAndDestinationAccount(User owner, Account destinationAccount);
}
