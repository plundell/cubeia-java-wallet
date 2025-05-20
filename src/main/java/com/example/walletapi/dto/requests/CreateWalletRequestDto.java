package com.example.walletapi.dto.requests;

import jakarta.validation.constraints.NotBlank;

public class CreateWalletRequestDto {

	@NotBlank(message = "Password is required to create a wallet")
	private String password;

	public CreateWalletRequestDto() {
	}

	public CreateWalletRequestDto(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
