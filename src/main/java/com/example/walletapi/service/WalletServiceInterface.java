package com.example.walletapi.service;

import java.math.BigDecimal;

import com.example.walletapi.exception.InsufficientFundsException;
import com.example.walletapi.exception.NotFoundException;
import com.example.walletapi.model.WalletInterface;
import com.example.walletapi.dto.responses.BalanceResponseDto;
import com.example.walletapi.dto.responses.TransferResponseDto;

public interface WalletServiceInterface {

	WalletInterface createWallet();

	WalletInterface getWallet(String walletId) throws NotFoundException;

	TransferResponseDto sendMoney(String sourceWalletId, String destinationWalletId, BigDecimal amount)
			throws InsufficientFundsException, NotFoundException;

	BalanceResponseDto depositMoney(String walletId, BigDecimal amount);
}
