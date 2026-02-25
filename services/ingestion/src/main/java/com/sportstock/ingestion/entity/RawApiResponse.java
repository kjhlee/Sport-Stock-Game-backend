package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "raw_api_responses", schema = "ingestion")
@Getter
@Setter
@NoArgsConstructor
public class RawApiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String endpoint;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "response_body", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responseBody;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}
