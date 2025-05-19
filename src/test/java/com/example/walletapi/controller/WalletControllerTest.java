package com.example.walletapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import com.example.walletapi.dto.requests.DepositRequestDto;
import com.example.walletapi.dto.requests.TransferRequestDto;
import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.LedgerResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;
import com.example.walletapi.dto.responses.WalletCreationResponseDto;
import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.model.TransferInterface;
import com.example.walletapi.model.impl.Wallet;
import com.example.walletapi.security.JwtUtil;
import com.example.walletapi.service.WalletServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;

@WebMvcTest(controllers = WalletController.class)
public class WalletControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private WalletServiceInterface walletService;

	@MockBean
	private JwtUtil jwtUtil;

	private Wallet testWallet;
	private String testWalletId;
	private String testToken;

	@BeforeEach
	public void setUp() {
		testWallet = new Wallet();
		testWalletId = testWallet.getId().toString();
		testToken = "test-jwt-token";

		// Mock JWT authentication
		when(jwtUtil.getAuthenticatedUser()).thenReturn(testWalletId);
	}

	@Test
	public void testCreateWallet() throws Exception {
		// Arrange
		when(walletService.createWallet()).thenReturn(testWallet);
		when(jwtUtil.generateToken(anyString())).thenReturn(testToken);

		// Act & Assert
		mockMvc.perform(post("/api/v1/wallet/create")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.walletId").value(testWalletId))
				.andExpect(jsonPath("$.token").value(testToken));
	}

	@Test
	public void testGetBalance() throws Exception {
		// Arrange
		BalanceResponseDto balanceDto = new BalanceResponseDto(
				testWallet.getId(),
				new BigDecimal("100.00"),
				System.currentTimeMillis());

		when(walletService.getWallet(testWalletId)).thenReturn(testWallet);
		when(testWallet.getBalanceDto()).thenReturn(balanceDto);

		// Act & Assert
		mockMvc.perform(get("/api/v1/wallet/{walletId}/balance", testWalletId)
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.header("Authorization", "Bearer " + testToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value("100.00"));
	}

	@Test
	public void testGetTransactions() throws Exception {
		// Arrange
		LedgerResponseDto ledgerDto = new LedgerResponseDto(java.util.Collections.emptyList(),
				System.currentTimeMillis());
		when(walletService.getWallet(testWalletId)).thenReturn(testWallet);
		when(testWallet.getLedgerDto()).thenReturn(ledgerDto);

		// Act & Assert
		mockMvc.perform(get("/api/v1/wallet/{walletId}/transactions", testWalletId)
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.header("Authorization", "Bearer " + testToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transactions").isArray());
	}

	@Test
	public void testTransfer() throws Exception {
		// Arrange
		TransferRequestDto transferRequest = new TransferRequestDto();
		UUID destinationWalletId = UUID.randomUUID();
		transferRequest.setDestinationWalletId(destinationWalletId.toString());
		transferRequest.setAmount(new BigDecimal("50.00"));

		// Create a mock TransferInterface
		TransferInterface mockTransfer = Mockito.mock(TransferInterface.class);
		when(mockTransfer.getId()).thenReturn(UUID.randomUUID());
		when(mockTransfer.getRecipient()).thenReturn(destinationWalletId);
		when(mockTransfer.getAmount()).thenReturn(new BigDecimal("50.00"));
		when(mockTransfer.getTimestamp()).thenReturn(System.currentTimeMillis());

		TransferResponseDto transferResponse = new TransferResponseDto(mockTransfer, new BigDecimal("50.00"));

		when(walletService.sendMoney(
				testWalletId,
				destinationWalletId.toString(),
				new BigDecimal("50.00"))).thenReturn(transferResponse);

		// Act & Assert
		mockMvc.perform(post("/api/v1/wallet/{walletId}/transfer", testWalletId)
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.header("Authorization", "Bearer " + testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(transferRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recipientWalletId").value(destinationWalletId.toString()))
				.andExpect(jsonPath("$.amount").value("50.00"));
	}

	@Test
	public void testDeposit() throws Exception {
		// Arrange
		DepositRequestDto depositRequest = new DepositRequestDto();
		depositRequest.setAmount(new BigDecimal("100.00"));

		BalanceResponseDto balanceResponse = new BalanceResponseDto(
				testWallet.getId(),
				new BigDecimal("100.00"),
				System.currentTimeMillis());

		when(walletService.depositMoney(testWalletId, new BigDecimal("100.00"))).thenReturn(balanceResponse);

		// Act & Assert
		mockMvc.perform(post("/api/v1/wallet/{walletId}/deposit", testWalletId)
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.header("Authorization", "Bearer " + testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(depositRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value("100.00"));
	}

	@Test
	public void testTransfer_InsufficientFunds() throws Exception {
		// Arrange
		TransferRequestDto transferRequest = new TransferRequestDto();
		UUID destinationWalletId = UUID.randomUUID();
		transferRequest.setDestinationWalletId(destinationWalletId.toString());
		transferRequest.setAmount(new BigDecimal("1000.00"));

		when(walletService.sendMoney(anyString(), anyString(), any(BigDecimal.class)))
				.thenThrow(new InsufficientFundsException("Insufficient funds"));

		// Act & Assert
		mockMvc.perform(post("/api/v1/wallet/{walletId}/transfer", testWalletId)
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.header("Authorization", "Bearer " + testToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(transferRequest)))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void testGetBalance_WalletNotFound() throws Exception {
		// Arrange
		String nonExistentWalletId = UUID.randomUUID().toString();
		when(jwtUtil.getAuthenticatedUser()).thenReturn(nonExistentWalletId);
		when(walletService.getWallet(nonExistentWalletId)).thenThrow(new NotFoundException("Wallet not found"));

		// Act & Assert
		mockMvc.perform(get("/api/v1/wallet/{walletId}/balance", nonExistentWalletId)
				.with(SecurityMockMvcRequestPostProcessors.csrf())
				.header("Authorization", "Bearer " + testToken))
				.andExpect(status().isNotFound());
	}
}