package com.example.walletapi.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.walletapi.security.JwtUtil.Jwt;

public class JwtUtilTest {

	private JwtUtil jwtUtil;
	private String testSecret = "testSecretKeyWhichNeedsToBeAtLeast32CharactersLong";
	private long testExpiration = 3600000; // 1 hour

	@BeforeEach
	public void setUp() {
		jwtUtil = new JwtUtil();
		ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
		ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", testExpiration);
	}

	@Test
	public void testGenerateToken() {
		// Arrange
		String walletId = UUID.randomUUID().toString();

		// Act
		String token = jwtUtil.generateToken(walletId);

		// Assert
		assertNotNull(token);
		assertTrue(token.length() > 0);
	}

	@Test
	public void testParseToken() {
		// Arrange
		String walletId = UUID.randomUUID().toString();
		String token = jwtUtil.generateToken(walletId);

		// Act
		Jwt jwt = jwtUtil.parseToken(token);

		// Assert
		assertNotNull(jwt);
		assertEquals(walletId, jwt.getStringClaim("walletId"));
		assertFalse(jwt.isExpired());
	}

	@Test
	public void testGetAuthenticatedUser_Authenticated() {
		// Arrange
		String expectedUserId = UUID.randomUUID().toString();

		Authentication mockAuth = mock(Authentication.class);
		when(mockAuth.isAuthenticated()).thenReturn(true);
		when(mockAuth.getName()).thenReturn(expectedUserId);
		when(mockAuth.getPrincipal()).thenReturn(expectedUserId);

		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(mockAuth);

		SecurityContextHolder.setContext(securityContext);

		// Act
		String result = jwtUtil.getAuthenticatedUser();

		// Assert
		assertEquals(expectedUserId, result);
	}

	@Test
	public void testGetAuthenticatedUser_NotAuthenticated() {
		// Arrange
		Authentication mockAuth = mock(Authentication.class);
		when(mockAuth.isAuthenticated()).thenReturn(false);

		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(mockAuth);

		SecurityContextHolder.setContext(securityContext);

		// Act
		String result = jwtUtil.getAuthenticatedUser();

		// Assert
		assertNull(result);
	}

	@Test
	public void testGetAuthenticatedUser_Anonymous() {
		// Arrange
		Authentication mockAuth = mock(Authentication.class);
		when(mockAuth.isAuthenticated()).thenReturn(true);
		when(mockAuth.getPrincipal()).thenReturn("anonymousUser");

		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(mockAuth);

		SecurityContextHolder.setContext(securityContext);

		// Act
		String result = jwtUtil.getAuthenticatedUser();

		// Assert
		assertNull(result);
	}

	@Test
	public void testGetAuthenticatedUser_NoContext() {
		// Arrange
		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(null);

		SecurityContextHolder.setContext(securityContext);

		// Act
		String result = jwtUtil.getAuthenticatedUser();

		// Assert
		assertNull(result);
	}
}