package com.example.walletapi.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.LedgerResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.model.impl.Wallet;
import com.example.walletapi.model.TransferInterface;
import com.example.walletapi.model.TransferInterface.TransferFactoryInterface;
import com.example.walletapi.model.WalletInterface;
import com.example.walletapi.model.impl.Wallet.WalletFactory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

@ExtendWith(MockitoExtension.class)
public class WalletTest {

	private Wallet wallet;
	private Wallet destinationWallet;
	private final String depositToken = "VALID-TOKEN";

	@Mock
	private TransferFactoryInterface mockTransferFactory;

	@Mock
	private TransferInterface mockTransfer;

	private WalletFactory walletFactory;

	public static Map<String, Serializable> generateTestWalletMap(String walletId, String password,
			String ledgerTransferId, UUID ledgerSenderId, UUID ledgerRecipientId,
			BigDecimal ledgerAmount, long ledgerTimestamp) {

		Map<String, Serializable> ledgerEntryMap = new HashMap<>();
		ledgerEntryMap.put("id", ledgerTransferId);
		ledgerEntryMap.put("sender", ledgerSenderId.toString());
		ledgerEntryMap.put("recipient", ledgerRecipientId.toString());
		ledgerEntryMap.put("amount", ledgerAmount);
		ledgerEntryMap.put("timestamp", ledgerTimestamp);

		List<Map<String, Serializable>> ledgerList = new ArrayList<>();
		ledgerList.add(ledgerEntryMap);

		Map<String, Serializable> walletData = new HashMap<>();
		walletData.put("id", walletId);
		walletData.put("password", password);
		walletData.put("ledger", (Serializable) ledgerList);
		return walletData;
	}

	@Test
	public void testCreateWalletFromMap() {
		// Arrange
		String testWalletIdStr = "123e4567-e89b-12d3-a456-426614174000";
		UUID testWalletId = UUID.fromString(testWalletIdStr);
		String testPassword = "test-password";

		String testLedgerTransferIdStr = "123e4567-e89b-12d3-a456-426734832422";
		UUID testLedgerTransferId = UUID.fromString(testLedgerTransferIdStr);

		UUID testLedgerSenderId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
		UUID testLedgerRecipientId = testWalletId;

		BigDecimal testLedgerAmount = new BigDecimal("400.00");
		long testLedgerTimestamp = 1716153600000L;

		BigDecimal expectedBalance = testLedgerAmount;

		Map<String, Serializable> walletMap = generateTestWalletMap(
				testWalletIdStr,
				testPassword,
				testLedgerTransferIdStr,
				testLedgerSenderId,
				testLedgerRecipientId,
				testLedgerAmount,
				testLedgerTimestamp);

		// Mock the behavior of the TransferFactory and the Transfer object it creates
		// This setup is crucial for the Wallet constructor to correctly process the
		// ledger.
		when(mockTransferFactory.fromMap(anyMap())).thenReturn(mockTransfer);
		when(mockTransfer.getId()).thenReturn(testLedgerTransferId);
		when(mockTransfer.getSender()).thenReturn(testLedgerSenderId);
		when(mockTransfer.getRecipient()).thenReturn(testLedgerRecipientId);
		when(mockTransfer.getAmount()).thenReturn(testLedgerAmount);
		when(mockTransfer.getTimestamp()).thenReturn(testLedgerTimestamp);

		// Act
		// Use the WalletFactory to create the wallet from the map
		walletFactory = new WalletFactory(mockTransferFactory);
		WalletInterface walletFromMap = walletFactory.fromMap(walletMap);

		// Assert
		assertNotNull(walletFromMap);
		assertEquals(testWalletId, walletFromMap.getId());
		assertEquals(testPassword, walletFromMap.getPassword());
		assertEquals(expectedBalance, walletFromMap.getBalance());

		assertNotNull(walletFromMap.getLedger());
		assertEquals(1, walletFromMap.getLedger().size());

		TransferInterface actualLedgerEntry = walletFromMap.getLedger().get(0);
		assertNotNull(actualLedgerEntry);

		assertEquals(testLedgerTransferId, actualLedgerEntry.getId());
		assertEquals(testLedgerSenderId, actualLedgerEntry.getSender());
		assertEquals(testLedgerRecipientId, actualLedgerEntry.getRecipient());
		assertEquals(testLedgerAmount, actualLedgerEntry.getAmount());
		assertEquals(testLedgerTimestamp, actualLedgerEntry.getTimestamp());
	}

