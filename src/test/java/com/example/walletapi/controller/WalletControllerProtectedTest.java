package com.example.walletapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.walletapi.dto.requests.DepositRequestDto;
import com.example.walletapi.dto.requests.TransferRequestDto;
import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.LedgerResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.model.TransferInterface;
import com.example.walletapi.model.WalletInterface;
import com.example.walletapi.model.impl.Wallet;
import com.example.walletapi.security.JwtUtil;
import com.example.walletapi.service.WalletServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;

@WebMvcTest(controllers = WalletControllerProtected.class)
public class WalletControllerProtectedTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private WalletServiceInterface walletService;

	@MockBean
	private JwtUtil jwtUtil;

	private WalletInterface mockWallet;
	private UUID testWalletId;
	private String jwtToken;

	@BeforeEach
	public void setUp() {
		testWalletId = UUID.randomUUID();
		jwtToken = "test-jwt-token";

		mockWallet = Mockito.mock(WalletInterface.class);
		when(mockWallet.getId()).thenReturn(testWalletId);

		// Mock JWT authentication for protected endpoints
		when(jwtUtil.getAuthenticatedUser(UUID.class)).thenReturn(testWalletId);
		// This line is crucial for WalletControllerProtected.getWallet()
		when(walletService.getWalletUnathenticated(testWalletId)).thenReturn(mockWallet);
	}

	@Test
	public void testGetBalance() throws Exception {
		// Arrange
		BalanceResponseDto balanceDto = new BalanceResponseDto(
				testWalletId,
				new BigDecimal("100.00"),
				System.currentTimeMillis());

		when(mockWallet.getBalanceDto()).thenReturn(balanceDto);

		// Act & Assert
		mockMvc.perform(get("/api/wallet/v1/protected/balance")
				.header("Authorization", "Bearer " + jwtToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
				.andExpect(jsonPath("$.balance").value(100.00));
	}

	@Test
	public void testGetTransactions() throws Exception {
		// Arrange
		LedgerResponseDto ledgerDto = new LedgerResponseDto(java.util.Collections.emptyList(),
				System.currentTimeMillis());
		when(mockWallet.getLedgerDto()).thenReturn(ledgerDto);

		// Act & Assert
		mockMvc.perform(get("/api/wallet/v1/protected/transactions") // Corrected path
				.header("Authorization", "Bearer " + jwtToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transfers").isArray()); // Property name in LedgerResponseDto is transfers
	}

	@Test
	public void testTransfer() throws Exception {
		// Arrange
		TransferRequestDto transferRequest = new TransferRequestDto();
		UUID destinationWalletId = UUID.randomUUID();
		transferRequest.setDestinationWalletId(destinationWalletId.toString());
		transferRequest.setAmount(new BigDecimal("50.00"));

		TransferInterface mockTransfer = Mockito.mock(TransferInterface.class);
		when(mockTransfer.getId()).thenReturn(UUID.randomUUID());
		when(mockTransfer.getRecipient()).thenReturn(destinationWalletId);
		when(mockTransfer.getAmount()).thenReturn(new BigDecimal("50.00"));
		when(mockTransfer.getTimestamp()).thenReturn(System.currentTimeMillis());

		TransferResponseDto transferResponse = new TransferResponseDto(mockTransfer, new BigDecimal("50.00"));

		when(walletService.sendMoney(
				testWalletId, // Sender is the authenticated wallet
				destinationWalletId,
				new BigDecimal("50.00"))).thenReturn(transferResponse);

		// Act & Assert
		mockMvc.perform(post("/api/wallet/v1/protected/transfer") // Corrected path
				.header("Authorization", "Bearer " + jwtToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(transferRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recipientWalletId").value(destinationWalletId.toString()))
				.andExpect(jsonPath("$.amount").value(50.00));
	}

	@Test
	public void testDeposit() throws Exception {
		// Arrange
		DepositRequestDto depositRequest = new DepositRequestDto();
		depositRequest.setAmount(new BigDecimal("100.00"));
		depositRequest.setToken("VALID-DEPOSIT-TOKEN");

		BalanceResponseDto balanceResponse = new BalanceResponseDto(
				testWalletId,
				new BigDecimal("100.00"),
				System.currentTimeMillis());

		when(walletService.depositMoney(testWalletId, new BigDecimal("100.00"), "VALID-DEPOSIT-TOKEN"))
				.thenReturn(balanceResponse);

		// Act & Assert
		mockMvc.perform(post("/api/wallet/v1/protected/deposit") // Corrected path
				.header("Authorization", "Bearer " + jwtToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(depositRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
				.andExpect(jsonPath("$.balance").value(100.00));
	}

	@Test
	public void testTransfer_InsufficientFunds() throws Exception {
		// Arrange
		TransferRequestDto transferRequest = new TransferRequestDto();
		UUID destinationWalletId = UUID.randomUUID();
		transferRequest.setDestinationWalletId(destinationWalletId.toString());
		transferRequest.setAmount(new BigDecimal("1000.00"));

		when(walletService.sendMoney(
				testWalletId,
				destinationWalletId,
				new BigDecimal("1000.00")))
				.thenThrow(new InsufficientFundsException("Insufficient funds"));

		// Act & Assert
		mockMvc.perform(post("/api/wallet/v1/protected/transfer") // Corrected path
				.header("Authorization", "Bearer " + jwtToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(transferRequest)))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void testGetBalance_WalletNotFound_WhenServiceThrows() throws Exception {
		// This test simulates if getAuthenticatedUser returns an ID, but that ID is not
		// in the service.
		UUID nonExistentWalletId = UUID.randomUUID();
		when(jwtUtil.getAuthenticatedUser(UUID.class)).thenReturn(nonExistentWalletId);
		when(walletService.getWalletUnathenticated(nonExistentWalletId))
				.thenThrow(new NotFoundException("Wallet not found"));

		// Act & Assert
		mockMvc.perform(get("/api/wallet/v1/protected/balance")
				.header("Authorization", "Bearer " + jwtToken))
				.andExpect(status().isNotFound());
	}

	// TODO: Add test for GET /api/wallet/v1/protected/help
}