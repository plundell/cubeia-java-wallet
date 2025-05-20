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
import com.example.walletapi.model.TransferInterface;
import com.example.walletapi.model.TransferInterface.TransferFactoryInterface;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

	@InjectMocks
	private WalletService walletService;

	@Mock
	private BCryptPasswordEncoder mockPasswordEncoder; // Mock encoder

	@Mock
	private TransferFactoryInterface mockTransferFactory;

	private ConcurrentMap<UUID, Wallet> walletsMap; // Renamed for clarity
	private final String defaultPassword = "test-password";
	private final String encodedPassword = "encoded-test-password";
	private UUID bankWalletId = UUID.fromString("00000000-0000-0000-0000-000000000000");

	@BeforeEach
	public void setUp() {
		walletsMap = new ConcurrentHashMap<>();
		ReflectionTestUtils.setField(walletService, "wallets", walletsMap);
		ReflectionTestUtils.setField(walletService, "passwordEncoder", mockPasswordEncoder);
		ReflectionTestUtils.setField(walletService, "bankWalletId", bankWalletId);
		// It's important that bankWallet has funds and the transferFactory
		Wallet bankWallet = new Wallet("bank-secret-password");
		ReflectionTestUtils.setField(bankWallet, "transferFactory", mockTransferFactory);
		ReflectionTestUtils.setField(bankWallet, "balance", new AtomicReference<>(new BigDecimal("1000000")));
		walletsMap.put(bankWalletId, bankWallet);

		when(mockPasswordEncoder.encode(defaultPassword)).thenReturn(encodedPassword);
		when(mockPasswordEncoder.matches(anyString(), anyString())).thenAnswer(invocation -> {
			String rawPassword = invocation.getArgument(0);
			String encPassword = invocation.getArgument(1);
			return encPassword.equals(mockPasswordEncoder.encode(rawPassword));
		});
		when(mockPasswordEncoder.matches(defaultPassword, encodedPassword)).thenReturn(true);

	}

	private Wallet createAndSetupWallet(String password) {
		Wallet wallet = new Wallet(password);
		ReflectionTestUtils.setField(wallet, "transferFactory", mockTransferFactory);
		return wallet;
	}

	@Test
	public void testCreateWallet() {
		// Act
		Wallet wallet = walletService.createWallet(defaultPassword);

		// Assert
		assertNotNull(wallet);
		assertNotNull(wallet.getId());
		assertEquals(BigDecimal.ZERO, wallet.getBalance());
		assertEquals(encodedPassword, wallet.getPassword()); // Wallet stores encoded password
		assertEquals(2, walletsMap.size()); // Bank wallet + new wallet
		assertTrue(walletsMap.containsKey(wallet.getId()));
	}

	@Test
	public void testGetWalletUnauthenticated_Success() {
		// Arrange
		Wallet wallet = createAndSetupWallet(encodedPassword);
		walletsMap.put(wallet.getId(), wallet);

		// Act
		Wallet retrievedWallet = walletService.getWalletUnathenticated(wallet.getId());

		// Assert
		assertNotNull(retrievedWallet);
		assertEquals(wallet.getId(), retrievedWallet.getId());
	}

	@Test
	public void testGetWalletUnauthenticated_NotFound() {
		// Arrange
		UUID nonExistentWalletId = UUID.randomUUID();

		// Act & Assert
		assertThrows(NotFoundException.class, () -> walletService.getWalletUnathenticated(nonExistentWalletId));
	}

	@Test
	public void testGetWallet_Authenticated_Success() {
		// Arrange
		Wallet wallet = createAndSetupWallet(encodedPassword);
		walletsMap.put(wallet.getId(), wallet);
		when(mockPasswordEncoder.matches(defaultPassword, encodedPassword)).thenReturn(true);

		// Act
		Wallet retrievedWallet = walletService.getWallet(wallet.getId(), defaultPassword);

		// Assert
		assertNotNull(retrievedWallet);
		assertEquals(wallet.getId(), retrievedWallet.getId());
	}

	@Test
	public void testGetWallet_Authenticated_IncorrectPassword() {
		// Arrange
		Wallet wallet = createAndSetupWallet(encodedPassword);
		walletsMap.put(wallet.getId(), wallet);
		when(mockPasswordEncoder.matches("wrong-password", encodedPassword)).thenReturn(false);

		// Act & Assert
		assertThrows(NotFoundException.class, () -> walletService.getWallet(wallet.getId(), "wrong-password"));
	}

	@Test
	public void testSendMoney_Success() throws InsufficientFundsException, NotFoundException {
		// Arrange
		Wallet sourceWallet = createAndSetupWallet(encodedPassword);
		ReflectionTestUtils.setField(sourceWallet, "balance", new AtomicReference<>(new BigDecimal("100.00")));
		walletsMap.put(sourceWallet.getId(), sourceWallet);

		Wallet destinationWallet = createAndSetupWallet(encodedPassword);
		walletsMap.put(destinationWallet.getId(), destinationWallet);

		BigDecimal transferAmount = new BigDecimal("50.00");

		TransferInterface mockTransfer = mock(TransferInterface.class);
		when(mockTransfer.getAmount()).thenReturn(transferAmount); // For recipient
		when(mockTransfer.getRecipient()).thenReturn(destinationWallet.getId());
		when(mockTransfer.getSender()).thenReturn(sourceWallet.getId());
		when(mockTransfer.getId()).thenReturn(UUID.randomUUID());
		when(mockTransfer.getTimestamp()).thenReturn(System.currentTimeMillis());

		when(mockTransferFactory.fromSendRequest(sourceWallet.getId(), destinationWallet.getId(), transferAmount))
				.thenReturn(mockTransfer);

		// Act
		TransferResponseDto result = walletService.sendMoney(sourceWallet.getId(), destinationWallet.getId(),
				transferAmount);

		// Assert
		assertNotNull(result);
		assertNotNull(result.getTransferId());
		assertEquals(destinationWallet.getId().toString(), result.getRecipientWalletId());
		assertEquals(transferAmount, result.getAmount());
		// Balance check depends on Wallet.sendMoney logic, which is tested elsewhere,
		// but we expect remaining balance in response
		assertEquals(new BigDecimal("50.00"), result.getRemainingBalance());
		assertEquals(new BigDecimal("50.00"), sourceWallet.getBalance());
		assertEquals(new BigDecimal("50.00"), destinationWallet.getBalance());
	}

	@Test
	public void testSendMoney_InsufficientFunds() {
		// Arrange
		Wallet sourceWallet = createAndSetupWallet(encodedPassword); // Initial balance is ZERO
		walletsMap.put(sourceWallet.getId(), sourceWallet);

		Wallet destinationWallet = createAndSetupWallet(encodedPassword);
		walletsMap.put(destinationWallet.getId(), destinationWallet);
		BigDecimal transferAmount = new BigDecimal("50.00");

		// Act & Assert
		assertThrows(InsufficientFundsException.class,
				() -> walletService.sendMoney(sourceWallet.getId(), destinationWallet.getId(), transferAmount));
	}

	@Test
	public void testDepositMoney_Success() {
		// Arrange
		Wallet targetWallet = createAndSetupWallet(encodedPassword);
		walletsMap.put(targetWallet.getId(), targetWallet); // Target wallet starts with 0

		BigDecimal depositAmount = new BigDecimal("100.00");
		String validToken = "VALID-TOKEN";

		// Mocking for the transfer from bank to targetWallet
		TransferInterface bankToTargetTransfer = mock(TransferInterface.class);
		when(bankToTargetTransfer.getAmount()).thenReturn(depositAmount);
		when(bankToTargetTransfer.getRecipient()).thenReturn(targetWallet.getId());
		when(bankToTargetTransfer.getSender()).thenReturn(bankWalletId);
		when(bankToTargetTransfer.getId()).thenReturn(UUID.randomUUID());
		when(bankToTargetTransfer.getTimestamp()).thenReturn(System.currentTimeMillis());

		when(mockTransferFactory.fromSendRequest(bankWalletId, targetWallet.getId(), depositAmount))
				.thenReturn(bankToTargetTransfer);

		// Act
		BalanceResponseDto result = walletService.depositMoney(targetWallet.getId(), depositAmount, validToken);

		// Assert
		assertNotNull(result);
		assertEquals(depositAmount, result.getBalance());
		assertEquals(targetWallet.getId().toString(), result.getWalletId());
		assertEquals(depositAmount, targetWallet.getBalance()); // Check wallet's balance directly
	}

	@Test
	public void testDepositMoney_InvalidToken() {
		// Arrange
		Wallet targetWallet = createAndSetupWallet(encodedPassword);
		walletsMap.put(targetWallet.getId(), targetWallet);
		BigDecimal depositAmount = new BigDecimal("100.00");
		String invalidToken = "INVALID-TOKEN";
		ReflectionTestUtils.setField(walletService, "depositTokenValidationUrl", "http://someurl");

		// Act & Assert
		Exception exception = assertThrows(org.springframework.security.access.AccessDeniedException.class,
				() -> walletService.depositMoney(targetWallet.getId(), depositAmount, invalidToken));
		assertTrue(exception.getMessage().toLowerCase().contains("invalid")
				|| exception.getMessage().toLowerCase().contains("denied"));
	}
}