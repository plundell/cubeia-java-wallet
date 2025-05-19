package com.example.walletapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.model.impl.Wallet;
import com.example.walletapi.service.impl.WalletService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

	@InjectMocks
	private WalletService walletService;

	private ConcurrentMap<String, Wallet> wallets;

	@BeforeEach
	public void setUp() {
		wallets = new ConcurrentHashMap<>();
		ReflectionTestUtils.setField(walletService, "wallets", wallets);
	}

	@Test
	public void testCreateWallet() {
		// Act
		Wallet wallet = walletService.createWallet();

		// Assert
		assertNotNull(wallet);
		assertNotNull(wallet.getId());
		assertEquals(BigDecimal.ZERO, wallet.getBalance());
		assertEquals(1, wallets.size());
		assertTrue(wallets.containsKey(wallet.getId().toString()));
	}

	@Test
	public void testGetWallet_Success() {
		// Arrange
		Wallet wallet = new Wallet();
		String walletId = wallet.getId().toString();
		wallets.put(walletId, wallet);

		// Act
		Wallet retrievedWallet = walletService.getWallet(walletId);

		// Assert
		assertNotNull(retrievedWallet);
		assertEquals(wallet.getId(), retrievedWallet.getId());
	}

	@Test
	public void testGetWallet_NotFound() {
		// Arrange
		String nonExistentWalletId = UUID.randomUUID().toString();

		// Act & Assert
		assertThrows(NotFoundException.class, () -> walletService.getWallet(nonExistentWalletId));
	}

	@Test
	public void testSendMoney_Success() throws InsufficientFundsException, NotFoundException {
		// Arrange
		Wallet sourceWallet = new Wallet();
		String sourceWalletId = sourceWallet.getId().toString();
		wallets.put(sourceWalletId, sourceWallet);

		// Add some money to the source wallet
		sourceWallet.depositMoney(new BigDecimal("100.00"));

		Wallet destinationWallet = new Wallet();
		String destinationWalletId = destinationWallet.getId().toString();
		wallets.put(destinationWalletId, destinationWallet);

		BigDecimal transferAmount = new BigDecimal("50.00");

		// Act
		TransferResponseDto result = walletService.sendMoney(sourceWalletId, destinationWalletId, transferAmount);

		// Assert
		assertNotNull(result);
		assertNotNull(result.getTransferId());
		assertEquals(destinationWalletId, result.getRecipientWalletId());
		assertEquals(transferAmount, result.getAmount());
		assertEquals(new BigDecimal("50.00"), result.getRemainingBalance());
		assertEquals(new BigDecimal("50.00"), sourceWallet.getBalance());
		assertEquals(new BigDecimal("50.00"), destinationWallet.getBalance());
	}

	@Test
	public void testSendMoney_InsufficientFunds() {
		// Arrange
		Wallet sourceWallet = new Wallet(); // Initial balance is ZERO
		String sourceWalletId = sourceWallet.getId().toString();
		wallets.put(sourceWalletId, sourceWallet);

		Wallet destinationWallet = new Wallet();
		String destinationWalletId = destinationWallet.getId().toString();
		wallets.put(destinationWalletId, destinationWallet);

		BigDecimal transferAmount = new BigDecimal("50.00");

		// Act & Assert
		assertThrows(InsufficientFundsException.class,
				() -> walletService.sendMoney(sourceWalletId, destinationWalletId, transferAmount));
	}

	@Test
	public void testDepositMoney() {
		// Arrange
		Wallet wallet = new Wallet();
		String walletId = wallet.getId().toString();
		wallets.put(walletId, wallet);
		BigDecimal depositAmount = new BigDecimal("100.00");

		// Act
		BalanceResponseDto result = walletService.depositMoney(walletId, depositAmount);

		// Assert
		assertNotNull(result);
		assertEquals(depositAmount, result.getBalance());
		assertEquals(depositAmount, wallet.getBalance());
	}
}