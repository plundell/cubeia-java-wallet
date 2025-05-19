package com.example.walletapi.dto.responses;

import java.util.List;

import com.example.walletapi.model.TransferInterface;

public class LedgerResponseDto {
	private final List<TransferInterface> transfers;
	private final long timestamp;

	/**
	 * Constructor for LedgerResponseDto
	 * 
	 * @param transfers The list of transfers to be added to the ledger
	 */
	public LedgerResponseDto(List<TransferInterface> transfers, long timestamp) {
		this.transfers = transfers;
		this.timestamp = timestamp;
	}

	public List<TransferInterface> getTransfers() {
		return transfers;
	}

	public long getTimestamp() {
		return timestamp;
	}
}