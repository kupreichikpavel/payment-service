package by.innowise.paymentservice.service;

import by.innowise.paymentservice.dto.PaymentRequestDto;
import by.innowise.paymentservice.dto.PaymentResponseDto;
import by.innowise.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentService {

  PaymentResponseDto create(PaymentRequestDto requestDto);

  List<PaymentResponseDto> getByUserId(Long userId);

  List<PaymentResponseDto> getByOrderId(Long orderId);

  List<PaymentResponseDto> getByStatus(PaymentStatus status);

  BigDecimal getTotalAmountByUserIdAndPeriod(
      Long userId,
      Instant from,
      Instant to
  );

  BigDecimal getTotalAmountForPeriod(
      Instant from,
      Instant to
  );
}