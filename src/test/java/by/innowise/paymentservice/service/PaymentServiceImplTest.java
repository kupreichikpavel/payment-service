package by.innowise.paymentservice.service;

import by.innowise.paymentservice.client.RandomNumberClient;
import by.innowise.paymentservice.dto.PaymentRequestDto;
import by.innowise.paymentservice.dto.PaymentResponseDto;
import by.innowise.paymentservice.entity.Payment;
import by.innowise.paymentservice.entity.PaymentStatus;
import by.innowise.paymentservice.event.PaymentEvent;
import by.innowise.paymentservice.mapper.PaymentMapper;
import by.innowise.paymentservice.producer.PaymentEventProducer;
import by.innowise.paymentservice.repository.PaymentRepository;
import by.innowise.paymentservice.repository.TotalAmountProjection;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private PaymentMapper paymentMapper;

  @Mock
  private RandomNumberClient randomNumberClient;

  @Mock
  private PaymentEventProducer paymentEventProducer;

  @InjectMocks
  private PaymentServiceImpl paymentService;

  @Test
  void createShouldCreateSuccessfulPaymentWhenRandomNumberIsEven() {
    PaymentRequestDto request = new PaymentRequestDto(1L, 10L, new BigDecimal("100.00"));

    Payment payment = Payment.builder().orderId(1L).userId(10L)
        .paymentAmount(new BigDecimal("100.00")).build();

    Instant timestamp = Instant.parse("2026-08-30T10:00:00Z");

    Payment savedPayment = Payment.builder().id("payment-1").orderId(1L).userId(10L)
        .status(PaymentStatus.SUCCESS).timestamp(timestamp).paymentAmount(new BigDecimal("100.00"))
        .build();

    PaymentResponseDto response = new PaymentResponseDto("payment-1", 1L, 10L,
        PaymentStatus.SUCCESS, timestamp, new BigDecimal("100.00"));

    when(paymentMapper.toEntity(request)).thenReturn(payment);
    when(randomNumberClient.getRandomNumber()).thenReturn(2);
    when(paymentRepository.save(payment)).thenReturn(savedPayment);
    when(paymentMapper.toDto(savedPayment)).thenReturn(response);

    PaymentResponseDto result = paymentService.create(request);

    assertEquals(response, result);

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

    verify(paymentRepository).save(paymentCaptor.capture());

    Payment paymentToSave = paymentCaptor.getValue();

    assertEquals(PaymentStatus.SUCCESS, paymentToSave.getStatus());
    assertNotNull(paymentToSave.getTimestamp());

    ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);

    verify(paymentEventProducer).send(eventCaptor.capture());

    PaymentEvent event = eventCaptor.getValue();

    assertEquals("payment-1", event.paymentId());
    assertEquals(1L, event.orderId());
    assertEquals(10L, event.userId());
    assertEquals(PaymentStatus.SUCCESS, event.status());
  }

  @Test
  void createShouldCreateFailedPaymentWhenRandomNumberIsOdd() {
    PaymentRequestDto request = new PaymentRequestDto(2L, 20L, new BigDecimal("50.00"));

    Payment payment = Payment.builder().orderId(2L).userId(20L)
        .paymentAmount(new BigDecimal("50.00")).build();

    Instant timestamp = Instant.parse("2026-08-30T11:00:00Z");

    Payment savedPayment = Payment.builder().id("payment-2").orderId(2L).userId(20L)
        .status(PaymentStatus.FAILED).timestamp(timestamp).paymentAmount(new BigDecimal("50.00"))
        .build();

    PaymentResponseDto response = new PaymentResponseDto("payment-2", 2L, 20L, PaymentStatus.FAILED,
        timestamp, new BigDecimal("50.00"));

    when(paymentMapper.toEntity(request)).thenReturn(payment);
    when(randomNumberClient.getRandomNumber()).thenReturn(3);
    when(paymentRepository.save(payment)).thenReturn(savedPayment);
    when(paymentMapper.toDto(savedPayment)).thenReturn(response);

    PaymentResponseDto result = paymentService.create(request);

    assertEquals(PaymentStatus.FAILED, result.status());

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

    verify(paymentRepository).save(paymentCaptor.capture());

    assertEquals(PaymentStatus.FAILED, paymentCaptor.getValue().getStatus());

    ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);

    verify(paymentEventProducer).send(eventCaptor.capture());

    assertEquals(PaymentStatus.FAILED, eventCaptor.getValue().status());
  }

  @Test
  void getByUserIdShouldReturnPayments() {
    List<Payment> payments = List.of(createPayment("1", 1L, 10L, "20.00"));

    List<PaymentResponseDto> responses = List.of(createResponse("1", 1L, 10L, "20.00"));

    when(paymentRepository.findByUserId(10L)).thenReturn(payments);

    when(paymentMapper.toDtoList(payments)).thenReturn(responses);

    List<PaymentResponseDto> result = paymentService.getByUserId(10L);

    assertEquals(responses, result);
  }

  @Test
  void getByOrderIdShouldReturnPayments() {
    List<Payment> payments = List.of(createPayment("1", 5L, 10L, "30.00"));

    List<PaymentResponseDto> responses = List.of(createResponse("1", 5L, 10L, "30.00"));

    when(paymentRepository.findByOrderId(5L)).thenReturn(payments);

    when(paymentMapper.toDtoList(payments)).thenReturn(responses);

    List<PaymentResponseDto> result = paymentService.getByOrderId(5L);

    assertEquals(responses, result);
  }

  @Test
  void getByStatusShouldReturnPayments() {
    List<Payment> payments = List.of(createPayment("1", 1L, 10L, "40.00"));

    List<PaymentResponseDto> responses = List.of(createResponse("1", 1L, 10L, "40.00"));

    when(paymentRepository.findByStatus(PaymentStatus.SUCCESS)).thenReturn(payments);

    when(paymentMapper.toDtoList(payments)).thenReturn(responses);

    List<PaymentResponseDto> result = paymentService.getByStatus(PaymentStatus.SUCCESS);

    assertEquals(responses, result);
  }

  @Test
  void getTotalAmountByUserIdAndPeriodShouldReturnTotalAmount() {
    Instant from = Instant.parse("2026-08-01T00:00:00Z");
    Instant to = Instant.parse("2026-08-31T23:59:59Z");

    when(paymentRepository.sumAmountByUserIdAndPeriod(10L, from, to))
        .thenReturn(Optional.of(
            new TotalAmountProjection(new BigDecimal("150.00"))
        ));

    BigDecimal result =
        paymentService.getTotalAmountByUserIdAndPeriod(
            10L,
            from,
            to
        );

    assertEquals(new BigDecimal("150.00"), result);

    verify(paymentRepository)
        .sumAmountByUserIdAndPeriod(10L, from, to);
  }

  @Test
  void getTotalAmountByUserIdAndPeriodShouldReturnZeroWhenNoPaymentsFound() {
    Instant from = Instant.parse("2026-08-01T00:00:00Z");
    Instant to = Instant.parse("2026-08-31T23:59:59Z");

    when(paymentRepository.sumAmountByUserIdAndPeriod(10L, from, to))
        .thenReturn(Optional.empty());

    BigDecimal result =
        paymentService.getTotalAmountByUserIdAndPeriod(
            10L,
            from,
            to
        );

    assertEquals(BigDecimal.ZERO, result);
  }

  @Test
  void getTotalAmountForPeriodShouldReturnTotalAmount() {
    Instant from = Instant.parse("2026-08-01T00:00:00Z");
    Instant to = Instant.parse("2026-08-31T23:59:59Z");

    when(paymentRepository.sumAmountForPeriod(from, to))
        .thenReturn(Optional.of(
            new TotalAmountProjection(new BigDecimal("300.00"))
        ));

    BigDecimal result =
        paymentService.getTotalAmountForPeriod(
            from,
            to
        );

    assertEquals(new BigDecimal("300.00"), result);

    verify(paymentRepository)
        .sumAmountForPeriod(from, to);
  }

  @Test
  void getTotalAmountForPeriodShouldReturnZeroWhenNoPaymentsFound() {
    Instant from = Instant.parse("2026-08-01T00:00:00Z");
    Instant to = Instant.parse("2026-08-31T23:59:59Z");

    when(paymentRepository.sumAmountForPeriod(from, to))
        .thenReturn(Optional.empty());

    BigDecimal result =
        paymentService.getTotalAmountForPeriod(
            from,
            to
        );

    assertEquals(BigDecimal.ZERO, result);
  }

  private Payment createPayment(String id, Long orderId, Long userId, String amount) {
    return Payment.builder().id(id).orderId(orderId).userId(userId).status(PaymentStatus.SUCCESS)
        .timestamp(Instant.now()).paymentAmount(new BigDecimal(amount)).build();
  }

  private PaymentResponseDto createResponse(String id, Long orderId, Long userId, String amount) {
    return new PaymentResponseDto(id, orderId, userId, PaymentStatus.SUCCESS, Instant.now(),
        new BigDecimal(amount));
  }
}