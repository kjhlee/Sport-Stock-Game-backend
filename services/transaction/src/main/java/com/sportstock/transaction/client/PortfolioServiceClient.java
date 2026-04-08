package com.sportstock.transaction.client;

import com.sportstock.common.exceptions.MissingAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.sportstock.common.dto.portfolio.HoldingsResponse;
import com.sportstock.common.dto.portfolio.PortfolioResponse;
import com.sportstock.common.dto.portfolio.ProcessBuyRequest;
import com.sportstock.common.dto.portfolio.ProcessSellRequest;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;


@Component
@RequiredArgsConstructor
public class PortfolioServiceClient {
    private final RestClient portfolioRestClient;

    private String getAuthorizationHeader() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
        throw new MissingAuthenticationException("No request context available");
        }
        if (attrs.getRequest() == null) {
        throw new MissingAuthenticationException("No servlet request available");
        }
        String authorizationHeader = attrs.getRequest().getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
        throw new MissingAuthenticationException("Missing Authorization header on incoming request");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
        throw new MissingAuthenticationException(
            "Authorization header must use Bearer token format (Authorization: Bearer <token>)");
        }
        return authorizationHeader;
    }

    public void processBuy(Long leagueId, UUID stockId, int quantity, BigDecimal price){
        ProcessBuyRequest request = new ProcessBuyRequest();
        request.setStockId(stockId);
        request.setQuantity(quantity);
        request.setPrice(price);

        try {
            portfolioRestClient
                .post()
                .uri("/api/v1/portfolio/{leagueId}/buy", leagueId)
                .header("Authorization", getAuthorizationHeader())
                .body(request)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw e;
        }
    }

    public void processSell(Long leagueId, UUID stockId, int decreaseAmmount){
        ProcessSellRequest request = new ProcessSellRequest();
        request.setStockId(stockId);
        request.setDecreaseAmmount(decreaseAmmount);
        try {
            portfolioRestClient
                .post()
                .uri("/api/v1/portfolio/{leagueId}/sell", leagueId)
                .header("Authorization", getAuthorizationHeader())
                .body(request)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw e;
        }
    }
    public PortfolioResponse getPortfolio(Long leagueId) {
        return portfolioRestClient
            .get()
            .uri("/api/v1/portfolio/{leagueId}", leagueId)
            .header("Authorization", getAuthorizationHeader())
            .retrieve()
            .body(PortfolioResponse.class);
    }

    public HoldingsResponse getHolding(Long leagueId, UUID stockId) {
        return portfolioRestClient
            .get()
            .uri("/api/v1/portfolio/{leagueId}/holdings/{stockId}", leagueId, stockId)
            .header("Authorization", getAuthorizationHeader())
            .retrieve()
            .body(HoldingsResponse.class);
    }
}
