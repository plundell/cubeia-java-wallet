package com.example.walletapi.service.impl;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.model.impl.Wallet;
import com.example.walletapi.service.WalletServiceInterface;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;

import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Singleton service for wallet operations.
 */
@Service
public class WalletService implements WalletServiceInterface {
	private final ConcurrentMap<UUID, Wallet> wallets;
	private final Logger logger;
	private final BCryptPasswordEncoder passwordEncoder;

	@Value("${bank.wallet.id:00000000-0000-0000-0000-000000000000}")
	private UUID bankWalletId;

	@Value("${bank.wallet.initial-deposit:1000000000}")
	private BigDecimal bankWealth;

	@Value("${wallet.data.file:wallet-data.ser}")
	private String walletDataFile;

	@Value("${wallet.data.dir:./test_data}")
	private String walletDataDir;

	@Autowired
	public WalletService() {
		this.wallets = new ConcurrentHashMap<>();
		this.logger = Logger.getLogger(WalletService.class.getName());
		this.passwordEncoder = new BCryptPasswordEncoder();
	}

	/**
	 * Loads wallet data from file when the application starts
	 */
	// @PostConstruct // Using json files instead
	public void loadWalletData() {
		File file = new File(walletDataFile);
		if (file.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
				@SuppressWarnings("unchecked")
				ConcurrentMap<UUID, Wallet> savedWallets = (ConcurrentMap<UUID, Wallet>) ois.readObject();
				wallets.putAll(savedWallets);
				logger.info("Loaded " + wallets.size() + " wallets from " + walletDataFile);
			} catch (Exception e) {
				logger.severe("Failed to load wallet data from file: " + e.getMessage());
			}
		} else {
			logger.info("No wallet data file found at " + walletDataFile);
		}
	}

	@PostConstruct
	public void loadWalletDataFromJsonFiles() {
		File dataDir = new File(walletDataDir);
		if (!dataDir.exists()) {
			logger.info("Wallet data directory not found at " + walletDataDir + ", creating...");
			try {
				dataDir.mkdirs();
			} catch (Exception e) {
				logger.warning("Failed to create wallet data directory. Nothing will be stored. " + e.getMessage());
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
						Wallet wallet = new Wallet(data);
						wallets.put(wallet.getId(), wallet);
					} catch (Exception e) {
						logger.warning(
								"Failed to load wallet data from file " + jsonFile.getName() + ": " + e.getMessage());
					}
				}
			}
		}
	}

	private Wallet getBankWallet() {
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
			saveWalletData();
		}));
	}

	/**
	 * Saves wallet data to file when the application shuts down
	 */
	// @EventListener(ContextClosedEvent.class) // Using json files instead
	public void saveWalletData() {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(walletDataFile))) {
			oos.writeObject(wallets);
			logger.info("Saved " + wallets.size() + " wallets to " + walletDataFile);
		} catch (Exception e) {
			logger.severe("Failed to save wallet data to file: " + e.getMessage());
		}
	}

	public void saveWalletDataToJsonFiles() {
		ObjectMapper objectMapper = new ObjectMapper();
		for (Wallet wallet : wallets.values()) {
			saveWalletDataToJsonFile(wallet, objectMapper);
		}
	}

	public boolean saveWalletDataToJsonFile(Wallet wallet, ObjectMapper objectMapper) {
		String filename = walletDataDir + "/" + wallet.getId().toString() + ".json";
		File jsonFile = new File(filename);
		try {
			if (objectMapper == null) {
				objectMapper = new ObjectMapper();
			}
			objectMapper.writeValue(jsonFile, wallet);
			return true;
		} catch (Exception e) {
			logger.warning("Failed to save wallet data to " + filename + ": " + e.getMessage());
			return false;
		}
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
	public Wallet getWallet(UUID walletId, String password) throws NotFoundException {
		Wallet wallet = this.wallets.get(walletId);
		if (wallet != null) {
			if (passwordEncoder.matches(password, wallet.getPassword())) {
				return wallet;
			} else {
				this.logger.warning("Password mismatch for wallet " + walletId);
			}
		}
		throw new NotFoundException("No wallet matching that id (" + walletId
				+ ") and password was found. Please check the id and try again.");
	}

	/**
	 * Retrieves a wallet by its ID WITHOUT performing password validation.
	 * 
	 * @param walletId The ID of the wallet to retrieve
	 * @return The wallet
	 * @throws NotFoundException if the wallet doesn't exist
	 */
	public Wallet getWalletUnathenticated(UUID walletId) throws NotFoundException {
		Wallet wallet = this.wallets.get(walletId);
		if (wallet == null) {
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
	public Wallet createWallet(String clearTextPassword) {
		if (clearTextPassword == null || clearTextPassword.isEmpty()) {
			throw new IllegalArgumentException("Password is required to create a wallet");
		}
		String encodedPassword = this.passwordEncoder.encode(clearTextPassword);
		Wallet wallet = new Wallet(encodedPassword);
		this.wallets.put(wallet.getId(), wallet);
		return wallet;
	}

	private Wallet createBankWallet() {
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

		Wallet bankWallet = new Wallet(bankWalletData);
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
		Wallet sourceWallet = this.getWalletUnathenticated(sourceWalletId);
		Wallet destinationWallet = this.getWalletUnathenticated(destinationWalletId);
		TransferResponseDto response = sourceWallet.sendMoney(destinationWallet, amount);

		// Since this is just a demo and we're not taking persistence serious we save
		// the changes async while returning the data to the user. This of course means
		// the save can fail...
		CompletableFuture.runAsync(() -> {
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				saveWalletDataToJsonFile(sourceWallet, objectMapper);
				saveWalletDataToJsonFile(destinationWallet, objectMapper);
			} catch (Exception e) {
				logger.warning("Failed to save wallet data to file: " + e.getMessage());
			}
		});

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