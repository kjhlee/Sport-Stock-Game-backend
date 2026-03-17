package com.sportstock.stockmarket.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient ingestionRestClient(
      @Value("${ingestion.api.base-url}") String ingestionBaseUrl) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
    requestFactory.setReadTimeout(Duration.ofSeconds(30));

    return RestClient.builder()
        .requestFactory(requestFactory)
        .baseUrl(ingestionBaseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .requestInterceptor(
            (request, body, execution) -> {
              ServletRequestAttributes attrs =
                  (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
              if (attrs != null) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null) {
                  request.getHeaders().set(HttpHeaders.AUTHORIZATION, auth);
                }
              }
              return execution.execute(request, body);
            })
        .build();
  }
}
