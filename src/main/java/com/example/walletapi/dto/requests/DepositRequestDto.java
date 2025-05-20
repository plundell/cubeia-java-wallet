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

	@NotBlank(message = "Deposit token is required")
	private String token;

	public DepositRequestDto() {
	}

	public DepositRequestDto(BigDecimal amount, String token) {
		this.amount = amount;
		this.token = token;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}