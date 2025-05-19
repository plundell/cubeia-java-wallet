package com.example.walletapi.service.impl;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.model.impl.Transfer;
import com.example.walletapi.model.impl.Wallet;
import com.example.walletapi.service.WalletServiceInterface;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Singleton service for wallet operations.
 */
@Service
public class WalletService implements WalletServiceInterface {
	private final ConcurrentMap<String, Wallet> wallets;
	private final Logger logger;

	@Value("${wallet.data.file:wallet-data.ser}")
	private String walletDataFile;

	@Value("${wallet.data.dir:./test_data}")
	private String walletDataDir;

	@Autowired
	public WalletService() {
		this.wallets = new ConcurrentHashMap<>();
		this.logger = Logger.getLogger(WalletService.class.getName());
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
				ConcurrentMap<String, Wallet> savedWallets = (ConcurrentMap<String, Wallet>) ois.readObject();
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
						wallets.put(wallet.getId().toString(), wallet);
					} catch (Exception e) {
						logger.warning(
								"Failed to load wallet data from file " + jsonFile.getName() + ": " + e.getMessage());
					}
				}
			}
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
	 * @return The wallet
	 * @throws NotFoundException if the wallet doesn't exist
	 */
	public Wallet getWallet(String walletId) throws NotFoundException {
		Wallet wallet = this.wallets.get(walletId);
		if (wallet == null) {
			throw new NotFoundException("No wallet with id " + walletId + " found. Please check the id and try again.");
		}
		return wallet;
	}

	/**
	 * Creates a new wallet with an initial balance of zero.
	 * 
	 * @return The newly created wallet
	 */
	public Wallet createWallet() {
		Wallet wallet = new Wallet();
		this.wallets.put(wallet.getId().toString(), wallet);
		return wallet;
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
	public TransferResponseDto sendMoney(String sourceWalletId, String destinationWalletId, BigDecimal amount)
			throws InsufficientFundsException, NotFoundException {
		Wallet sourceWallet = this.getWallet(sourceWalletId);
		Wallet destinationWallet = this.getWallet(destinationWalletId);
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
	public BalanceResponseDto depositMoney(String walletId, BigDecimal amount) {
		// Normally maybe we would want the money to come from somewhere... but hey
		return this.getWallet(walletId).depositMoney(amount);
	}
}