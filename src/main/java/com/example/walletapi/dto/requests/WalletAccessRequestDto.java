package com.example.walletapi.dto.requests;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public class WalletAccessRequestDto {
	@NotBlank(message = "Wallet ID is required")
	private UUID walletId;
	@NotBlank(message = "Password is required")
	private String password;

	public WalletAccessRequestDto() {
	}

	public WalletAccessRequestDto(UUID walletId, String password) {
		this.walletId = walletId;
		this.password = password;
	}

	public UUID getWalletId() {
		return walletId;
	}

	public String getPassword() {
		return password;
	}

	public void setWalletId(UUID walletId) {
		this.walletId = walletId;
	}

	public void setWalletId(String walletId) {
		this.walletId = UUID.fromString(walletId);
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
