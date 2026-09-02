package by.innowise.paymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic paymentEventsTopic(
      @Value("${app.kafka.payment-events-topic}") String topicName,
      @Value("${app.kafka.payment-events-partitions}") int partitions
  ) {
    return TopicBuilder.name(topicName)
        .partitions(partitions)
        .replicas(1)
        .build();
  }
}