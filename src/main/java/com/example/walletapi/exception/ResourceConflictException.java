package com.example.walletapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ResourceConflictException extends ResponseStatusException {
	public ResourceConflictException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
