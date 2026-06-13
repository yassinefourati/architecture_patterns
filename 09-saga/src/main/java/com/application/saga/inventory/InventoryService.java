package com.application.saga.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final Map<String, Integer> stock = new HashMap<>(Map.of(
        "WIDGET-1", 10,
        "WIDGET-2", 5,
        "WIDGET-3", 0
    ));
    private final ConcurrentHashMap<UUID, Reservation> reservations = new ConcurrentHashMap<>();

    public synchronized UUID reserve(String productCode, int quantity) {
        int available = stock.getOrDefault(productCode, -1);
        if (available < 0) throw new InventoryException("Unknown product " + productCode);
        if (available < quantity) {
            throw new InventoryException("Out of stock for " + productCode + " (need " + quantity + ", have " + available + ")");
        }
        stock.put(productCode, available - quantity);
        UUID reservationId = UUID.randomUUID();
        reservations.put(reservationId, new Reservation(productCode, quantity));
        log.info("[inventory] reserved {}Ã—{} â†’ {}", quantity, productCode, reservationId);
        return reservationId;
    }

    public synchronized void release(UUID reservationId) {
        Reservation r = reservations.remove(reservationId);
        if (r != null) {
            stock.merge(r.productCode(), r.quantity(), Integer::sum);
            log.info("[inventory] released {}Ã—{} ({})", r.quantity(), r.productCode(), reservationId);
        }
    }

    private record Reservation(String productCode, int quantity) {}

    public static class InventoryException extends RuntimeException {
        public InventoryException(String msg) { super(msg); }
    }
}
