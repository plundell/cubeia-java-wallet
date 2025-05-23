package com.example.walletapi.service.impl;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.model.WalletInterface.WalletFactoryInterface;
import com.example.walletapi.model.WalletInterface;
import com.example.walletapi.service.WalletServiceInterface;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;

import java.util.concurrent.ConcurrentMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Singleton service for wallet operations.
 */
@Service
public class WalletService implements WalletServiceInterface {
	private final ConcurrentMap<UUID, WalletInterface> wallets = new ConcurrentHashMap<>();
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final WalletFactoryInterface walletFactory;

	@Value("${bank.wallet.id:00000000-0000-0000-0000-000000000000}")
	private UUID bankWalletId;

	@Value("${bank.wallet.initial-deposit:1000000000}")
	private BigDecimal bankWealth;

	@Value("${wallet.data.file:wallet-data.ser}")
	private String walletDataFile;

	@Value("${wallet.data.dir:./test_data}")
	private String walletDataDir;

	@Autowired
	public WalletService(WalletFactoryInterface walletFactory) {
		this.walletFactory = walletFactory;
	}

	// @PostConstruct //trying to load wallets on request instead
	public void loadWalletDataFromJsonFiles() {
		File dataDir = new File(walletDataDir);
		if (!dataDir.exists()) {
			logger.info("Wallet data directory not found at " + walletDataDir + ", creating...");
			try {
				dataDir.mkdirs();
			} catch (Exception e) {
				logger.warn("Failed to create wallet data directory. Nothing will be stored.", e);
			}
		} else {
			File[] jsonFiles = dataDir.listFiles((dir, name) -> name.endsWith(".json"));
			if (jsonFiles == null || jsonFiles.length == 0) {
				logger.info("No JSON files found in " + walletDataDir);
			} else {
				logger.info("Found " + jsonFiles.length + " wallets in " + walletDataDir);
				for (File jsonFile : jsonFiles) {
					try {
						Map<String, Serializable> data = new ObjectMapper()
								.readValue(jsonFile, new TypeReference<Map<String, Serializable>>() {
								});
						WalletInterface wallet = this.walletFactory.fromMap(data);
						wallets.put(wallet.getId(), wallet);
					} catch (Exception e) {
						logger.warn("Failed to load wallet data from file " + jsonFile.getName(), e);
					}
				}
			}
		}
	}

	private String getWalletDataFilePath(UUID walletId) {
		return walletDataDir + "/" + walletId.toString() + ".json";
	}

	/**
	 * Loads a wallet from a JSON file, stores it in the wallets map and returns it.
	 * 
	 * @param walletId The ID of the wallet to load
	 * @return The wallet
	 * @throws NotFoundException if the wallet file doesn't exist OR if the file has
	 *                           a bad format
	 */
	private WalletInterface loadWalletFromJsonFile(UUID walletId) throws NotFoundException {
		File jsonFile = new File(getWalletDataFilePath(walletId));
		if (!jsonFile.exists()) {
			throw new NotFoundException("No wallet file found at: " + jsonFile.getAbsolutePath());
		}
		try {
			logger.info("Loading wallet data from file " + jsonFile.getName());
			Map<String, Serializable> data = new ObjectMapper()
					.readValue(jsonFile, new TypeReference<Map<String, Serializable>>() {
					});
			WalletInterface wallet = this.walletFactory.fromMap(data);
			if (!wallet.getId().equals(walletId)) {
				throw new NotFoundException("Wallet ID in filename and in file data didn't match:" +
						"filename: " + walletId + ", file data: " + wallet.getId());
			}
			this.wallets.put(wallet.getId(), wallet);
			return wallet;
		} catch (Exception e) {
			logger.warn("Failed to load wallet data from file " + jsonFile.getName(), e);
			throw new NotFoundException("Failed to load wallet data from file " + jsonFile.getName());
		}
	}

	private WalletInterface getBankWallet() {
		try {
			return this.getWalletUnathenticated(bankWalletId);
		} catch (NotFoundException e) {
			this.logger.info("Bank wallet not found, creating...");
			return createBankWallet();
		}
	}

