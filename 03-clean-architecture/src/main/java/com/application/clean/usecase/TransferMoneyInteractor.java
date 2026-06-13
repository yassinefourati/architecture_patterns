package com.application.clean.usecase;

import com.application.clean.entity.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application-specific business rule (interactor). Orchestrates entities
 * through the input boundary. Depends only on entities and the output
 * boundary interface â€” never on a concrete persistence class.
 */
@Service
public class TransferMoneyInteractor implements TransferMoneyUseCase {

    private final AccountGateway accountGateway;

    public TransferMoneyInteractor(AccountGateway accountGateway) {
        this.accountGateway = accountGateway;
    }

    @Override
    @Transactional
    public void transfer(TransferCommand command) {
        Account from = accountGateway.loadAccount(command.fromAccountId());
        Account to = accountGateway.loadAccount(command.toAccountId());

        from.withdraw(command.amount());
        to.deposit(command.amount());

        accountGateway.saveAccount(from);
        accountGateway.saveAccount(to);
    }

}
