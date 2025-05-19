package com.example.walletapi.model.impl;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.LedgerResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.ResourceConflictException;
import com.example.walletapi.exception.ServerErrorException;
import com.example.walletapi.model.TransferInterface;
import com.example.walletapi.model.TransferInterface.TransferFactoryInterface;
import com.example.walletapi.model.WalletInterface;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Represents a wallet which holds a balance and can initiate transfers.
 */
public class Wallet implements WalletInterface {

	/**
	 * The maximum timeout in milliseconds to try performing concurrent actions
	 * which are hitting locked resources.
	 */
	@Value("${wallet.api.max-concurrent-retries-timeout:10000}")
	private int maxConcurrentRetriesTimeout;

	private final UUID id;

	private Logger logger;

	/**
	 * The balance of the wallet.
	 * 
	 * We are choosing to store the balance as an AtomicReference instead of simply
	 * summing up the ledger every time someone wants the balance.
	 * 
	 * The reasoning is performance, especially we want to avoid locking the ledger
	 * for longer periods as much as possible so we can always receive money from
	 * other wallets. While summing the ledger would be fast at first, after
	 * thousands of transactions it could become a bottleneck.
	 * 
	 * The downside is possible inconsistency between balance and legder if care is
	 * not taken.
	 */
	private final AtomicReference<BigDecimal> balance = new AtomicReference<>(BigDecimal.ZERO);

	private final ConcurrentLinkedQueue<TransferInterface> ledger = new ConcurrentLinkedQueue<>();

	@Autowired
	private TransferFactoryInterface transferFactory;

	/**
	 * Constructs a wallet from a map of data. Suitable for deserialization.
	 * 
	 * NOTE: We ignore balance stored in the data, and instead calculate it from the
	 * ledger.
	 * 
	 * @param data The data to construct the wallet from.
	 * @throws IllegalArgumentException if the data is invalid.
	 */
	public Wallet(Map<String, Serializable> data) throws IllegalArgumentException {
		try {
			this.id = (UUID) data.get("id");

			@SuppressWarnings("unchecked")
			List<Map<String, Serializable>> ledgerMap = (List<Map<String, Serializable>>) data.get("ledger");

			BigDecimal _balance = BigDecimal.ZERO;
			for (Map<String, Serializable> transferMap : ledgerMap) {
				try {
					TransferInterface transfer = transferFactory.fromMap(transferMap);
					this.ledger.add(transfer);
					_balance = _balance.add(transfer.getAmount());
				} catch (Exception e) {
					this.getLogger().severe("Failed to add transfer to ledger: " + e.getMessage());
				}
			}
			this.balance.set(_balance);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid wallet data: " + e.getMessage());
		}
	}

	/**
	 * Constructs a new wallet with a random ID and zero balance.
	 */
	public Wallet() {
		this.id = UUID.randomUUID();
	}

	private Logger getLogger() {
		if (this.logger == null) {
			this.logger = Logger.getLogger(Wallet.class.getName() + ":" + this.id);
		}
		return this.logger;
	}

	/**
	 * Gets the ID of the wallet.
	 */
	public UUID getId() {
		return this.id;
	}

	/**
	 * Gets the current balance of the wallet as a float (as opposed to an
	 * AtomicReference which is how it's stored internally).
	 */
	public BigDecimal getBalance() {
		return this.balance.get();
	}

	/**
	 * Gets the current balance along with a timestamp when it was valid.
	 * 
	 * @throws ResourceConflictException if the balance keeps being updated by other
	 *                                   threads so we can't get a lock for long
	 *                                   enough to get a consistent balance.
	 */
	public BalanceResponseDto getBalanceDto() throws ResourceConflictException {

		long timeout = System.currentTimeMillis() + this.maxConcurrentRetriesTimeout;
		long timestamp;
		BigDecimal _balance;
		while (System.currentTimeMillis() < timeout) {
			_balance = this.getBalance();
			timestamp = System.currentTimeMillis();
			if (_balance.compareTo(this.getBalance()) == 0) {
				return new BalanceResponseDto(this.id, _balance, timestamp);
			}
			this.sleep("getting balance");
		}
		throw new ResourceConflictException("Failed to get a consistent balance for wallet " + this.id);
	}

	/**
	 * Retrieves a snapshot of the transaction history for this wallet as a regular
	 * list (as opposed to a ConcurrentLinkedQueue which is how it's stored
	 * internally).
	 */
	public List<TransferInterface> getLedger() {
		return new ArrayList<>(this.ledger);
	}

