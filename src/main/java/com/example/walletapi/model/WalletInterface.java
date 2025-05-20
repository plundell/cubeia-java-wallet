package com.example.walletapi.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.LedgerResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.ResourceConflictException;

public interface WalletInterface extends Serializable {
	/**
	 * Get the ID of the wallet.
	 */
	UUID getId();

	/**
	 * Get the current balance of the wallet as a float (as opposed to an
	 * AtomicReference which is how it's stored internally).
	 */
	BigDecimal getBalance();

	/**
	 * Get the current balance along with a timestamp when it was valid.
	 * 
	 * @throws ResourceConflictException if the balance is being updated by another
	 *                                   thread.
	 */
	BalanceResponseDto getBalanceDto() throws ResourceConflictException;

	/**
	 * Get a snapshot of the transaction history for this wallet as a regular
	 * list (as opposed to a ConcurrentLinkedQueue which is how it's stored
	 * internally).
	 */
	List<TransferInterface> getLedger();

	/**
	 * Get a snapshot of the transaction history for this wallet along with a
	 * timestamp when it was valid.
	 * 
	 * @throws ResourceConflictException if the ledger is being updated by another
	 *                                   thread.
	 */
	LedgerResponseDto getLedgerDto() throws ResourceConflictException;

	/**
	 * Create a transfer to send money to a destination wallet.
	 * 
	 * @param destination The destination wallet ID.
	 * @param amount      The amount to send.
	 * @return The transfer that was added to the ledger.
	 * @throws InsufficientFundsException if the balance is insufficient.
	 */
	TransferResponseDto sendMoney(WalletInterface destination, BigDecimal amount)
			throws InsufficientFundsException, ResourceConflictException;

	/**
	 * Receive money from another wallet.
	 * 
	 * @param transfer The transfer to receive.
	 */
	void receiveMoney(TransferInterface transfer);

}
