package by.innowise.paymentservice.client;

import by.innowise.paymentservice.client.RandomNumberClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RandomNumberClientImpl implements RandomNumberClient {

  private final RestClient restClient;
  private final String integersPath;

  public RandomNumberClientImpl(
      @Qualifier("randomOrgRestClient") RestClient restClient,
      @Value("${random-org.integers-path}") String integersPath
  ) {
    this.restClient = restClient;
    this.integersPath = integersPath;
  }

  @Override
  public int getRandomNumber() {
    String response = restClient.get()
        .uri(integersPath)
        .retrieve()
        .body(String.class);

    if (response == null || response.isBlank()) {
      throw new IllegalStateException(
          "Random.org returned empty response"
      );
    }

    return Integer.parseInt(response.trim());
  }
}