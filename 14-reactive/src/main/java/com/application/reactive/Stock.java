package com.application.reactive;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * R2DBC mapping â€” NOT JPA. Looks similar but uses Spring Data Relational's
 * lightweight annotations. There's no Hibernate, no lazy loading, no proxy.
 */
@Table("stocks")
public class Stock {

    @Id
    private String symbol;

    private String name;

    @Column("last_price")
    private BigDecimal lastPrice;

    @Column("updated_at")
    private Instant updatedAt;

    public Stock() {}

    public Stock(String symbol, String name, BigDecimal lastPrice, Instant updatedAt) {
        this.symbol = symbol;
        this.name = name;
        this.lastPrice = lastPrice;
        this.updatedAt = updatedAt;
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setLastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
