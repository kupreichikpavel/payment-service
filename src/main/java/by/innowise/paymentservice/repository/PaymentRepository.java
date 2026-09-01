package by.innowise.paymentservice.repository;

import by.innowise.paymentservice.entity.Payment;
import by.innowise.paymentservice.entity.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {

  List<Payment> findByUserId(Long userId);

  List<Payment> findByOrderId(Long orderId);

  List<Payment> findByStatus(PaymentStatus status);

  @Aggregation(pipeline = {
      "{ '$match': { 'user_id': ?0, 'timestamp': { '$gte': ?1, '$lte': ?2 } } }",
      "{ '$group': { '_id': null, 'total': { '$sum': '$payment_amount' } } }"
  })
  Optional<TotalAmountProjection> sumAmountByUserIdAndPeriod(
      Long userId,
      Instant from,
      Instant to
  );

  @Aggregation(pipeline = {
      "{ '$match': { 'timestamp': { '$gte': ?0, '$lte': ?1 } } }",
      "{ '$group': { '_id': null, 'total': { '$sum': '$payment_amount' } } }"
  })
  Optional<TotalAmountProjection> sumAmountForPeriod(
      Instant from,
      Instant to
  );
}