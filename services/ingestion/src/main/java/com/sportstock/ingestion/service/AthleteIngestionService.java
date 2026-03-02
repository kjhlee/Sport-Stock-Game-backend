package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.config.EspnApiProperties;
import com.sportstock.ingestion.entity.Athlete;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.AthleteRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.textOrNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class AthleteIngestionService {

    private final EspnApiClient espnApiClient;
    private final AthleteRepository athleteRepository;
    private final EspnApiProperties espnApiProperties;
    private final EntityManager entityManager;
    private final JsonPayloadCodec jsonPayloadCodec;
    private final PlatformTransactionManager transactionManager;


    public void ingestAthletes(Integer pageSize, Integer pageCount) {
        long requestedRows = (long) pageSize * (long) pageCount;
        if (requestedRows > espnApiProperties.getMaxAthleteRowsPerSync()) {
            throw new IngestionException("Requested athlete sync exceeds max allowed rows");
        }

        long jobStartNanos = System.nanoTime();
        long totalFetchNanos = 0L;
        long totalDbNanos = 0L;

        int totalSeen = 0;
        int totalInserted = 0;
        int totalUpdated = 0;
        int totalUnchanged = 0;

        Set<String> seenEspnIds = new HashSet<>();

        TransactionTemplate pageWriteTransaction = new TransactionTemplate(transactionManager);
        pageWriteTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        pageWriteTransaction.setTimeout(espnApiProperties.getAthletePageTransactionTimeoutSeconds());

        for (int page = 1; page <= pageCount; page++) {
            long pageStartNanos = System.nanoTime();

            long fetchStartNanos = System.nanoTime();
            String json = espnApiClient.fetchAthletes(pageSize, page);
            JsonNode root = jsonPayloadCodec.parseJson(json);
            long pageFetchNanos = System.nanoTime() - fetchStartNanos;
            totalFetchNanos += pageFetchNanos;

            JsonNode items = root.path("items");

            if (!items.isArray()) {
                throw new IngestionException("Unexpected ESPN athletes response structure on page " + page);
            }

            LinkedHashSet<String> espnIds = new LinkedHashSet<>();
            for (JsonNode item : items) {
                String ref = textOrNull(item, "$ref");
                if (ref == null) {
                    continue;
                }
                String espnId = extractAthleteIdFromRef(ref);
                if (espnId == null) {
                    continue;
                }
                espnIds.add(espnId);
            }

            espnIds.removeAll(seenEspnIds);
            seenEspnIds.addAll(espnIds);

            totalSeen += espnIds.size();
            if (espnIds.isEmpty()) {
                log.info("Athlete page {} processed: seen=0 inserted=0 updated=0 unchanged=0 durationMs={}",
                        page, millisSince(pageStartNanos));
                continue;
            }

            PageWriteResult pageWriteResult = pageWriteTransaction.execute(status -> writeAthletePage(espnIds));
            if (pageWriteResult == null) {
                throw new IngestionException("Athlete page transaction returned no result for page " + page);
            }

            long pageDbNanos = pageWriteResult.dbNanos();
            totalDbNanos += pageDbNanos;

            totalInserted += pageWriteResult.inserted();
            totalUpdated += pageWriteResult.updated();
            totalUnchanged += pageWriteResult.unchanged();

            log.info(
                    "Athlete page {} processed: seen={} inserted={} updated={} unchanged={} fetchMs={} dbMs={} durationMs={}",
                    page,
                    espnIds.size(),
                    pageWriteResult.inserted(),
                    pageWriteResult.updated(),
                    pageWriteResult.unchanged(),
                    nanosToMillis(pageFetchNanos),
                    nanosToMillis(pageDbNanos),
                    millisSince(pageStartNanos)
            );
        }
        log.info(
                "Athlete sync complete: pages={} seen={} inserted={} updated={} unchanged={} totalFetchMs={} totalDbMs={} totalDurationMs={}",
                pageCount,
                totalSeen,
                totalInserted,
                totalUpdated,
                totalUnchanged,
                nanosToMillis(totalFetchNanos),
                nanosToMillis(totalDbNanos),
                millisSince(jobStartNanos)
        );
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

    private PageWriteResult writeAthletePage(LinkedHashSet<String> espnIds) {
        long dbStartNanos = System.nanoTime();
        Map<String, Athlete> existingByEspnId = athleteRepository.findByEspnIdIn(espnIds).stream()
                .collect(Collectors.toMap(Athlete::getEspnId, Function.identity()));

        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        int skippedDuplicates = 0;
        List<Athlete> newAthletes = new ArrayList<>();

        for (String espnId : espnIds) {
            Athlete existing = existingByEspnId.get(espnId);
            if (existing == null) {
                Instant now = Instant.now();
                Athlete athlete = new Athlete();
                athlete.setEspnId(espnId);
                athlete.setFullName(espnId);
                athlete.setIngestedAt(now);
                athlete.setUpdatedAt(now);
                newAthletes.add(athlete);
                continue;
            }

            boolean changed = false;
            if (existing.getIngestedAt() == null) {
                existing.setIngestedAt(Instant.now());
                changed = true;
            }
            if (existing.getFullName() == null || existing.getFullName().isBlank()) {
                existing.setFullName(espnId);
                changed = true;
            }

            if (changed) {
                existing.setUpdatedAt(Instant.now());
                athleteRepository.save(existing);
                updated++;
            } else {
                unchanged++;
            }
        }

        // Batch insert new athletes (fast path)
        try {
            athleteRepository.saveAll(newAthletes);
            inserted = newAthletes.size();
        } catch (DataIntegrityViolationException e) {
            log.info("Batch insert failed with duplicate, falling back to individual saves");
            entityManager.clear();
            for (Athlete athlete : newAthletes) {
                try {
                    athleteRepository.saveAndFlush(athlete);
                    inserted++;
                } catch (DataIntegrityViolationException ex) {
                    log.debug("Athlete {} already exists, skipping insert", athlete.getEspnId());
                    entityManager.clear();
                    skippedDuplicates++;
                }
            }
        }

        entityManager.flush();
        entityManager.clear();

        if (skippedDuplicates > 0) {
            log.info("Skipped {} duplicate athlete inserts", skippedDuplicates);
        }
        return new PageWriteResult(inserted, updated, unchanged, System.nanoTime() - dbStartNanos);
    }

    private long millisSince(long startNanos) {
        return nanosToMillis(System.nanoTime() - startNanos);
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    private record PageWriteResult(int inserted, int updated, int unchanged, long dbNanos) {
    }

}
