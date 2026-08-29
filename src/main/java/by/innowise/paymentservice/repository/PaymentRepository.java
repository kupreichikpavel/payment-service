package by.innowise.paymentservice.repository;

import by.innowise.paymentservice.entity.Payment;
import by.innowise.paymentservice.entity.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {

  List<Payment> findByUserId(Long userId);

  List<Payment> findByOrderId(Long orderId);

  List<Payment> findByStatus(PaymentStatus status);

  List<Payment> findByUserIdAndTimestampBetween(
      Long userId,
      Instant from,
      Instant to
  );

  List<Payment> findByTimestampBetween(
      Instant from,
      Instant to
  );
}