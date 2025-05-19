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

public class WalletTest {

	private Wallet wallet;
	private Wallet destinationWallet;

	@BeforeEach
	public void setUp() {
		wallet = new Wallet();
		destinationWallet = new Wallet();
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
	public void testDepositMoney() {
		// Arrange
		BigDecimal depositAmount = new BigDecimal("100.00");

		// Act
		BalanceResponseDto result = wallet.depositMoney(depositAmount);

		// Assert
		assertEquals(depositAmount, wallet.getBalance());
		assertEquals(depositAmount, result.getBalance());
		assertEquals(1, wallet.getLedger().size());
	}

	@Test
	public void testSendMoney_Success() throws InsufficientFundsException {
		// Arrange
		BigDecimal initialDeposit = new BigDecimal("100.00");
		BigDecimal transferAmount = new BigDecimal("50.00");

		wallet.depositMoney(initialDeposit);

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