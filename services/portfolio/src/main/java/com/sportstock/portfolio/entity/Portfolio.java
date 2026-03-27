package com.sportstock.portfolio.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class Portfolio {
    private UUID id;
    private Long userId;
    private Long leagueId;
    private List holdingsList = new ArrayList<Holdings>();
}
