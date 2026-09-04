package by.innowise.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequestDto(

    @NotNull
    @Positive
    Long orderId,

    @NotNull
    @Positive
    Long userId,

    @NotNull
    @Positive
    BigDecimal paymentAmount
) {

}