package sportstock.scheduler.job;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sportstock.scheduler.client.IngestionClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreseasonBootstrapJob {

    private static final int FULL_ROSTER_LIMIT = 500;

    private final IngestionClient ingestionClient;

    @Scheduled(cron = "${scheduler.preseason.cron}")
    public void runScheduled() {
        run(null);
    }

    public void run(Integer seasonYear) {
        int year = seasonYear != null ? seasonYear : LocalDate.now().getYear();
        log.info("PreseasonBootstrapJob started for season {}", year);

        try {
            ingestionClient.syncTeams();
        } catch (Exception e) {
            log.warn("Failed to sync teams: {}", e.getMessage());
        }

        try {
            ingestionClient.syncAllTeamDetails(year);
        } catch (Exception e) {
            log.warn("Failed to sync team details: {}", e.getMessage());
        }

        try {
            ingestionClient.syncRosters(year, FULL_ROSTER_LIMIT);
            log.info("Synced full rosters");
        } catch (Exception e) {
            log.warn("Failed to sync rosters: {}", e.getMessage());
        }

        log.info("PreseasonBootstrapJob completed for season {}", year);
    }
}
