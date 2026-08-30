package by.innowise.paymentservice.producer;

import by.innowise.paymentservice.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducerImpl implements PaymentEventProducer {

  private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

  @Value("${app.kafka.payment-events-topic}")
  private String topicName;

  @Override
  public void send(PaymentEvent event) {
    kafkaTemplate.send(
        topicName,
        event.orderId().toString(),
        event
    );
  }
}