	/**
	 * Gets a snapshot of the transaction history for this wallet along with a
	 * timestamp when it was valid.
	 * 
	 */
	public LedgerResponseDto getLedgerDto() throws ResourceConflictException {
		// Don't run for longer than the timeout...
		long timeout = System.currentTimeMillis() + this.maxConcurrentRetriesTimeout;
		while (System.currentTimeMillis() < timeout) {
			int len = this.ledger.size();
			long timestamp = System.currentTimeMillis();
			List<TransferInterface> _ledger = new ArrayList<>(this.ledger);
			if (len == _ledger.size()) {
				// It's enough to just check the size since we never remove anything from the
				// ledger, money is simply returned with a second entry if something goes wrong.
				return new LedgerResponseDto(_ledger, timestamp);
			}
			this.sleep("getting ledger");

		}
		throw new ResourceConflictException(
				"Failed to get a snapshot of the ledger for wallet " + this.id
						+ " as a result of concurrent modification");

	}

	private void sleep(String what) throws ServerErrorException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			String msg = "Interrupted while " + what + " for wallet " + this.id;
			this.getLogger().severe(msg + ": " + e.getMessage());
			throw new ServerErrorException(msg);
		}
	}

	/**
	 * Create a transfer to send money to a destination wallet.
	 * 
	 * @param destination The destination wallet ID.
	 * @param amount      The amount to send.
	 * @return The transfer that was added to the ledger.
	 * @throws InsufficientFundsException if the balance is insufficient.
	 */
	public TransferResponseDto sendMoney(WalletInterface destination, BigDecimal amount)
			throws InsufficientFundsException, ResourceConflictException {

		// In case multiple threads are trying to send money from the same wallet
		// it will prevent us from updating the atomic reference, so keep trying
		// until we timeout, or run out of money, or succeed
		BigDecimal _balance;
		BigDecimal _remainingBalance;
		long timeout = System.currentTimeMillis() + this.maxConcurrentRetriesTimeout;
		while (true) {
			if (System.currentTimeMillis() > timeout) {
				throw new ResourceConflictException(
						"Failed to send " + amount + " to wallet " + destination.getId()
								+ " as a result of concurrent modification on wallet " + this.id);
			}

			// Check if the balance is sufficient else throw an exception
			_balance = this.balance.get();
			if (_balance.compareTo(amount) < 0) {
				throw new InsufficientFundsException("Cannot transfer " + amount + " to wallet " + destination.getId()
						+ " since the balance of wallet " + this.id + " is only " + _balance);
			}
			// Try to update the balance, and if we succeed, break out of the loop
			_remainingBalance = _balance.subtract(amount);
			if (this.balance.compareAndSet(_balance, _remainingBalance)) {
				break;
			}
			this.sleep("sending money");
		}

		// Now we have allocated the money which means nobody can double spend it,
		// therefore creating and adding the transfers to the sending and receiving
		// ledgers doesn't have to be atomic. However, if any of these operations
		// fail, we need to revert what's been done so far.
		TransferInterface transfer = transferFactory.fromSendRequest(this.id, destination.getId(), amount);
		try {
			this.ledger.add(transfer);
			try {
				destination.receiveMoney(transfer); // This will also validate the transfer
			} catch (Exception e) {
				this.getLogger()
						.severe("Failed to add transfer to receiving ledger after having added it "
								+ "to sender, reverting both sending ledger and balance." + e.getMessage());
				this.ledger.remove(transfer);
				throw new Exception("AAA");

			}
		} catch (Exception e) {
			if (!e.getMessage().equals("AAA")) {
				this.getLogger()
						.severe("Failed to add transfer to ledger after having allocated money. "
								+ "Reverting the balance." + e.getMessage());
			}
			this.balance.getAndUpdate(b -> b.add(amount));
			throw new ResourceConflictException(
					"Failed to send " + amount + " to wallet " + destination.getId()
							+ " as a result of concurrent modification on wallet " + this.id);
		}

		return new TransferResponseDto(transfer, _remainingBalance);

	}

	/**
	 * Receive money from another wallet.
	 * 
	 * @param transfer The transfer to receive.
	 */
	public void receiveMoney(TransferInterface transfer) {

		// First we we add the transfer (receipt) to the ledger...
		this.ledger.add(transfer);

		// ...then we validate it since it now exists in both ledgers
		transfer.validate();

		// Finally we increase the balance so it can be spent (here we use
		// getAndUpdate() instead of compareAndSet() because there is no risk
		// of going below zero.
		this.balance.getAndUpdate(balance -> balance.add(transfer.getAmount()));

	}

	public BalanceResponseDto depositMoney(BigDecimal amount) {
		TransferInterface transfer = transferFactory.fromDepositRequest(this.id, amount);
		this.receiveMoney(transfer);
		return this.getBalanceDto();
	}
}