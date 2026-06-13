package com.application.clean.usecase;

import com.application.clean.entity.Account;

/**
 * Output boundary. Defined in the use case layer; implemented by the gateway in the
 * outer layer. This inverts the dependency so the inner layer doesn't depend on
 * the outer one.
 */
public interface AccountGateway {
	Account loadAccount(String id);

	void saveAccount(Account account);

}
