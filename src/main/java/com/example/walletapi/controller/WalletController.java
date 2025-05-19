package com.example.walletapi.controller;

import com.example.walletapi.dto.requests.DepositRequestDto;
import com.example.walletapi.dto.requests.TransferRequestDto;

import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.LedgerResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.dto.responses.WalletCreationResponseDto;
import com.example.walletapi.exception.AccessDeniedException;
import com.example.walletapi.security.JwtUtil;
import com.example.walletapi.service.WalletServiceInterface;
import com.example.walletapi.model.WalletInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * REST Controller for wallet operations.
 */
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

	private final WalletServiceInterface walletService;
	private final JwtUtil jwtUtil;

	@Autowired
	public WalletController(WalletServiceInterface walletService, JwtUtil jwtUtil) {
		this.walletService = walletService;
		this.jwtUtil = jwtUtil;
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

				Available Endpoints:

				GET /api/v1/wallet/help
					Returns this documentation.

				POST /api/v1/wallet/create
					Creates a new wallet.
					Returns: { walletId: UUID, token: JWT }

				GET /api/v1/wallet/{walletId}/balance
					Gets the balance of a wallet.
					Requires: JWT token in Authorization header
					Returns: { walletId: UUID, balance: BigDecimal, timestamp: Long }

				GET /api/v1/wallet/{walletId}/ledger
					Gets the transaction history of a wallet.
					Requires: JWT token in Authorization header
					Returns: { transfers: Array<Transfer>, timestamp: Long}

				POST /api/v1/wallet/{walletId}/deposit
					Deposits money into a wallet.
					Requires: JWT token in Authorization header
					Body: { amount: BigDecimal }
					Returns: { walletId: UUID, balance: BigDecimal, timestamp: Long }

				POST /api/v1/wallet/{walletId}/transfer
					Sends money to another wallet.
					Requires: JWT token in Authorization header
					Body: { destinationWalletId: UUID, amount: BigDecimal }
					Returns: { transferId: UUID,recipientWalletId:UUID, amount: BigDecimal, remainingBalance: BigDecimal, timestamp: Long }


				Authentication:
				- All endpoints except /create and /help require a valid JWT token
				- Token must be included in the Authorization header as: "Bearer <token>"
				- Token must match the walletId being accessed
				""";
	}

	/**
	 * Create a new wallet.
	 * 
	 * @return A {@link WalletCreationResponseDto} containing the new wallet's ID,
	 *         and a JWT token to access it
	 */
	@PostMapping("/create")
	public ResponseEntity<WalletCreationResponseDto> createWallet() {
		WalletInterface wallet = walletService.createWallet();
		UUID walletId = wallet.getId();
		String token = jwtUtil.generateToken(walletId.toString());

		WalletCreationResponseDto response = new WalletCreationResponseDto(
				walletId,
				token);

		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	/**
	 * Checks if the requested walletId matches the authenticated walletId
	 * 
	 * @param walletId The requested walletId
	 * @throws AccessDeniedException if the walletIds don't match
	 */
	private void verifyWalletAccess(String walletId) {
		String authenticatedWalletId = jwtUtil.getAuthenticatedUser();
		if (!walletId.equals(authenticatedWalletId)) {
			throw new AccessDeniedException("You are authenticated as " + authenticatedWalletId
					+ ", but you're trying to access " + walletId);
		}
	}

	/**
	 * Get wallet balance.
	 * 
	 * @param walletId The ID of the wallet
	 * @return A {@link BalanceResponseDto} containing the wallet balance
	 */
	@GetMapping("/{walletId}/balance")
	public ResponseEntity<BalanceResponseDto> getBalance(@PathVariable String walletId) {
		verifyWalletAccess(walletId);
		return ResponseEntity.ok(walletService.getWallet(walletId).getBalanceDto());
	}

	/**
	 * Get wallet transactions.
	 * 
	 * @param walletId The ID of the wallet
	 * @return List of transactions for the wallet
	 */
	@GetMapping("/{walletId}/transactions")
	public ResponseEntity<LedgerResponseDto> getTransactions(@PathVariable String walletId) {
		verifyWalletAccess(walletId);
		return ResponseEntity.ok(walletService.getWallet(walletId).getLedgerDto());
	}

	/**
	 * Send money to another wallet
	 * 
	 * @param transferRequestDto The transfer details
	 * 
	 * @return The completed transfer details
	 */
	@PostMapping("/{walletId}/transfer")
	public ResponseEntity<TransferResponseDto> transfer(@PathVariable String walletId,
			@Valid @RequestBody TransferRequestDto transferRequestDto) {
		verifyWalletAccess(walletId);
		return ResponseEntity.ok(walletService.sendMoney(walletId, transferRequestDto.getDestinationWalletId(),
				transferRequestDto.getAmount()));
	}

	/**
	 * Put money in your wallet
	 * 
	 * @param transferRequestDto The transfer details
	 * 
	 * @return The completed transfer details
	 */
	@PostMapping("/{walletId}/deposit")
	public ResponseEntity<BalanceResponseDto> deposit(@PathVariable String walletId,
			@Valid @RequestBody DepositRequestDto transferRequestDto) {
		verifyWalletAccess(walletId);
		return ResponseEntity.ok(walletService.depositMoney(walletId, transferRequestDto.getAmount()));
	}
}