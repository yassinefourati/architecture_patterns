package com.application.clean.framework.config;

import com.application.clean.framework.persistence.AccountJpaEntity;
import com.application.clean.framework.persistence.AccountJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(AccountJpaRepository repository) {
        return args -> {
            repository.save(new AccountJpaEntity("alice", new BigDecimal("500.00")));
            repository.save(new AccountJpaEntity("bob", new BigDecimal("100.00")));
        };
    }

}
