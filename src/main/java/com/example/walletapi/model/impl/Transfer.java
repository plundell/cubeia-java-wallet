package com.example.walletapi.model.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.IntStream;

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

		private final Logger logger;

		public TransferFactory() {
			this.logger = Logger.getLogger(TransferFactory.class.getName());
		}

		public TransferInterface fromMap(Map<String, Serializable> data) throws IllegalArgumentException {
			if (!data.containsKey("timestamp") || !(data.get("timestamp") instanceof Long)) {
				throw new IllegalArgumentException("Transfer is missing timestamp (Long), i.e. it was never validated");
			}
			if (!data.containsKey("id") || !(data.get("id") instanceof String)) {
				throw new IllegalArgumentException("Transfer data is missing id (String).");
			}
			if (!data.containsKey("sender") || data.get("sender") == null) { // Sender can be null for deposits
				// No specific type check for sender if it can be null, or check if String when
				// not null
			} else if (!(data.get("sender") instanceof String)) {
				throw new IllegalArgumentException("Transfer sender must be a String UUID if present.");
			}
			if (!data.containsKey("recipient") || !(data.get("recipient") instanceof String)) {
				throw new IllegalArgumentException("Transfer data is missing recipient (String UUID).");
			}
			if (!data.containsKey("amount") || !(data.get("amount") instanceof BigDecimal)) {
				throw new IllegalArgumentException("Transfer data is missing amount (BigDecimal).");
			}

			try {
				String idStr = (String) data.get("id");
				String senderStr = (String) data.get("sender");
				String recipientStr = (String) data.get("recipient");

				return new Transfer(
						UUID.fromString(idStr),
						senderStr == null ? null : UUID.fromString(senderStr), // Handle nullable sender
						UUID.fromString(recipientStr),
						(BigDecimal) data.get("amount"),
						(Long) data.get("timestamp"));
			} catch (IllegalArgumentException e) { // Catch UUID.fromString errors
				throw new IllegalArgumentException("Invalid UUID string in transfer data: " + e.getMessage(), e);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid transfer data: " + e.getMessage(), e);
			}
		}

		public TransferInterface fromSendRequest(UUID sender, UUID receiver, BigDecimal amount) {
			return new Transfer(
					UUID.randomUUID(), sender, receiver, amount, 0);
		}

		/**
		 * Returns an iterator that yields TransferInterface objects from
		 * "raw ledger data", ie. a List of Maps which has been deserialized
		 * from JSON.
		 * 
		 * Invalid items in the list will be logged and skipped, but don't throw.
		 * 
		 * @param rawLedger The raw ledger data, or null.
		 * @return Null if null is passed in or the list is empty, else an Iterator of
		 *         TransferInterface.
		 * @throws IllegalArgumentException if the rawLedger is not a list.
		 */
		public Iterator<TransferInterface> getTransferIterator(Serializable rawLedger) {
			if (rawLedger == null) {
				return null;
			}
			if (!(rawLedger instanceof List)) {
				throw new IllegalArgumentException("rawLedger was a " + rawLedger.getClass().getName() + " not a List");
			}
			if (((List<?>) rawLedger).isEmpty()) {
				return null;
			} else {
				return IntStream.range(0, ((List<?>) rawLedger).size())
						.mapToObj(i -> {
							try {
								@SuppressWarnings("unchecked")
								Map<String, Serializable> mapItem = ((List<Map<String, Serializable>>) rawLedger)
										.get(i); // throws if cast fails
								return fromMap(mapItem);
							} catch (Exception e) {
								this.logger.warning(
										"Failed to deserialize transfer at index " + i + ": " + e.getMessage());
								return null;
							}
						})
						.filter(Objects::nonNull)
						.iterator();
			}
		}

	}
}