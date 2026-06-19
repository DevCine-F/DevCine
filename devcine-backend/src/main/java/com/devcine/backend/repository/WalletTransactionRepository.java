package com.devcine.backend.repository;

import com.devcine.backend.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {
    List<WalletTransaction> findAllByWalletIdOrderByCreatedAtDesc(Integer walletId);
}
