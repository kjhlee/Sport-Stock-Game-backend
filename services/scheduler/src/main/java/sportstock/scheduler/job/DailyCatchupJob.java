package sportstock.scheduler.job;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.ingestion.EventResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sportstock.scheduler.client.IngestionClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCatchupJob {

    private static final int STALE_ROSTER_HOURS = 24;

    private final IngestionClient ingestionClient;

    @Scheduled(cron = "${scheduler.daily-catchup.cron}")
    public void run() {
        log.info("DailyCatchupJob started");

        if (!ingestionClient.isSeasonActive()) {
            log.info("Season not active, skipping daily catchup");
            return;
        }

        CurrentWeekResponse week = ingestionClient.getCurrentWeek();
        int seasonYear = week.seasonYear();
        int seasonType = Integer.parseInt(week.seasonType());
        int weekNumber = week.week();

        try {
            ingestionClient.syncScoreboard(seasonYear, seasonType, weekNumber);
        } catch (Exception e) {
            log.warn("Failed to refresh scoreboard during daily catchup: {}", e.getMessage());
        }

        List<EventResponse> events = ingestionClient.getEvents(seasonYear, seasonType, weekNumber);
        Instant cutoff = Instant.now().minus(36, ChronoUnit.HOURS);

        int summariesSynced = 0;
        for (EventResponse event : events) {
            if (!Boolean.TRUE.equals(event.statusCompleted())) {
                continue;
            }
            if (event.date() != null && event.date().isBefore(cutoff)) {
                continue;
            }
            if (event.summaryIngestedAt() != null) {
                continue;
            }
            try {
                ingestionClient.syncEventSummary(event.espnId());
                summariesSynced++;
            } catch (Exception e) {
                log.warn("Failed to sync event summary for {}: {}", event.espnId(), e.getMessage());
            }
        }

        try {
            ingestionClient.syncStaleRosters(seasonYear, STALE_ROSTER_HOURS);
        } catch (Exception e) {
            log.warn("Failed to refresh rosters during daily catchup: {}", e.getMessage());
        }

        log.info("DailyCatchupJob completed, synced {} summaries", summariesSynced);
    }
}
