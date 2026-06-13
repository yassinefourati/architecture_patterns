package com.application.saga.shipping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);
    private final ConcurrentHashMap<UUID, String> shipments = new ConcurrentHashMap<>();

    public UUID schedule(String address) {
        // Demo failure: any address containing "INVALID" gets rejected
        if (address == null || address.toUpperCase().contains("INVALID")) {
            throw new ShippingException("Cannot ship to invalid address: " + address);
        }
        UUID shipmentId = UUID.randomUUID();
        shipments.put(shipmentId, address);
        log.info("[shipping] scheduled {} â†’ {}", shipmentId, address);
        return shipmentId;
    }

    public void cancel(UUID shipmentId) {
        String addr = shipments.remove(shipmentId);
        if (addr != null) {
            log.info("[shipping] cancelled {} â†’ {}", shipmentId, addr);
        }
    }

    public static class ShippingException extends RuntimeException {
    
        private static final long serialVersionUID = 2343381176983022716L;

		public ShippingException(String msg) {
			super(msg);
		}
    }
}
