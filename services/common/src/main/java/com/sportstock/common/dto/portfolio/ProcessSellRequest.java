package com.sportstock.common.dto.portfolio;

import java.util.UUID;

import lombok.Data;

@Data
public class ProcessSellRequest {
    private UUID stockId;
    private int decreaseAmmount;
}
