package com.sportstock.league.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "services.transaction")
public class TransactionServiceProperties {
    
    private String baseUrl;
    private int port;
    
    public String getFullUrl() {
        return baseUrl + ":" + port;
    }
}
