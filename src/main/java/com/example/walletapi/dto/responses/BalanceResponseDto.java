package com.example.walletapi.dto.responses;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for wallet balance responses.
 */
public class BalanceResponseDto {
	private String walletId;
	private BigDecimal balance;
	private long timestamp;

	public BalanceResponseDto(UUID id, BigDecimal balance, long timestamp) {
		this.walletId = id.toString();
		this.balance = balance;
		this.timestamp = timestamp;
	}

	public String getWalletId() {
		return walletId;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public long getTimestamp() {
		return timestamp;
	}

}