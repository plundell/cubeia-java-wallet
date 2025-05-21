package com.example.walletapi.exception;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Handles exceptions thrown by controllers or after controllers. For errors
 * thrown before that see {@link FilterChainExceptionHandler}.
 */
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

	private Logger logger;

	private Logger getLogger() {
		if (this.logger == null) {
			this.logger = LoggerFactory.getLogger(this.getClass());
		}
		return this.logger;
	}

	// Handle all ResponseStatusException (our custom exceptions extend this)
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Object> handleResponseStatusException(
			ResponseStatusException ex, WebRequest request) {

		ErrorResponseMap body = new ErrorResponseMap(
				ex.getStatusCode().value(),
				ex.getReason(),
				request.getDescription(false).replace("uri=", ""));

		return new ResponseEntity<>(body, ex.getStatusCode());
	}

	// Any Exception which doesn't extend ResponseStatusException is considered
	// unintended
	// for user consumption and replaced by a 500
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
		ErrorResponseMap body = new ErrorResponseMap(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				ex.getMessage(),
				request.getDescription(false).replace("uri=", ""));
		getLogger().error("Uncaught exception. Replacing with a '500 Internal Server Error'.", ex);
		return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * A map of error response data. Common for this file and
	 * {@link FilterChainExceptionHandler}.
	 */
	public static class ErrorResponseMap extends HashMap<String, Object> {
		public ErrorResponseMap(int status, String message, String path) {
			this.put("status", status);
			this.put("statusName", HttpStatus.valueOf(status).getReasonPhrase());
			this.put("message", message);
			this.put("path", path);
		}
	}
}