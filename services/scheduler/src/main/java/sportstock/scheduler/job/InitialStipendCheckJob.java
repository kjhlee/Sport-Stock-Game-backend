package sportstock.scheduler.job;

import com.sportstock.common.dto.league.LeagueResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sportstock.scheduler.client.IngestionClient;
import sportstock.scheduler.client.LeagueClient;
import sportstock.scheduler.client.TransactionClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialStipendCheckJob {

    private final IngestionClient ingestionClient;
    private final LeagueClient leagueClient;
    private final TransactionClient transactionClient;

    @Scheduled(cron = "${scheduler.initial-stipend-check.cron}")
    public void run() {
        log.info("InitialStipendCheckJob started");

        if (!ingestionClient.isSeasonActive()) {
            log.info("Season not active, skipping initial stipend check");
            return;
        }

        List<LeagueResponse> pendingLeagues = leagueClient.getPendingInitialStipendLeagues();
        if (pendingLeagues.isEmpty()) {
            log.info("No leagues pending initial stipend");
            return;
        }

        log.info("Found {} leagues pending initial stipend", pendingLeagues.size());

        for (LeagueResponse league : pendingLeagues) {
            processLeague(league);
        }

        log.info("InitialStipendCheckJob completed");
    }

    private void processLeague(LeagueResponse league) {
        Long leagueId = league.id();
        try {
            List<Long> memberIds = leagueClient.getMemberUserIds(leagueId);
            if (memberIds.isEmpty()) {
                log.warn("League {} has no members, skipping initial stipend", leagueId);
                return;
            }

            transactionClient.issueInitialStipends(
                    leagueId, league.initialStipendAmount(), memberIds);
            log.info("Issued initial stipends for league {}", leagueId);

            leagueClient.updateInitialStipendStatus(leagueId, "ISSUED");
            log.info("Updated stipend status to ISSUED for league {}", leagueId);
        } catch (Exception e) {
            log.error(
                    "Failed to process initial stipend for league {}: {}",
                    leagueId,
                    e.getMessage());
        }
    }
}
