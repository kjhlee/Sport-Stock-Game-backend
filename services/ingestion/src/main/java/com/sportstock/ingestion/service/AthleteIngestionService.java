package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Athlete;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.AthleteMapper;
import com.sportstock.ingestion.mapper.JsonNodeUtils;
import com.sportstock.ingestion.repo.AthleteRepository;
import com.sportstock.ingestion.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.textOrNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class AthleteIngestionService {

    private final EspnApiClient espnApiClient;
    private final AthleteRepository athleteRepository;
    private final RateLimiter rateLimiter;


    @Transactional
    public void ingestAthletes(Integer pageSize, Integer pageCount) {
        int totalIngested = 0;

        for (int page = 1; page <= pageCount; page++) {
            String json = espnApiClient.fetchAthletes(pageSize, page);
            JsonNode root = JsonNodeUtils.parseJson(json);
            JsonNode items = root.path("items");

            if (!items.isArray()) {
                throw new IngestionException("Unexpected ESPN athletes response structure on page " + page);
            }

            for (JsonNode item : items) {
                String ref = textOrNull(item, "$ref");
                if (ref == null) {
                    continue;
                }
                String espnId = extractAthleteIdFromRef(ref);
                if (espnId == null) {
                    continue;
                }

                Athlete athlete = athleteRepository.findByEspnId(espnId).orElseGet(() -> {
                    Athlete a = new Athlete();
                    a.setEspnId(espnId);
                    a.setFullName(espnId);
                    return a;
                });

                if (athlete.getIngestedAt() == null) {
                    athlete.setIngestedAt(java.time.Instant.now());
                }
                athlete.setUpdatedAt(java.time.Instant.now());
                athleteRepository.save(athlete);
                totalIngested++;
            }
            rateLimiter.pause();
        }
        log.info("Ingested {} athlete references across {} pages", totalIngested, pageCount);
    }

    public List<Athlete> listAthletes(String positionAbbreviation) {
        if (positionAbbreviation != null && !positionAbbreviation.isBlank()) {
            return athleteRepository.findByPositionAbbreviationOrderByFullNameAsc(positionAbbreviation);
        }
        return athleteRepository.findAllByOrderByFullNameAsc();
    }

    public Athlete getAthleteByEspnId(String athleteEspnId) {
        return athleteRepository.findByEspnId(athleteEspnId)
                .orElseThrow(() -> new EntityNotFoundException("Athlete not found with ESPN ID: " + athleteEspnId));
    }

    private String extractAthleteIdFromRef(String ref) {
        int idx = ref.lastIndexOf("/athletes/");
        if (idx < 0) {
            return null;
        }
        String tail = ref.substring(idx + "/athletes/".length());
        int q = tail.indexOf('?');
        return q > 0 ? tail.substring(0, q) : tail;
    }

}
