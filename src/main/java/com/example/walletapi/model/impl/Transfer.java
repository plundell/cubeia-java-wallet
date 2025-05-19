package com.example.walletapi.model.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.walletapi.exception.ResourceConflictException;
import com.example.walletapi.model.TransferInterface;

/**
 * Represents a transfer.
 * 
 * NOTE: The transfer should not be considered complete until it has a
 * timestamp. We make use of that in deserialization to ensure that
 * non-validated transfers fail to deserialize.
 * 
 * TODO: we need to make sure that TransferFactory.fromMap() is actually called
 * for ^ to be true. Also look at Wallet(Map)...
 */
public class Transfer implements TransferInterface {
	private UUID id;
	private final UUID sender;
	private final UUID recipient;
	private final BigDecimal amount;
	private long timestamp;

	protected Transfer(UUID id, UUID sender, UUID recipient, BigDecimal amount, long timestamp) {
		this.id = id;
		this.sender = sender;
		this.recipient = recipient;
		this.amount = amount;
		this.timestamp = timestamp;
	}

	public UUID getId() {
		return id;
	}

	public UUID getSender() {
		return sender;
	}

	public UUID getRecipient() {
		return recipient;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getAmount(UUID asWho) {
		if (this.sender == asWho) {
			return this.amount.negate();
		} else if (this.recipient == asWho) {
			return this.amount;
		} else {
			throw new IllegalArgumentException("Invalid UUID. Please use sender or recipient UUID.");
		}
	}

	public long getTimestamp() {
		return timestamp;
	}

	public TransferInterface validate() {
		if (this.timestamp > 0) {
			throw new ResourceConflictException("Transfer already validated");
			// Maybe not the correct type of exception to throw here???
		} else {
			this.timestamp = System.currentTimeMillis();
			return this;
		}
	}

	@Component
	public static class TransferFactory implements TransferFactoryInterface {

		public TransferInterface fromMap(Map<String, Serializable> data) {
			if (!data.containsKey("timestamp") || !(data.get("timestamp") instanceof Long)) {
				throw new IllegalArgumentException("Transfer is missing timestamp, i.e. it was never validated");
			}
			try {
				return new Transfer(
						(UUID) data.get("id"),
						(UUID) data.get("sender"),
						(UUID) data.get("recipient"),
						(BigDecimal) data.get("amount"),
						(Long) data.get("timestamp"));
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid transfer data", e);
			}
		}

		public TransferInterface fromSendRequest(UUID sender, UUID receiver, BigDecimal amount) {
			return new Transfer(
					UUID.randomUUID(), sender, receiver, amount, 0);
		}

		public TransferInterface fromDepositRequest(UUID receiver, BigDecimal amount) {
			return new Transfer(
					UUID.randomUUID(), null, receiver, amount, 0);

		}

	}
}