package by.innowise.paymentservice.producer;

import by.innowise.paymentservice.event.PaymentEvent;

public interface PaymentEventProducer {

  void send(PaymentEvent event);
}