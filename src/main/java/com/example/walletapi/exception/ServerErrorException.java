package com.example.walletapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ServerErrorException extends ResponseStatusException {
	public ServerErrorException(String message) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, message);
	}

	public ServerErrorException(Integer bugCode) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error: " + bugCode);
	}

}
