package com.application.clean.interfaceadapter.controller;

import com.application.clean.entity.AccountNotFoundException;
import com.application.clean.entity.InsufficientFundsException;
import com.application.clean.entity.Money;
import com.application.clean.usecase.TransferMoneyUseCase;
import com.application.clean.usecase.TransferMoneyUseCase.TransferCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferMoneyUseCase transferMoney;

    public TransferController(TransferMoneyUseCase transferMoney) {
        this.transferMoney = transferMoney;
    }

    @PostMapping
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        transferMoney.transfer(new TransferCommand(
            request.fromAccountId(),
            request.toAccountId(),
            new Money(request.amount())
        ));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleNotFound(AccountNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficient(InsufficientFundsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    public record TransferRequest(
        @NotBlank String fromAccountId,
        @NotBlank String toAccountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {
    	
    }
}
