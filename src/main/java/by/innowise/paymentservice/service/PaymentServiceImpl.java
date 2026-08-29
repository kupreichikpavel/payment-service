package by.innowise.paymentservice.service;

import by.innowise.paymentservice.client.RandomNumberClient;
import by.innowise.paymentservice.dto.PaymentRequestDto;
import by.innowise.paymentservice.dto.PaymentResponseDto;
import by.innowise.paymentservice.entity.Payment;
import by.innowise.paymentservice.entity.PaymentStatus;
import by.innowise.paymentservice.mapper.PaymentMapper;
import by.innowise.paymentservice.repository.PaymentRepository;
import by.innowise.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;
  private final RandomNumberClient randomNumberClient;

  @Override
  public PaymentResponseDto create(PaymentRequestDto requestDto) {
    Payment payment = paymentMapper.toEntity(requestDto);

    int randomNumber = randomNumberClient.getRandomNumber();

    payment.setStatus(
        randomNumber % 2 == 0
            ? PaymentStatus.SUCCESS
            : PaymentStatus.FAILED
    );

    payment.setTimestamp(Instant.now());

    Payment savedPayment = paymentRepository.save(payment);

    return paymentMapper.toDto(savedPayment);
  }

  @Override
  public List<PaymentResponseDto> getByUserId(Long userId) {
    return paymentMapper.toDtoList(
        paymentRepository.findByUserId(userId)
    );
  }

  @Override
  public List<PaymentResponseDto> getByOrderId(Long orderId) {
    return paymentMapper.toDtoList(
        paymentRepository.findByOrderId(orderId)
    );
  }

  @Override
  public List<PaymentResponseDto> getByStatus(PaymentStatus status) {
    return paymentMapper.toDtoList(
        paymentRepository.findByStatus(status)
    );
  }

  @Override
  public BigDecimal getTotalAmountByUserIdAndPeriod(
      Long userId,
      Instant from,
      Instant to
  ) {
    List<Payment> payments =
        paymentRepository.findByUserIdAndTimestampBetween(
            userId,
            from,
            to
        );

    return calculateTotalAmount(payments);
  }

  @Override
  public BigDecimal getTotalAmountForPeriod(
      Instant from,
      Instant to
  ) {
    List<Payment> payments =
        paymentRepository.findByTimestampBetween(from, to);

    return calculateTotalAmount(payments);
  }

  private BigDecimal calculateTotalAmount(List<Payment> payments) {
    return payments.stream()
        .map(Payment::getPaymentAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}