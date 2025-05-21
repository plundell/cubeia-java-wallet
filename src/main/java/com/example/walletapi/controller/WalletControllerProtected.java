package com.example.walletapi.controller;

import com.example.walletapi.dto.requests.DepositRequestDto;
import com.example.walletapi.dto.requests.TransferRequestDto;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.LedgerResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.exception.ServerErrorException;
import com.example.walletapi.security.JwtUtil;
import com.example.walletapi.service.WalletServiceInterface;
import com.example.walletapi.model.WalletInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for authenticated operations on a wallet.
 */
@RestController
@RequestMapping("/api/wallet/v1/protected")
public class WalletControllerProtected {

	private final WalletServiceInterface walletService;
	private final JwtUtil jwtUtil;

	private final Logger logger;

	@Autowired
	public WalletControllerProtected(WalletServiceInterface walletService, JwtUtil jwtUtil) {
		this.walletService = walletService;
		this.jwtUtil = jwtUtil;
		this.logger = LoggerFactory.getLogger(this.getClass());
	}

	/**
	 * Get documentation for the API
	 * 
	 * @return A {@link String} containing the documentation
	 */
	@GetMapping("/help")
	public String getDocumentation() {
		return """
				Wallet API Documentation
				======================

				Protected Endpoints:

				GET /api/wallet/v1/help
					Returns this documentation.

				GET /api/wallet/v1/balance
					Gets the balance of a wallet.
					Requires: JWT token in Authorization header
					Returns: { walletId: UUID, balance: BigDecimal, timestamp: Long }

				GET /api/wallet/v1/ledger
					Gets the transaction history of a wallet.
					Requires: JWT token in Authorization header
					Returns: { transfers: Array<Transfer>, timestamp: Long}

				POST /api/wallet/v1/deposit
					Deposits money into a wallet.
					Requires: JWT token in Authorization header
					Body: { amount: BigDecimal, token: String }
					Returns: { walletId: UUID, balance: BigDecimal, timestamp: Long }

				POST /api/wallet/v1/transfer
					Sends money to another wallet.
					Requires: JWT token in Authorization header
					Body: { destinationWalletId: UUID, amount: BigDecimal }
					Returns: { transferId: UUID,recipientWalletId:UUID, amount: BigDecimal, remainingBalance: BigDecimal, timestamp: Long }

				""";
	}

	/**
	 * Checks if the requested walletId matches the authenticated walletId
	 * 
	 * @param walletId The requested walletId
	 * @throws ServerErrorException BUG 66645 if the security filter chain failed to
	 *                              set a wallet ID on the security context
	 * 
	 */
	private WalletInterface getWallet() {
		UUID walletId = jwtUtil.getAuthenticatedUser(UUID.class);
		if (walletId == null) {
			logger.error("BUGBUG: The security filter chain failed to set a wallet ID on the security context.");
			throw new ServerErrorException(66645);
		}
		try {
			return walletService.getWalletUnathenticated(walletId);
		} catch (NotFoundException e) {
			throw new NotFoundException("It seems you have a valid token for a Wallet which no longer exists. "
					+ "Please contact customer service to find out why.");
		}
	}

	/**
	 * Get wallet balance.
	 * 
	 * @param walletId The ID of the wallet
	 * @return A {@link BalanceResponseDto} containing the wallet balance
	 */
	@GetMapping("/balance")
	public ResponseEntity<BalanceResponseDto> getBalance() {
		return ResponseEntity.ok(getWallet().getBalanceDto());
	}

	/**
	 * Get wallet transactions.
	 * 
	 * @param walletId The ID of the wallet
	 * @return List of transactions for the wallet
	 */
	@GetMapping("/transactions")
	public ResponseEntity<LedgerResponseDto> getTransactions() {
		return ResponseEntity.ok(getWallet().getLedgerDto());
	}

	/**
	 * Send money to another wallet
	 * 
	 * @param transferRequestDto The transfer details
	 * 
	 * @return The completed transfer details
	 */
	@PostMapping("/transfer")
	public ResponseEntity<TransferResponseDto> transfer(@Valid @RequestBody TransferRequestDto transferRequestDto) {
		WalletInterface wallet = getWallet();
		return ResponseEntity.ok(walletService.sendMoney(
				wallet.getId(), transferRequestDto.getDestinationWalletId(), transferRequestDto.getAmount()));
	}

	/**
	 * Put money in your wallet
	 * 
	 * @param transferRequestDto The transfer details
	 * 
	 * @return The completed transfer details
	 */
	@PostMapping("/deposit")
	public ResponseEntity<BalanceResponseDto> deposit(@Valid @RequestBody DepositRequestDto transferRequestDto) {
		WalletInterface wallet = getWallet();
		return ResponseEntity.ok(walletService.depositMoney(wallet.getId(), transferRequestDto.getAmount(),
				transferRequestDto.getToken()));
	}
}