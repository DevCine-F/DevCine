package com.devcine.backend.service;

import com.devcine.backend.entity.Customer;
import com.devcine.backend.entity.Wallet;
import com.devcine.backend.entity.WalletTransaction;
import com.devcine.backend.repository.CustomerRepository;
import com.devcine.backend.repository.WalletRepository;
import com.devcine.backend.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public Wallet getOrCreateWallet(Integer customerId) {
        return walletRepository.findByCustomerUserId(customerId).orElseGet(() -> {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
            Wallet wallet = Wallet.builder()
                    .customer(customer)
                    .balance(BigDecimal.ZERO)
                    .status("ACTIVE")
                    .build();
            return walletRepository.save(wallet);
        });
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactions(Integer walletId) {
        return walletTransactionRepository.findAllByWalletIdOrderByCreatedAtDesc(walletId);
    }

    @Transactional
    public Wallet deposit(Integer walletId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền nạp phải lớn hơn 0");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction wt = WalletTransaction.builder()
                .wallet(wallet)
                .type("DEPOSIT")
                .amount(amount)
                .description(description != null ? description : "Nạp tiền vào ví")
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(wt);

        log.info("Deposited {} to wallet {}", amount, walletId);
        return wallet;
    }

    @Transactional
    public void debit(Integer customerId, BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findByCustomerUserId(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư ví không đủ");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction wt = WalletTransaction.builder()
                .wallet(wallet)
                .type("PAYMENT")
                .amount(amount.negate())
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(wt);
    }
}
