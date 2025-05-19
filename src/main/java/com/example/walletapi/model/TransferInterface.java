package com.example.walletapi.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.example.walletapi.exception.ResourceConflictException;

public interface TransferInterface extends Serializable {
	UUID getId();

	UUID getSender();

	UUID getRecipient();

	BigDecimal getAmount();

	/**
	 * Get the signed amount from on party's point of view (suitable when summing a
	 * ledger)
	 */
	BigDecimal getAmount(UUID asWho);

	long getTimestamp();

	/**
	 * Validates the transfer (which creates and sets a timestamp). This should be
	 * called after
	 * the transfer has been added to both sending and receiving ledgers.
	 * 
	 * @throws ResourceConflictException if the transfer is already validated.
	 */
	TransferInterface validate() throws ResourceConflictException;

	public interface TransferFactoryInterface {
		TransferInterface fromSendRequest(UUID sender, UUID receiver, BigDecimal amount);

		TransferInterface fromMap(Map<String, Serializable> data);

		TransferInterface fromDepositRequest(UUID receiver, BigDecimal amount);
	}
}
