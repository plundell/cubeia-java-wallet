package com.example.walletapi.dto.responses;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import com.example.walletapi.model.TransferInterface;

/**
 * DTO for transfer responses.
 */
public class TransferResponseDto {
	private final UUID transferId;
	private final String recipientWalletId;
	private final BigDecimal amount;
	private final BigDecimal remainingBalance;
	private final long timestamp;

	public TransferResponseDto(TransferInterface transfer, BigDecimal remainingBalance) {
		this.transferId = transfer.getId();
		this.recipientWalletId = transfer.getRecipient().toString();
		this.amount = transfer.getAmount();
		this.remainingBalance = remainingBalance;
		this.timestamp = transfer.getTimestamp();
	}

	public UUID getTransferId() {
		return transferId;
	}

	public String getRecipientWalletId() {
		return recipientWalletId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public BigDecimal getRemainingBalance() {
		return remainingBalance;
	}
}