	@BeforeEach
	public void setUp() {
		// Ensure wallet and destinationWallet in setUp also use the constructor that
		// accepts a factory
		walletFactory = new WalletFactory(mockTransferFactory);
		wallet = (Wallet) walletFactory.generateNew("test-password");
		destinationWallet = (Wallet) walletFactory.generateNew("test-password-dest");
		// ReflectionTestUtils are no longer needed for setting transferFactory if
		// constructors handle it.
		// ReflectionTestUtils.setField(wallet, "transferFactory", mockTransferFactory);
		// ReflectionTestUtils.setField(destinationWallet, "transferFactory",
		// mockTransferFactory);
	}

	@Test
	public void testNewWalletHasZeroBalance() {
		// Assert
		assertEquals(BigDecimal.ZERO, wallet.getBalance());
	}

	@Test
	public void testGetId() {
		// Assert
		assertNotNull(wallet.getId());
		assertTrue(wallet.getId() instanceof UUID);
	}

	@Test
	public void testGetBalanceDto() {
		// Act
		BalanceResponseDto balanceDto = wallet.getBalanceDto();

		// Assert
		assertNotNull(balanceDto);
		assertEquals(wallet.getId().toString(), balanceDto.getWalletId());
		assertEquals(BigDecimal.ZERO, balanceDto.getBalance());
		assertTrue(balanceDto.getTimestamp() > 0);
	}

	@Test
	public void testGetLedger_EmptyByDefault() {
		// Act
		var ledger = wallet.getLedger();

		// Assert
		assertNotNull(ledger);
		assertTrue(ledger.isEmpty());
	}

	@Test
	public void testGetLedgerDto() {
		// Act
		LedgerResponseDto ledgerDto = wallet.getLedgerDto();

		// Assert
		assertNotNull(ledgerDto);
		assertNotNull(ledgerDto.getTransfers());
		assertTrue(ledgerDto.getTransfers().isEmpty());
		assertTrue(ledgerDto.getTimestamp() > 0);
	}

	@Test
	public void testSendMoney_Success() throws InsufficientFundsException {
		// Arrange
		BigDecimal initialDeposit = new BigDecimal("100.00");
		BigDecimal transferAmount = new BigDecimal("50.00");

		// Mock the transfer for deposit
		when(mockTransfer.getAmount()).thenReturn(initialDeposit);
		when(mockTransfer.getRecipient()).thenReturn(wallet.getId());
		// Simulate deposit
		wallet.receiveMoney(mockTransfer);

		// Mock the transfer for sending money
		TransferInterface sentTransfer = mock(TransferInterface.class);
		when(sentTransfer.getAmount()).thenReturn(transferAmount.negate()); // Amount is negated for sender
		when(sentTransfer.getRecipient()).thenReturn(destinationWallet.getId());
		when(sentTransfer.getSender()).thenReturn(wallet.getId());

		when(mockTransferFactory.fromSendRequest(wallet.getId(), destinationWallet.getId(), transferAmount))
				.thenReturn(sentTransfer);

		// Act
		TransferResponseDto result = wallet.sendMoney(destinationWallet, transferAmount);

		// Assert
		assertNotNull(result);
		assertEquals(transferAmount, result.getAmount());
		assertEquals(destinationWallet.getId().toString(), result.getRecipientWalletId());
		assertEquals(new BigDecimal("50.00"), result.getRemainingBalance());

		// Check source wallet
		assertEquals(new BigDecimal("50.00"), wallet.getBalance());
		assertEquals(2, wallet.getLedger().size()); // deposit + transfer out

		// Check destination wallet
		assertEquals(transferAmount, destinationWallet.getBalance());
		assertEquals(1, destinationWallet.getLedger().size()); // transfer in
	}

	@Test
	public void testSendMoney_InsufficientFunds() {
		// Arrange
		BigDecimal transferAmount = new BigDecimal("100.00");
		// Wallet starts with zero balance

		// Act & Assert
		Exception exception = assertThrows(InsufficientFundsException.class, () -> {
			wallet.sendMoney(destinationWallet, transferAmount);
		});

		assertTrue(exception.getMessage().contains("Insufficient funds"));
		assertEquals(BigDecimal.ZERO, wallet.getBalance());
		assertEquals(BigDecimal.ZERO, destinationWallet.getBalance());
	}

	@Test
	public void testSendMoney_ZeroAmount() {
		// Arrange
		BigDecimal transferAmount = BigDecimal.ZERO;

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			wallet.sendMoney(destinationWallet, transferAmount);
		});

		assertTrue(exception.getMessage().contains("Amount must be positive"));
	}

	@Test
	public void testSendMoney_NegativeAmount() {
		// Arrange
		BigDecimal transferAmount = new BigDecimal("-50.00");

		// Act & Assert
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			wallet.sendMoney(destinationWallet, transferAmount);
		});

		assertTrue(exception.getMessage().contains("Amount must be positive"));
	}
}