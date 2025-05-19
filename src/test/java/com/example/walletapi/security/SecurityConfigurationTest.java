package com.example.walletapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Assertions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import com.example.walletapi.dto.responses.WalletCreationResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigurationTest {

	@Autowired
	private MockMvc mockMvc;

	public MvcResult createWallet() throws Exception {
		try {
			return mockMvc.perform(post("/api/v1/wallet/create")
					.with(SecurityMockMvcRequestPostProcessors.csrf()))
					.andReturn();
		} catch (Exception e) {
			System.out.println("-----------------Got exception:\n" + e.getMessage() + "\n" + e.getStackTrace());
			throw new Exception(
					"createWallet() threw an exception while calling API (i.e. it didn't get an error response, but an actual exception was thrown). See Debug Console.");
		}
	}

	public WalletCreationResponseDto getWalletCreationResponse() throws Exception {
		MvcResult result = createWallet();
		try {
			String responseBody = result.getResponse().getContentAsString();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(responseBody);
			UUID walletId = UUID.fromString(jsonNode.get("walletId").asText());
			String token = jsonNode.get("token").asText();
			return new WalletCreationResponseDto(walletId, token);
		} catch (Exception e) {
			throw new Exception(
					"getWalletCreationResponse() failed to create a WalletCreationResponseDto() from the API response. See Debug Console.");
		}
	}

	@Test
	public void testPublicEndpoints() {
		// Test a public endpoint by creating a wallet
		try {
			MvcResult result = createWallet();
			int status = result.getResponse().getStatus();
			Assertions.assertEquals(201, status);
		} catch (Exception e) {
			Assertions.fail(e.getMessage());

		}
	}

	@Test
	public void testProtectedEndpointsWithoutToken() {
		try {
			String walletId = UUID.randomUUID().toString();

			// Test protected endpoints without providing any token, this should result in
			// 401 unauthorized
			MvcResult result;
			result = mockMvc.perform(get("/api/v1/wallet/{walletId}/balance", walletId)
					.with(SecurityMockMvcRequestPostProcessors.csrf()))
					.andReturn();
			int status = result.getResponse().getStatus();
			System.out.println("-----------------Got status: " + status);

			Assertions.assertEquals(401, status);
		} catch (Exception e) {
			System.out.println("-----------------Got exception:\n" + e.getMessage() + "\n" + e.getStackTrace());
			Assertions.fail(
					"Test failed because an exception was thrown instead of an error reponse being returned. See Debug Console.");

		}
	}

	/**
	 * Test that protected endpoints respond with 403 if a token is invalid
	 */
	@Test
	public void testProtectedEndpointsWithInvalidToken() {
		try {

			MvcResult result = mockMvc.perform(get("/api/v1/wallet/{walletId}/balance", "doesnt-matter")
					.header("Authorization", "Bearer thisisnotavalidtoken") // this matters
					.with(SecurityMockMvcRequestPostProcessors.csrf()))
					.andReturn();
			int status = result.getResponse().getStatus();
			System.out.println("-----------------Got status: " + status);

			Assertions.assertEquals(401, status);
		} catch (Exception e) {
			System.out.println("-----------------Got exception:\n" + e.getMessage() + "\n" + e.getStackTrace());
			Assertions.fail(
					"Test failed because an exception was thrown instead of an error reponse being returned. See Debug Console.");
		}
	}

	/**
	 * Test that protected endpoints respond with 403 if a token is valid but
	 * doesn't match the walletId
	 */
	@Test
	public void testProtectedEndpointsWithTokenWalletMismatch() {
		try {
			WalletCreationResponseDto walletCreationResponseDto = getWalletCreationResponse();
			String token = walletCreationResponseDto.getToken();
			String wrongWalletId = UUID.randomUUID().toString();

			MvcResult result = mockMvc.perform(get("/api/v1/wallet/{walletId}/balance", wrongWalletId)
					.header("Authorization", "Bearer " + token)
					.with(SecurityMockMvcRequestPostProcessors.csrf()))
					.andReturn();
			int status = result.getResponse().getStatus();
			System.out.println("-----------------Got status: " + status);

			Assertions.assertEquals(403, status);
		} catch (Exception e) {
			Assertions.fail(e.getMessage());
		}
	}

	/**
	 * Test that protected endpoints respond with 200 when all is good
	 */
	@Test
	public void testProtectedEndpointsWithCorrectToken() {
		try {
			WalletCreationResponseDto walletCreationResponseDto = getWalletCreationResponse();
			String token = walletCreationResponseDto.getToken();
			String wrongWalletId = UUID.randomUUID().toString();

			MvcResult result = mockMvc.perform(get("/api/v1/wallet/{walletId}/balance", wrongWalletId)
					.header("Authorization", "Bearer " + token)
					.with(SecurityMockMvcRequestPostProcessors.csrf()))
					.andReturn();
			int status = result.getResponse().getStatus();
			System.out.println("-----------------Got status: " + status);

			Assertions.assertEquals(200, status);
		} catch (Exception e) {
			System.out.println("-----------------Got exception:\n" + e.getMessage() + "\n" + e.getStackTrace());
			Assertions.fail(
					"Test failed because an exception was thrown instead of an error reponse being returned. See Debug Console.");
		}
	}

	/**
	 * Test that protected endpoints respond with 403 when all is good but CSRF is
	 * missing
	 */
	@Test
	public void testCsrfProtection() {
		try {
			WalletCreationResponseDto walletCreationResponseDto = getWalletCreationResponse();
			String token = walletCreationResponseDto.getToken();
			String wrongWalletId = UUID.randomUUID().toString();

			MvcResult result = mockMvc.perform(get("/api/v1/wallet/{walletId}/balance", wrongWalletId)
					.header("Authorization", "Bearer " + token))
					.andReturn();

			int status = result.getResponse().getStatus();
			System.out.println("-----------------Got status: " + status);

			Assertions.assertEquals(403, status);
		} catch (Exception e) {
			System.out.println("-----------------Got exception:\n" + e.getMessage() + "\n" + e.getStackTrace());
			Assertions.fail(
					"Test failed because an exception was thrown instead of an error reponse being returned. See Debug Console.");
		}
	}
}