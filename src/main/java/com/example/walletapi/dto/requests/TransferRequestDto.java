package com.example.walletapi.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO request to send money to another wallet
 */
public class TransferRequestDto {

	@NotBlank(message = "Destination wallet ID is required")
	private String destinationWalletId;

	@NotNull(message = "Transfer amount is required")
	@Positive(message = "Transfer amount must be positive")
	private BigDecimal amount;

	public TransferRequestDto() {
	}

	public TransferRequestDto(String destinationWalletId, BigDecimal amount) {
		this.destinationWalletId = destinationWalletId;
		this.amount = amount;
	}

	public String getDestinationWalletId() {
		return destinationWalletId;
	}

	public void setDestinationWalletId(String destinationWalletId) {
		this.destinationWalletId = destinationWalletId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
}