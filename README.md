# README

This super simple project shows us how to configure WebClient's custom connect, read and write time out.

In following code snippet we set connect timeout to 2 second and read timeout 2 second.

> For test cases of this configuration, please look at [WebClientTimeoutTests](/src/test/java/com/tiket/poc/WebClientTimeoutTests.java).

```java
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration(proxyBeanMethods = false)
class ClientConfiguration {
  @Bean
  PaymentClient paymentClient(WebClient.Builder webClients) {
    // Start of essential part
    WebClient webClient = webClients.clone()
        .baseUrl("http://localhost:12345/payment-service")
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
            .tcpConfiguration(tcp -> tcp
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .doOnConnected(connection -> connection
                    .addHandlerLast(new ReadTimeoutHandler(2000, TimeUnit.MILLISECONDS))
                )
            )
        ))
        .build();
    // End of essential part
    
    return new DefaultPaymentClient(webClient);
  }
}
```

Spring boot 2.2+

```java
@Configuration(proxyBeanMethods = false)
class ClientConfiguration {
  @Bean
  PaymentClient paymentClient(WebClient.Builder webClients) {
    // Start of essential part
    WebClient webClient = webClients.clone()
        .baseUrl("http://localhost:12345/payment-service")
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
            .tcpConfiguration(tcp -> tcp
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
            )
            .responseTimeout(Duration.ofMillis(2000))
        ))
        .build();
    // End of essential part
    
    return new DefaultPaymentClient(webClient);
  }
}
```

When connect timed out request stream will throw `io.netty.channel.ConnectTimeoutException` and read timeout will throw `io.netty.handler.timeout.ReadTimeoutException`.