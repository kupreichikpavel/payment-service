package by.innowise.paymentservice.client;

import by.innowise.paymentservice.client.RandomNumberClientImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RandomNumberClientIntegrationTest {

  private WireMockServer wireMockServer;
  private RandomNumberClient randomNumberClient;

  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();

    RestClient restClient = RestClient.builder()
        .baseUrl("http://localhost:" + wireMockServer.port()).build();

    randomNumberClient = new RandomNumberClientImpl(restClient, "/integers");
  }

  @AfterEach
  void tearDown() {
    if (wireMockServer != null && wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  @Test
  void shouldReturnRandomNumber() {
    wireMockServer.stubFor(
        get(urlEqualTo("/integers")).willReturn(aResponse().withStatus(200).withBody("52\n")));

    int result = randomNumberClient.getRandomNumber();

    assertEquals(52, result);

    wireMockServer.verify(getRequestedFor(urlEqualTo("/integers")));
  }
}