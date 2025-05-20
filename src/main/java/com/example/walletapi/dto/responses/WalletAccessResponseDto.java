package com.example.walletapi.dto.responses;

import java.util.UUID;

/**
 * Data transfer object for wallet creation response that includes the balance
 * and JWT token
 */
public class WalletAccessResponseDto {
	private final UUID walletId;
	private final String token;

	public WalletAccessResponseDto(UUID walletId, String token) {
		this.walletId = walletId;
		this.token = token;
	}

	public UUID getWalletId() {
		return walletId;
	}

	public String getToken() {
		return token;
	}
}