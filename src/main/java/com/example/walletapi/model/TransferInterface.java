package com.example.walletapi.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Iterator;
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
		/**
		 * Should return a TransferInterface object from a sender, receiver, and amount.
		 * 
		 * @param sender   The sender of the transfer.
		 * @param receiver The receiver of the transfer.
		 * @param amount   The amount of the transfer.
		 * @return A TransferInterface object.
		 */
		TransferInterface fromSendRequest(UUID sender, UUID receiver, BigDecimal amount);

		/**
		 * Should return a TransferInterface object from a Map of String keys and
		 * Serializable values.
		 * 
		 * @param data The Map of String keys and Serializable values.
		 * @return A TransferInterface object.
		 */
		TransferInterface fromMap(Map<String, Serializable> data);

		/**
		 * Should return a lazy iterator that yields TransferInterface objects from
		 * "raw ledger data", ie. a List of Maps which has been deserialized
		 * from JSON.
		 * 
		 * Invalid items should be logged and skipped, but not throw.
		 * 
		 * @param rawLedger The raw ledger data, or null.
		 * @return Null if null is passed in or the list is empty, else an Iterator of
		 *         TransferInterface.
		 * @throws IllegalArgumentException if the rawLedger is not a list.
		 */
		Iterator<TransferInterface> getTransferIterator(Serializable rawLedger);

	}
}
