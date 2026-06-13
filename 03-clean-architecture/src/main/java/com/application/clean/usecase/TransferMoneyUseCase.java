package com.application.clean.usecase;

import com.application.clean.entity.Money;

/** Input boundary — called by outer layers. */
public interface TransferMoneyUseCase {

	void transfer(TransferCommand command);

	record TransferCommand(String fromAccountId, String toAccountId, Money amount) {
		
	}
}
