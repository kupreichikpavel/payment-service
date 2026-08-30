package by.innowise.paymentservice.integration;

import by.innowise.paymentservice.dto.PaymentRequestDto;
import by.innowise.paymentservice.dto.PaymentResponseDto;
import by.innowise.paymentservice.entity.Payment;
import by.innowise.paymentservice.entity.PaymentStatus;
import by.innowise.paymentservice.repository.PaymentRepository;
import by.innowise.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
@SpringBootTest(
    properties = "spring.docker.compose.enabled=false"
)
class PaymentFlowIntegrationTest {

  private static final String TOPIC = "payment-events";

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper();

  @Container
  static final MongoDBContainer MONGO =
      new MongoDBContainer("mongo:8.0");

  @Container
  static final KafkaContainer KAFKA =
      new KafkaContainer("apache/kafka:4.0.0");

  static final WireMockServer WIRE_MOCK =
      new WireMockServer(0);

  static {
    WIRE_MOCK.start();
  }

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private PaymentRepository paymentRepository;

  @DynamicPropertySource
  static void registerProperties(
      DynamicPropertyRegistry registry
  ) {
    registry.add(
        "spring.mongodb.uri",
        () -> MONGO.getConnectionString() + "/payment_db"
    );

    registry.add(
        "spring.kafka.bootstrap-servers",
        KAFKA::getBootstrapServers
    );

    registry.add(
        "random-org.base-url",
        () -> "http://localhost:" + WIRE_MOCK.port()
    );

    registry.add(
        "random-org.integers-path",
        () -> "/integers"
    );

    registry.add(
        "app.kafka.payment-events-topic",
        () -> TOPIC
    );
  }

  @BeforeEach
  void setUp() {
    paymentRepository.deleteAll();
    WIRE_MOCK.resetAll();
  }

  @AfterAll
  static void tearDown() {
    if (WIRE_MOCK.isRunning()) {
      WIRE_MOCK.stop();
    }
  }

  @Test
  void shouldCreateSuccessfulPaymentSaveToMongoAndPublishKafkaEvent()
      throws Exception {

    WIRE_MOCK.stubFor(
        get(urlEqualTo("/integers"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("42\n")
            )
    );

    PaymentRequestDto request =
        new PaymentRequestDto(
            100L,
            200L,
            new BigDecimal("99.90")
        );

    PaymentResponseDto response =
        paymentService.create(request);

    assertNotNull(response.id());
    assertEquals(100L, response.orderId());
    assertEquals(200L, response.userId());
    assertEquals(
        PaymentStatus.SUCCESS,
        response.status()
    );

    List<Payment> payments =
        paymentRepository.findByOrderId(100L);

    assertEquals(1, payments.size());

    Payment savedPayment = payments.getFirst();

    assertEquals(
        PaymentStatus.SUCCESS,
        savedPayment.getStatus()
    );

    assertEquals(
        0,
        new BigDecimal("99.90")
            .compareTo(savedPayment.getPaymentAmount())
    );

    ConsumerRecord<String, String> record =
        waitForPaymentEvent(100L);

    JsonNode event =
        OBJECT_MAPPER.readTree(record.value());

    assertEquals(
        "CREATE_PAYMENT",
        event.get("type").asText()
    );

    assertEquals(
        response.id(),
        event.get("paymentId").asText()
    );

    assertEquals(
        100L,
        event.get("orderId").asLong()
    );

    assertEquals(
        200L,
        event.get("userId").asLong()
    );

    assertEquals(
        "SUCCESS",
        event.get("status").asText()
    );
  }

  @Test
  void shouldCreateFailedPaymentSaveToMongoAndPublishKafkaEvent()
      throws Exception {

    WIRE_MOCK.stubFor(
        get(urlEqualTo("/integers"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody("41\n")
            )
    );

    PaymentRequestDto request =
        new PaymentRequestDto(
            101L,
            201L,
            new BigDecimal("49.90")
        );

    PaymentResponseDto response =
        paymentService.create(request);

    assertNotNull(response.id());
    assertEquals(101L, response.orderId());
    assertEquals(201L, response.userId());
    assertEquals(
        PaymentStatus.FAILED,
        response.status()
    );

    List<Payment> payments =
        paymentRepository.findByOrderId(101L);

    assertEquals(1, payments.size());

    Payment savedPayment = payments.getFirst();

    assertEquals(
        PaymentStatus.FAILED,
        savedPayment.getStatus()
    );

    assertEquals(
        0,
        new BigDecimal("49.90")
            .compareTo(savedPayment.getPaymentAmount())
    );

    ConsumerRecord<String, String> record =
        waitForPaymentEvent(101L);

    JsonNode event =
        OBJECT_MAPPER.readTree(record.value());

    assertEquals(
        "CREATE_PAYMENT",
        event.get("type").asText()
    );

    assertEquals(
        response.id(),
        event.get("paymentId").asText()
    );

    assertEquals(
        101L,
        event.get("orderId").asLong()
    );

    assertEquals(
        201L,
        event.get("userId").asLong()
    );

    assertEquals(
        "FAILED",
        event.get("status").asText()
    );
  }

  private ConsumerRecord<String, String> waitForPaymentEvent(
      Long orderId
  ) {
    Properties properties = new Properties();

    properties.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        KAFKA.getBootstrapServers()
    );

    properties.put(
        ConsumerConfig.GROUP_ID_CONFIG,
        "payment-test-" + UUID.randomUUID()
    );

    properties.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class
    );

    properties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class
    );

    properties.put(
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        "earliest"
    );

    try (KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(properties)) {

      consumer.subscribe(List.of(TOPIC));

      long deadline =
          System.currentTimeMillis() + 10_000;

      while (System.currentTimeMillis() < deadline) {
        var records =
            consumer.poll(Duration.ofMillis(500));

        for (ConsumerRecord<String, String> record : records) {
          if (orderId.toString().equals(record.key())) {
            return record;
          }
        }
      }
    }

    return fail(
        "Payment event was not received for order: "
            + orderId
    );
  }
}