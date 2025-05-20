package com.example.walletapi.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.walletapi.dto.requests.CreateWalletRequestDto;
import com.example.walletapi.model.impl.Wallet;
import com.example.walletapi.security.JwtUtil;
import com.example.walletapi.service.WalletServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = WalletControllerPublic.class)
public class WalletControllerPublicTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private WalletServiceInterface walletService;

	@MockBean
	private JwtUtil jwtUtil;

	private Wallet testWallet;
	private UUID testWalletId;
	private String jwtToken;
	private String testPassword;

	@BeforeEach
	public void setUp() {
		testPassword = "test-password";
		testWallet = new Wallet(testPassword); // In a real scenario, password would be hashed by service
		testWalletId = testWallet.getId();
		jwtToken = "test-jwt-token";
	}

	@Test
	public void testCreateWallet() throws Exception {
		// Arrange
		CreateWalletRequestDto requestDto = new CreateWalletRequestDto();
		requestDto.setPassword(testPassword);

		when(walletService.createWallet(testPassword)).thenReturn(testWallet);
		when(jwtUtil.generateToken(testWalletId.toString())).thenReturn(jwtToken);

		// Act & Assert
		mockMvc.perform(post("/api/wallet/v1/public/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
				.andExpect(jsonPath("$.token").value(jwtToken));
	}

	// TODO: Add test for GET /api/wallet/v1/public/help
	// TODO: Add test for PUT /api/wallet/v1/public/access
}