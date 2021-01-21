package com.tiket.poc;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

/**
 * @author zakyalvan
 */
@SpringBootTest(classes = DemoApplication.class, webEnvironment = WebEnvironment.NONE)
@MockServerSettings(ports = 12345)
class WebClientTimeoutTests {
  private static final HttpRequest TEST_MOCK_REQUEST = HttpRequest.request()
      .withMethod("GET").withPath("/ping")
      .withHeader(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE);

  private static final HttpResponse TEST_MOCK_RESPONSE = HttpResponse.response()
      .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
      .withBody("PONG");

  @Autowired
  private WebClient.Builder webClients;

  @Test
  void whenRequestHandlingFastEnough_thenShouldReturnPong(MockServerClient mockServer) {
    mockServer.when(TEST_MOCK_REQUEST).respond(TEST_MOCK_RESPONSE);

    WebClient webClient = webClients.clone()
        .baseUrl("http://localhost:12345")
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
            .tcpConfiguration(tcp -> tcp
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(2000, TimeUnit.MILLISECONDS)))
            )
        ))
        .build();

    Mono<String> testStream = webClient.get()
        .uri(builder -> builder.path("/ping").build())
        .accept(MediaType.TEXT_PLAIN)
        .retrieve().bodyToMono(String.class);

    StepVerifier.create(testStream)
        .expectSubscription().thenAwait()
        .assertNext(body -> Assertions.assertThat(body).isEqualTo("PONG"))
        .expectComplete()
        .verify(Duration.ofSeconds(5));

    mockServer.verify(TEST_MOCK_REQUEST, VerificationTimes.once());
  }

  @Test
  void whenRequestReadTimedOut_thenShouldThrowTimeoutError(MockServerClient mockServer) {
    mockServer.when(TEST_MOCK_REQUEST)
        .respond(TEST_MOCK_RESPONSE.clone().withDelay(TimeUnit.MILLISECONDS, 2250));

    WebClient webClient = webClients.clone()
        .baseUrl("http://localhost:12345")
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
            .tcpConfiguration(tcp -> tcp
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(2000, TimeUnit.MILLISECONDS)))
            )
        ))
        .build();

    Mono<String> testStream = webClient.get()
        .uri(builder -> builder.path("/ping").build())
        .accept(MediaType.TEXT_PLAIN)
        .retrieve().bodyToMono(String.class);

    StepVerifier.create(testStream)
        .expectSubscription().thenAwait()
        .expectErrorSatisfies(error -> {
          Assertions.assertThat(error).isInstanceOf(ReadTimeoutException.class);
        })
        .verify(Duration.ofSeconds(5));

    mockServer.verify(TEST_MOCK_REQUEST, VerificationTimes.once());
  }

  @Test
  void whenRequestReadTimedOutAgain_thenShouldThrowTimeoutError(MockServerClient mockServer) {
    mockServer.when(TEST_MOCK_REQUEST)
        .respond(TEST_MOCK_RESPONSE.clone().withDelay(TimeUnit.MILLISECONDS, 2250));

    WebClient webClient = webClients.clone()
        .baseUrl("http://localhost:12345")
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
            .tcpConfiguration(tcp -> tcp
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
            )
            // Available in spring boot 2.2+
            .responseTimeout(Duration.ofMillis(2000))
        ))
        .build();

    Mono<String> testStream = webClient.get()
        .uri(builder -> builder.path("/ping").build())
        .accept(MediaType.TEXT_PLAIN)
        .retrieve().bodyToMono(String.class);

    StepVerifier.create(testStream)
        .expectSubscription().thenAwait()
        .expectErrorSatisfies(error -> {
          Assertions.assertThat(error).isInstanceOf(ReadTimeoutException.class);
        })
        .verify(Duration.ofSeconds(5));

    mockServer.verify(TEST_MOCK_REQUEST, VerificationTimes.once());
  }

  @AfterEach
  void tearDown(MockServerClient mockServer) {
    mockServer.reset();
  }
}