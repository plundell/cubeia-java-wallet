package com.example.walletapi.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO request to send money to another wallet
 */
public class DepositRequestDto {

	@NotNull(message = "Deposit amount is required")
	@Positive(message = "Deposit amount must be positive")
	private BigDecimal amount;

	public DepositRequestDto() {
	}

	public DepositRequestDto(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
}