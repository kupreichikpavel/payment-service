package by.innowise.paymentservice.event;

import by.innowise.paymentservice.entity.PaymentStatus;

import java.time.Instant;

public record PaymentEvent(
        PaymentEventType type,
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status,
        Instant timestamp
) {
}