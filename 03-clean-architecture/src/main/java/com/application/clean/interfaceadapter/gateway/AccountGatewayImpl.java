package com.application.clean.interfaceadapter.gateway;

import com.application.clean.entity.Account;
import com.application.clean.entity.AccountNotFoundException;
import com.application.clean.entity.Money;
import com.application.clean.framework.persistence.AccountJpaEntity;
import com.application.clean.framework.persistence.AccountJpaRepository;
import com.application.clean.usecase.AccountGateway;
import org.springframework.stereotype.Component;

/**
 * Interface adapter: implements the use-case output boundary using JPA.
 * Translates between the entity model and the persistence model.
 */
@Component
public class AccountGatewayImpl implements AccountGateway {

    private final AccountJpaRepository repository;

    public AccountGatewayImpl(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Account loadAccount(String id) {
        AccountJpaEntity entity = repository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        return new Account(entity.getId(), new Money(entity.getBalance()));
    }

    @Override
    public void saveAccount(Account account) {
        AccountJpaEntity entity = repository.findById(account.getId()).orElse(new AccountJpaEntity(account.getId(), account.getBalance().amount()));
        entity.setBalance(account.getBalance().amount());
        repository.save(entity);
    }
}
