package com.application.modulith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic
public class ModularMonolithApplication {
	public static void main(String[] args) {
		SpringApplication.run(ModularMonolithApplication.class, args);
	}
}
