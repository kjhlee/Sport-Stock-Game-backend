package com.sportstocks.stockmarket.model.entity.ingestion;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Immutable
@Table(name = "teams", schema = "ingestion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngestionTeam {

    @Id
    private Long id;

    @Column(name = "espn_id", nullable = false)
    private String espnId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "abbreviation", nullable = false)
    private String abbreviation;
}