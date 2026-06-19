package com.devcine.backend.controller;

import com.devcine.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getWallet(@PathVariable Integer customerId) {
        try {
            var wallet = walletService.getOrCreateWallet(customerId);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<?> getTransactions(@PathVariable Integer walletId) {
        return ResponseEntity.ok(walletService.getTransactions(walletId));
    }

    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<?> deposit(@PathVariable Integer walletId,
                                      @RequestParam BigDecimal amount,
                                      @RequestParam(required = false) String description) {
        try {
            var wallet = walletService.deposit(walletId, amount, description);
            return ResponseEntity.ok(Map.of("success", true, "data", wallet));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Giữ lại endpoint mock-deposit cho dev/testing
    @PostMapping("/{walletId}/mock-deposit")
    public ResponseEntity<?> mockDeposit(@PathVariable Integer walletId,
                                          @RequestParam BigDecimal amount) {
        return deposit(walletId, amount, "Nạp tiền giả lập (Mock Deposit)");
    }
}
