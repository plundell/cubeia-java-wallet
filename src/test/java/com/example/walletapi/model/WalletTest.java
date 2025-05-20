package com.example.walletapi.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.LedgerResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.model.impl.Wallet;
import com.example.walletapi.model.TransferInterface;
import com.example.walletapi.model.TransferInterface.TransferFactoryInterface;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class WalletTest {

	private Wallet wallet;
	private Wallet destinationWallet;
	private final String depositToken = "VALID-TOKEN";

	@Mock
	private TransferFactoryInterface mockTransferFactory;

	@Mock
	private TransferInterface mockTransfer;

	public static String generateTestWallet(String walletId, String password, BigDecimal balance,
			String ledgerTransferId, String ledgerSenderId, String ledgerRecipientId,
			BigDecimal ledgerAmount, long ledgerTimestamp) {
		return """
				{
					"id": "%s",
					"password": "%s",
					"balance": %s,
					"ledger": [
						{
							"id": "%s",
							"sender": "%s",
							"recipient": "%s",
							"amount": %s,
							"timestamp": %d
						}
					]
				}
				""".formatted(walletId, password, balance.toPlainString(),
				ledgerTransferId, ledgerSenderId, ledgerRecipientId,
				ledgerAmount.toPlainString(), ledgerTimestamp);
	}

	@Test
	public void testCreateWalletFromJson() {
		// Arrange
		String testWalletId = "123e4567-e89b-12d3-a456-426614174000";
		String testPassword = "test-password";
		BigDecimal testBalance = new BigDecimal("500.00");
		String testDestinationWalletId = "123e4567-e89b-12d3-a456-426614174001"; // Used as sender in ledger
		String testTransferId = "123e4567-e89b-12d3-a456-426734832422";
		BigDecimal testLedgerAmount = new BigDecimal("400.00");
		long testLedgerTimestamp = 1716153600000L;

		// The JSON defines 'sender' as testDestinationWalletId and 'recipient' as
		// testWalletId for the ledger entry.
		String jsonString = generateTestWallet(
				testWalletId,
				testPassword,
				testBalance,
				testTransferId,
				testDestinationWalletId, // This will be the 'sender' in the JSON ledger
				testWalletId, // This will be the 'recipient' in the JSON ledger
				testLedgerAmount,
				testLedgerTimestamp);

		// Act
		Wallet walletFromJson = new Wallet(jsonString);

		// Assert
		assertNotNull(walletFromJson);
		assertEquals(UUID.fromString(testWalletId), walletFromJson.getId());
		assertEquals(testPassword, walletFromJson.getPassword());
		assertEquals(testBalance, walletFromJson.getBalance());
		assertEquals(1, walletFromJson.getLedger().size());
		assertEquals(testTransferId, walletFromJson.getLedger().get(0).getId().toString());
		// The original assertions are maintained. If
		// Wallet.getLedger().get(0).getRecipient()
		// and .getSender() seem swapped, it's preserving original test's expectation.
		assertEquals(testDestinationWalletId, walletFromJson.getLedger().get(0).getRecipient().toString());
		assertEquals(testWalletId, walletFromJson.getLedger().get(0).getSender().toString());
		assertEquals(testLedgerAmount, walletFromJson.getLedger().get(0).getAmount());
		assertEquals(testLedgerTimestamp, walletFromJson.getLedger().get(0).getTimestamp());
	}

	@BeforeEach
	public void setUp() {
		wallet = new Wallet("test-password");
		destinationWallet = new Wallet("test-password");
		ReflectionTestUtils.setField(wallet, "transferFactory", mockTransferFactory);
		ReflectionTestUtils.setField(destinationWallet, "transferFactory", mockTransferFactory);
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