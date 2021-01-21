# README

This super simple project shows us how to configure WebClient's custom connect, read and write time out.

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

For spring boot 2.2+

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

For testing of this custom time out configuration, please look at `com.tiket.poc.WebClientTimeoutTests`.