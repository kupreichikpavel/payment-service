package by.innowise.paymentservice.repository;

import java.math.BigDecimal;

public record TotalAmountProjection(
    BigDecimal total
) {
}