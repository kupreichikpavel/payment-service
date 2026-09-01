package by.innowise.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

  @Id
  private String id;

  @Field("order_id")
  private Long orderId;

  @Field("user_id")
  private Long userId;

  @Field("status")
  private PaymentStatus status;

  @Field("timestamp")
  private Instant timestamp;

  @Field(name = "payment_amount", targetType = FieldType.DECIMAL128)
  private BigDecimal paymentAmount;
}