package com.example.walletapi.security;

import org.junit.jupiter.api.Assertions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.walletapi.dto.responses.WalletAccessResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigurationTest {

	@Autowired
	private MockMvc mockMvc;

	public MvcResult createWallet() throws Exception {
		try {
			return mockMvc.perform(post("/api/wallet/v1/public/create"))
					.andReturn();
		} catch (Exception e) {
			System.out.println("-----------------Got exception:\n" + e.getMessage() + "\n" + e.getStackTrace());
			throw new Exception(
					"createWallet() threw an exception while calling API (i.e. it didn't get an error response, but an actual exception was thrown). See Debug Console.");
		}
	}

	public WalletAccessResponseDto getWalletCreationResponse() throws Exception {
		MvcResult result = createWallet();
		try {
			String responseBody = result.getResponse().getContentAsString();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(responseBody);
			UUID walletId = UUID.fromString(jsonNode.get("walletId").asText());
			String token = jsonNode.get("token").asText();
			return new WalletAccessResponseDto(walletId, token);
		} catch (Exception e) {
			throw new Exception(
					"getWalletCreationResponse() failed to create a WalletCreationResponseDto() from the API response. See Debug Console.");
		}
	}

	@Test
	public void testPublicEndpoints() {
		// Test a public endpoint by creating a wallet
		try {
			MvcResult result = mockMvc.perform(get("/api/wallet/v1/public/help"))
					.andReturn();
			int status = result.getResponse().getStatus();
			Assertions.assertEquals(200, status);
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
			result = mockMvc.perform(get("/api/wallet/v1/balance", walletId))
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

			MvcResult result = mockMvc.perform(get("/api/wallet/v1/balance", "doesnt-matter")
					.header("Authorization", "Bearer thisisnotavalidtoken") // this matters
			)
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
			WalletAccessResponseDto walletCreationResponseDto = getWalletCreationResponse();
			String token = walletCreationResponseDto.getToken();
			String wrongWalletId = UUID.randomUUID().toString();

			MvcResult result = mockMvc.perform(get("/api/wallet/v1/balance", wrongWalletId)
					.header("Authorization", "Bearer " + token))
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
			WalletAccessResponseDto walletCreationResponseDto = getWalletCreationResponse();
			String token = walletCreationResponseDto.getToken();
			String walletId = walletCreationResponseDto.getWalletId().toString();

			MvcResult result = mockMvc.perform(get("/api/wallet/v1/balance", walletId)
					.header("Authorization", "Bearer " + token))
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
	 * Test that protected endpoints respond with 200 when no CSRF token is sent
	 * (CSRF is disabled).
	 */
	@Test
	public void testProtectedEndpointsAccessibleWithoutCsrfToken() {
		try {
			WalletAccessResponseDto walletCreationResponseDto = getWalletCreationResponse();
			String token = walletCreationResponseDto.getToken();
			String walletId = walletCreationResponseDto.getWalletId().toString();

			MvcResult result = mockMvc.perform(get("/api/wallet/v1/balance", walletId)
					.header("Authorization", "Bearer " + token))
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
}