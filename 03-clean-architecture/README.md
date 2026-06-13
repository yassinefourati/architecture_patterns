# 03 — Clean Architecture

Robert C. Martin's concentric circles. Dependencies point **only inward**.

## Structure

```
com.example.clean
├── entity/                       INNERMOST — enterprise-wide rules
│   ├── Account.java              (no overdraft invariant)
│   └── Money.java                (value arithmetic)
├── usecase/                      Application-specific rules
│   ├── TransferMoneyUseCase.java     input boundary
│   ├── TransferMoneyInteractor.java  interactor (impl)
│   └── AccountGateway.java           output boundary (defined here!)
├── interfaceadapter/             Translates between use cases and frameworks
│   ├── controller/TransferController.java
│   └── gateway/AccountGatewayImpl.java   implements AccountGateway using JPA
└── framework/                    OUTERMOST — Spring, JPA, web
    ├── persistence/AccountJpaEntity, AccountJpaRepository
    └── config/DataInitializer
```

## Run

```bash
mvn spring-boot:run
```

Port **8083**. Seeds two accounts on startup: `alice` ($500), `bob` ($100).

## Try it

```bash
curl -X POST http://localhost:8083/api/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"alice","toAccountId":"bob","amount":50.00}'

# Try to overdraw — should return 422 Unprocessable Entity
curl -X POST http://localhost:8083/api/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"bob","toAccountId":"alice","amount":99999.00}'
```

## What to notice

- **The dependency rule**: `entity` → nothing. `usecase` → `entity`. `interfaceadapter` → `usecase` + `entity`. `framework` → everything.
- **`AccountGateway` is defined in the use case layer**, but implemented in `interfaceadapter`. This is the **dependency inversion** that lets inner layers stay clean — the interactor calls an interface owned by its own layer, even though the implementation reaches out to JPA.
- **`Account` knows its own invariant** (`withdraw` throws `InsufficientFundsException`). Business rules live with the data they protect.
- **Compare with hexagonal (02)**: very similar in practice — both invert dependencies via interfaces. Clean Architecture's distinguishing trait is the explicit four-layer naming.
