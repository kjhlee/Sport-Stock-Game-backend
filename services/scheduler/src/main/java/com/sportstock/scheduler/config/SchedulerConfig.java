package com.sportstock.scheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import com.sportstock.scheduler.client.IngestionClient;
import com.sportstock.scheduler.client.LeagueClient;
import com.sportstock.scheduler.client.StockMarketClient;
import com.sportstock.scheduler.client.TransactionClient;

@Configuration
public class SchedulerConfig {

    @Bean
    public RestClient ingestionRestClient(
            @Value("${services.ingestion.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient stockMarketRestClient(
            @Value("${services.stock-market.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient transactionRestClient(
            @Value("${services.transaction.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient leagueRestClient(
            @Value("${services.league.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public IngestionClient ingestionClient(RestClient ingestionRestClient) {
        return new IngestionClient(ingestionRestClient);
    }

    @Bean
    public StockMarketClient stockMarketClient(RestClient stockMarketRestClient) {
        return new StockMarketClient(stockMarketRestClient);
    }

    @Bean
    public TransactionClient transactionClient(RestClient transactionRestClient) {
        return new TransactionClient(transactionRestClient);
    }

    @Bean
    public LeagueClient leagueClient(RestClient leagueRestClient) {
        return new LeagueClient(leagueRestClient);
    }
}