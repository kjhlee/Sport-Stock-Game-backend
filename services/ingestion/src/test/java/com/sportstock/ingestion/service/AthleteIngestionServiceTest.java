package com.sportstock.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.config.EspnApiProperties;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.AthleteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class AthleteIngestionServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock private EspnApiClient espnApiClient;
  @Mock private AthleteRepository athleteRepository;
  @Mock private EspnApiProperties espnApiProperties;
  @Mock private EntityManager entityManager;
  @Mock private JsonPayloadCodec jsonPayloadCodec;
  @Mock private PlatformTransactionManager transactionManager;
  @Mock private TransactionStatus transactionStatus;
  @Mock private Query query;

  private AthleteIngestionService service;
  private JsonNode athletePageNode;

  @BeforeEach
  void setUp() throws Exception {
    service =
        new AthleteIngestionService(
            espnApiClient,
            athleteRepository,
            espnApiProperties,
            entityManager,
            jsonPayloadCodec,
            transactionManager);

    athletePageNode =
        OBJECT_MAPPER.readTree(
            """
                {
                  "items": [
                    {
                      "$ref": "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/athletes/4294246?lang=en&region=us"
                    },
                    {
                      "$ref": "https://sports.core.api.espn.com/v2/sports/football/leagues/nfl/athletes/1234567?lang=en&region=us"
                    }
                  ]
                }
                """);
  }

  @Test
  void ingestAthletesUsesOnConflictInsertForPlaceholderDuplicates() {
    when(espnApiProperties.getMaxAthleteRowsPerSync()).thenReturn(1000);
    when(espnApiProperties.getAthletePageTransactionTimeoutSeconds()).thenReturn(30);
    when(espnApiClient.fetchAthletes(2, 1)).thenReturn("athlete-page");
    when(jsonPayloadCodec.parseJson("athlete-page")).thenReturn(athletePageNode);
    when(athleteRepository.findByEspnIdIn(anyCollection())).thenReturn(List.of());
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    doNothing().when(transactionManager).commit(transactionStatus);
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.setParameter(anyString(), any())).thenReturn(query);
    when(query.executeUpdate()).thenReturn(1, 0);

    assertDoesNotThrow(() -> service.ingestAthletes(2, 1));

    verify(entityManager, times(2)).createNativeQuery(anyString());
    verify(athleteRepository, never()).saveAll(any());
    verify(athleteRepository, never()).saveAndFlush(any());
  }
}
