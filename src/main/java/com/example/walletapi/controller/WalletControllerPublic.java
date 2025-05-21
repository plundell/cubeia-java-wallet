package com.example.walletapi.controller;

import com.example.walletapi.dto.requests.CreateWalletRequestDto;
import com.example.walletapi.dto.requests.WalletAccessRequestDto;
import com.example.walletapi.dto.responses.WalletAccessResponseDto;
import com.example.walletapi.security.JwtUtil;
import com.example.walletapi.service.WalletServiceInterface;
import com.example.walletapi.model.WalletInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for wallet operations.
 */
@RestController
@RequestMapping("/api/wallet/v1/public")
public class WalletControllerPublic {

	private final WalletServiceInterface walletService;
	private final JwtUtil jwtUtil;

	@Autowired
	public WalletControllerPublic(WalletServiceInterface walletService, JwtUtil jwtUtil) {
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

				Public Endpoints:

				GET /api/wallet/v1/help
					Returns this documentation.

				POST /api/wallet/v1/create
					Creates a new wallet.
					Requires: { password: String }
					Returns: { walletId: UUID, token: JWT }

				PUT /api/wallet/v1/access
					Get the access token for an existing wallet
					Requires: { walletId: UUID, password: String }
					Returns: { walletId: UUID, token: JWT }


				Usage:
				======
				These endpoints don't require authentication. You don't need to set any
				particular headers. Request bodies should be json formated strings.

				The '/access' and '/create' endpoints return a token which should be set
				on the Authorization header ("Bearer <token>") of any requests to protected
				endpoints '/protected/**'.
				""";
	}

	/**
	 * Create a new wallet.
	 * 
	 * @return A {@link WalletAccessResponseDto} containing the new wallet's ID,
	 *         and a JWT token to access it
	 */
	@PostMapping("/create")
	public ResponseEntity<WalletAccessResponseDto> createWallet(@RequestBody CreateWalletRequestDto request) {
		WalletInterface wallet = walletService.createWallet(request.getPassword());
		return new ResponseEntity<WalletAccessResponseDto>(createWalletAccessResponseDto(wallet), HttpStatus.CREATED);

	}

	/**
	 * Get the token for an existing wallet.
	 * 
	 * NOTE: We're using PUT since we want to avoid GET (they just feel insecure)
	 * and POST is non-idempotent (multiple calls should produce different results)
	 * 
	 * @return A {@link WalletAccessResponseDto} containing the new wallet's ID,
	 *         and a JWT token to access it
	 */
	@PutMapping("/access")
	public ResponseEntity<WalletAccessResponseDto> getToken(@RequestBody WalletAccessRequestDto request) {
		WalletInterface wallet = walletService.getWallet(request.getWalletId(), request.getPassword());
		return new ResponseEntity<WalletAccessResponseDto>(createWalletAccessResponseDto(wallet), HttpStatus.OK);
	}

	/**
	 * Create a {@link WalletAccessResponseDto} from a {@link WalletInterface}.
	 * 
	 * @param wallet The wallet to create the response from
	 * @return A {@link WalletAccessResponseDto} containing the wallet's ID,
	 *         and a JWT token to access it
	 */
	protected WalletAccessResponseDto createWalletAccessResponseDto(WalletInterface wallet) {
		UUID walletId = wallet.getId();
		String token = jwtUtil.generateToken(walletId.toString());
		return new WalletAccessResponseDto(walletId, token);
	}

}