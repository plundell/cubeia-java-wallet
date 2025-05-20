package com.example.walletapi.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.model.WalletInterface;
import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;

public interface WalletServiceInterface {

	WalletInterface createWallet(String password);

	/**
	 * Retrieves a wallet by its ID after performing password validation.
	 * 
	 * @param walletId The ID of the wallet to retrieve
	 * @param password The password of the wallet
	 * 
	 * @return The wallet
	 * @throws NotFoundException if the wallet doesn't exist or if the password
	 *                           doesn't match (to prevent sniffing)
	 */
	WalletInterface getWallet(UUID walletId, String password) throws NotFoundException;

	/**
	 * Retrieves a wallet by its ID WITHOUT performing password validation.
	 * 
	 * @param walletId The ID of the wallet to retrieve
	 * 
	 * @return The wallet
	 * @throws NotFoundException if the wallet doesn't exist
	 */
	WalletInterface getWalletUnathenticated(UUID walletId) throws NotFoundException;

	TransferResponseDto sendMoney(UUID sourceWalletId, UUID destinationWalletId, BigDecimal amount)
			throws InsufficientFundsException, NotFoundException;

	BalanceResponseDto depositMoney(UUID walletId, BigDecimal amount, String token)
			throws NotFoundException, AccessDeniedException;
}
