# 08 — Modular Monolith (Spring Modulith)

One deployable, internally split into modules with **enforced** boundaries. Modules talk through public APIs, not internal classes.

## Structure

```
com.example.modulith
├── ModularMonolithApplication.java       @Modulithic
├── order/                                MODULE
│   ├── package-info.java                 @ApplicationModule(allowedDependencies={"inventory"})
│   ├── OrderController.java              public — exposed to web layer
│   └── internal/                         NOT accessible from other modules
│       ├── OrderEntity.java
│       ├── OrderJpaRepository.java
│       └── OrderApplicationService.java  depends on InventoryApi (public)
└── inventory/                            MODULE
    ├── package-info.java                 @ApplicationModule
    ├── InventoryApi.java                 public API
    └── internal/                         NOT accessible from other modules
        ├── InventoryEntity.java
        ├── InventoryJpaRepository.java
        └── InventoryService.java         implements InventoryApi
```

## Run

```bash
mvn spring-boot:run
```

Port **8088**. Seeds three products: `WIDGET-1` (10), `WIDGET-2` (5), `WIDGET-3` (0).

## Try it

```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productCode":"WIDGET-1","quantity":2,"customerEmail":"alice@example.com"}'

# Out of stock — 422
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productCode":"WIDGET-3","quantity":1,"customerEmail":"bob@example.com"}'
```

## Run the architecture check

```bash
mvn test
```

`ModularStructureTests.shouldRespectModuleBoundaries()` will **fail the build** if any module reaches into another's `internal` package. Try it: add `import com.example.modulith.inventory.internal.InventoryEntity` into the order module and rerun.

## What to notice

- **`internal` is a Modulith convention**: classes in `module.internal.*` are off-limits to other modules, even though they're `public` to Java. Modulith uses ArchUnit under the hood to verify this.
- **`OrderApplicationService` depends on `InventoryApi`**, the published interface — never on `InventoryService` or `InventoryEntity`.
- **`allowedDependencies = {"inventory"}`** on the order module — declare module dependencies explicitly. Any other dependency triggers a verification failure.
- **Compare with microservices (07)**: same boundary discipline, single process. No network calls, no eventual consistency, no separate deployments. When boundaries prove out, modules can later be extracted to services.
- **Free documentation**: `Documenter` generates PlantUML module diagrams from the code into `target/spring-modulith-docs/`.
