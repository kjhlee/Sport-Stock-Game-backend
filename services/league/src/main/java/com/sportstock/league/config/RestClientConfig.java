package com.sportstock.league.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient transactionRestClient(TransactionServiceProperties props) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
    requestFactory.setReadTimeout(Duration.ofSeconds(30));

    return RestClient.builder()
        .requestFactory(requestFactory)
        .baseUrl(props.getFullUrl())
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
