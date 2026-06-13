package com.application.saga.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final ConcurrentHashMap<UUID, BigDecimal> charges = new ConcurrentHashMap<>();

    public UUID charge(String customerId, BigDecimal amount) {
        // Demo: reject negative or huge amounts
        if (amount.signum() <= 0) throw new PaymentException("Invalid amount");
        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            throw new PaymentException("Amount exceeds limit for customer " + customerId);
        }
        UUID paymentId = UUID.randomUUID();
        charges.put(paymentId, amount);
        log.info("[payment] charged {} for {} â†’ {}", customerId, amount, paymentId);
        return paymentId;
    }

    public void refund(UUID paymentId) {
        BigDecimal amount = charges.remove(paymentId);
        if (amount != null) {
            log.info("[payment] refunded {} ({})", paymentId, amount);
        }
    }

    public static class PaymentException extends RuntimeException {
		private static final long serialVersionUID = -306759604001253393L;

		public PaymentException(String msg) {
			super(msg);
		}
    }
}
