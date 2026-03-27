package com.sportstock.portfolio.entity;

import java.util.UUID;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class Holdings {

    private UUID Id;

    private UUID portfolioId;

    private UUID stock_id;
    
    private float quantity;

    // The price for each quantity per stock 
    private float avgCostBasis;
}