	/**
	 * Register shutdown hook to ensure data is saved.
	 * 
	 * In reality this is NOT enough persistence, but for this demo we're not
	 * focusing on persistence.
	 */
	@PostConstruct
	public void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("Shutdown hook triggered, saving wallet data...");
			saveWalletDataToJsonFiles();
		}));
	}

	/**
	 * Saves all wallets' data to individual JSON files.
	 */
	public void saveWalletDataToJsonFiles() {
		for (WalletInterface wallet : this.wallets.values()) {
			saveWalletDataToJsonFile(wallet);
		}
	}

	/**
	 * Saves a wallet's data to a JSON file.
	 * 
	 * @param wallet       The wallet to save
	 * @param objectMapper The ObjectMapper to use
	 * @return True if the save was successful, false otherwise
	 */
	public void saveWalletDataToJsonFile(WalletInterface wallet) {
		CompletableFuture.runAsync(() -> {
			String filename = walletDataDir + "/????.json"; // so we have something to log
			try {
				filename = getWalletDataFilePath(wallet.getId());
				File jsonFile = new File(filename);
				new ObjectMapper().writeValue(jsonFile, wallet);
				// FIXME: Seems this writes a JSON object without the closing }
				logger.info("ASYNC: Saved wallet data to " + filename);
			} catch (Exception e) {
				logger.warn("ASYNC: Failed to save wallet data to " + filename, e);
			}
		});
	}

	/**
	 * Retrieves a wallet by its ID.
	 * 
	 * @param walletId The ID of the wallet to retrieve
	 * @param password The password of the wallet
	 * 
	 * @return The wallet
	 * @throws NotFoundException if the wallet doesn't exist or if the password
	 *                           doesn't match (to prevent sniffing)
	 */
	public WalletInterface getWallet(UUID walletId, String password) throws NotFoundException {
		try {

			WalletInterface wallet = this.getWalletUnathenticated(walletId);
			if (passwordEncoder.matches(password, wallet.getPassword())) {
				return wallet;
			} else {
				this.logger.warn("Password mismatch for wallet " + walletId
						+ ". Throwing a NotFoundException.");
				throw new NotFoundException("");
			}
		} catch (NotFoundException e) {
			// Make sure the same error goes out for both cases to prevent sniffing
			throw new NotFoundException(
					"No wallet with id " + walletId + " and that password found. Please check the id and try again.");
		}
	}

	/**
	 * Retrieves a wallet by its ID WITHOUT performing password validation.
	 * 
	 * @param walletId The ID of the wallet to retrieve
	 * @return The wallet
	 * @throws NotFoundException if the wallet doesn't exist
	 */
	public WalletInterface getWalletUnathenticated(UUID walletId) throws NotFoundException {
		WalletInterface wallet = this.wallets.get(walletId);
		if (wallet == null) {
			wallet = loadWalletFromJsonFile(walletId); // also stores it for next time
		}
		if (wallet == null) {
			logger.warn("No wallet with id " + walletId + " found. These are the available wallets: "
					+ this.wallets.keySet());
			throw new NotFoundException("No wallet with id " + walletId + " found. Please check the id and try again.");
		}
		return wallet;
	}

	/**
	 * Creates a new wallet with an initial balance of zero.
	 * 
	 * @param password The password which will be used to access the wallet
	 * 
	 * @return The newly created wallet
	 */
	public WalletInterface createWallet(String clearTextPassword) {
		if (clearTextPassword == null || clearTextPassword.isEmpty()) {
			throw new IllegalArgumentException("Password is required to create a wallet");
		}
		String encodedPassword = this.passwordEncoder.encode(clearTextPassword);
		WalletInterface wallet = this.walletFactory.generateNew(encodedPassword);
		this.wallets.put(wallet.getId(), wallet);
		saveWalletDataToJsonFile(wallet);
		return wallet;
	}

	private WalletInterface createBankWallet() {
		Map<String, Serializable> initialDeposit = new HashMap<>();
		initialDeposit.put("id", UUID.randomUUID());
		initialDeposit.put("sender", this.bankWalletId);
		initialDeposit.put("recipient", this.bankWalletId);
		initialDeposit.put("amount", bankWealth);
		initialDeposit.put("timestamp", System.currentTimeMillis());

		List<Map<String, Serializable>> bankLedger = new ArrayList<>();
		bankLedger.add(initialDeposit);

		Map<String, Serializable> bankWalletData = new HashMap<>();
		bankWalletData.put("id", this.bankWalletId);
		bankWalletData.put("password", "thiswillnevermatchanything");
		bankWalletData.put("ledger", (Serializable) bankLedger);

		WalletInterface bankWallet = this.walletFactory.fromMap(bankWalletData);
		this.wallets.put(bankWallet.getId(), bankWallet);
		return bankWallet;
	}

	/**
	 * Sends money from one wallet to another.
	 * 
	 * @param sourceWalletId      The ID of the source wallet
	 * @param destinationWalletId The ID of the destination wallet
	 * @param amount              The amount of money to send
	 * @return The transfer
	 * @throws InsufficientFundsException if the source wallet doesn't have enough
	 *                                    money
	 * @throws NotFoundException          if the source or destination wallet
	 *                                    doesn't exist
	 */
	public TransferResponseDto sendMoney(UUID sourceWalletId, UUID destinationWalletId, BigDecimal amount)
			throws InsufficientFundsException, NotFoundException {
		if (sourceWalletId == null || destinationWalletId == null || amount == null) {
			throw new IllegalArgumentException("Invalid arguments");
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be greater than zero");
		}
		WalletInterface sourceWallet = this.getWalletUnathenticated(sourceWalletId);
		WalletInterface destinationWallet = this.getWalletUnathenticated(destinationWalletId);
		TransferResponseDto response = sourceWallet.sendMoney(destinationWallet, amount);

		// Since this is just a demo and we're not taking persistence serious we save
		// the changes async while returning the data to the user. This of course means
		// the save can fail...
		saveWalletDataToJsonFile(sourceWallet); // doesn't throw
		saveWalletDataToJsonFile(destinationWallet); // doesn't throw

		return response;
	}

	/**
	 * Just put money in your wallet... because why not?!
	 * 
	 * @param walletId The ID of the wallet to send money from
	 * @param amount   The amount of money to send
	 * @return The transfer
	 */
	public BalanceResponseDto depositMoney(UUID walletId, BigDecimal amount, String token)
			throws NotFoundException, AccessDeniedException {
		// Check that the token is valid for that amount
		if (token == null || token.isEmpty() || token.equals("TEST-INVALID-TOKEN")) {
			throw new AccessDeniedException("Invalid token");
		}

		// ****** PERFORM SUPER SECURE CHECK OF TOKEN HERE ******//

		// Make sure the bank wallet exists...
		this.getBankWallet();

		// ...then perform a normal transfer...
		this.sendMoney(bankWalletId, walletId, amount);

		// ...and finally return the balance
		return this.getWalletUnathenticated(walletId).getBalanceDto();
	}

}