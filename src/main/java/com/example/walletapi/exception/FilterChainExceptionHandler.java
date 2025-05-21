package com.example.walletapi.exception;

import com.example.walletapi.exception.ControllerExceptionHandler.ErrorResponseMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles exceptions thrown by Spring Boot filters before the controllers are
 * hit. For errors thrown after the controllers see
 * {@link ControllerExceptionHandler}.
 * <p>
 * <b>NOTE:</b> This class needs to be added to a filter chain to actually do
 * anything,
 * see
 * {@link com.example.walletapi.security.SecurityConfig#configureCommonHttpSecurity
 * SecurityConfig} for that
 */
public class FilterChainExceptionHandler extends OncePerRequestFilter {

	// private static final Logger log =
	// LoggerFactory.getLogger(FilterChainExceptionHandler.class); // Example logger
	private final ObjectMapper objectMapper = new ObjectMapper();

	private Logger logger;

	private Logger getLogger() {
		if (this.logger == null) {
			this.logger = LoggerFactory.getLogger(this.getClass());
		}
		return this.logger;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		// Wrap the remainder of the chain in a try-block so we can handle all the
		// exceptions....
		try {
			getLogger().info("FilterChainExceptionHandler: PASSING REQUEST DOWN THE CHAIN...");
			filterChain.doFilter(request, response);

		} catch (ResponseStatusException ex) {
			// These are the errors this filter is expected to catch...
			getLogger().info("Correctly caught " + ex.getClass().getName() + ". Responding with " + ex
					.getStatusCode().value() + " to user. The actual error reads:" + ex.getMessage(), ex);
			setErrorResponse(response, ex.getStatusCode().value(), ex.getReason(), request.getRequestURI());
			return;

		} catch (AuthenticationException | AccessDeniedException ex) {
			// These errors are meant to be handled by security.AuthErrorResponse
			getLogger().error("BUGBUG: Caught a " + ex.getClass().getName()
					+ ", these should already have been handled by security.AuthErrorResponse. You may"
					+ "have registered this filter in the wrong place. Rethrowing the error hoping it "
					+ "will be caught by the next filter in the chain...");
			throw ex;

		} catch (Exception ex) {
			// These are unexpected errors which shouldn't happen, but this filter is still
			// meant too catch them...
			getLogger().error(
					"Uncaught error in filter chain. Returning 500 to user. The error reads:" + ex.getMessage(), ex);
			// Send a generic 500 error response to the client
			setErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"An internal server error occurred. Please try again later.", request.getRequestURI());
		}

	}

	private void setErrorResponse(HttpServletResponse response, int status, String message, String path) {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		// Assuming ErrorResponseMap is accessible and defined, perhaps in
		// ControllerExceptionHandler
		// If ErrorResponseMap is an inner class of ControllerExceptionHandler, ensure
		// it is static or FilterChainExceptionHandler has access to it.
		ErrorResponseMap errorDetails = new ErrorResponseMap(status, message, path);

		try {
			objectMapper.writeValue(response.getOutputStream(), errorDetails);
		} catch (IOException e) {
			getLogger().error("Failed to write error response to output stream", e);
		}
	}

}