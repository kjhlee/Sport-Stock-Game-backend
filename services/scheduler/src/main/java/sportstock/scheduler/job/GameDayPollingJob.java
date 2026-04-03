package sportstock.scheduler.job;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.ingestion.EventResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sportstock.scheduler.client.IngestionClient;
import sportstock.scheduler.client.StockMarketClient;
import sportstock.scheduler.entity.EventState;
import sportstock.scheduler.repo.EventStateRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameDayPollingJob {

    private static final String STATUS_PRE = "pre";
    private static final String STATUS_IN = "in";
    private static final String STATUS_POST = "post";

    private static final String STATE_LOCKED = "LOCKED";
    private static final String STATE_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATE_FINALIZED = "FINALIZED";

    private final IngestionClient ingestionClient;
    private final StockMarketClient stockMarketClient;
    private final EventStateRepository eventStateRepository;

    @Scheduled(fixedDelayString = "${scheduler.game-poll.interval-ms}")
    public void run() {
        if (!ingestionClient.isSeasonActive()) {
            return;
        }

        CurrentWeekResponse week = ingestionClient.getCurrentWeek();
        int seasonYear = week.seasonYear();
        int seasonType = Integer.parseInt(week.seasonType());
        int weekNumber = week.week();

        List<EventResponse> events =
                ingestionClient.getEvents(seasonYear, seasonType, weekNumber);

        if (events.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        boolean hasRelevantWindow =
                events.stream()
                        .anyMatch(
                                event ->
                                        Boolean.TRUE.equals(event.statusCompleted())
                                                || STATUS_IN.equalsIgnoreCase(event.statusState())
                                                || isNearKickoff(event.date(), now));

        if (!hasRelevantWindow) {
            return;
        }

        try {
            ingestionClient.syncScoreboard(seasonYear, seasonType, weekNumber);
            events = ingestionClient.getEvents(seasonYear, seasonType, weekNumber);
        } catch (Exception e) {
            log.warn("Failed to sync scoreboard, using cached data: {}", e.getMessage());
        }

        Map<String, EventState> stateMap =
                eventStateRepository
                        .findByWeekNumberAndSeasonYearAndSeasonType(
                                weekNumber, seasonYear, seasonType)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        EventState::getEventEspnId, Function.identity()));

        for (EventResponse event : events) {
            try {
                processEvent(event, stateMap, weekNumber, seasonYear, seasonType);
            } catch (Exception e) {
                log.error("Error processing event {}: {}", event.espnId(), e.getMessage());
            }
        }
    }

    private void processEvent(
            EventResponse event,
            Map<String, EventState> stateMap,
            int weekNumber,
            int seasonYear,
            int seasonType) {

        String espnId = event.espnId();
        String statusState = event.statusState();
        EventState state = stateMap.get(espnId);

        if (STATUS_PRE.equalsIgnoreCase(statusState)) {
            return;
        }

        if (STATUS_IN.equalsIgnoreCase(statusState)) {
            handleInProgress(espnId, state, weekNumber, seasonYear, seasonType);
            return;
        }

        if (STATUS_POST.equalsIgnoreCase(statusState)
                || Boolean.TRUE.equals(event.statusCompleted())) {
            handleCompleted(espnId, state, weekNumber, seasonYear, seasonType);
        }
    }

    private void handleInProgress(
            String espnId,
            EventState state,
            int weekNumber,
            int seasonYear,
            int seasonType) {

        if (state == null) {
            int locked = stockMarketClient.lockEvent(espnId);
            log.info("Locked {} stocks for event {} (game started)", locked, espnId);

            state = new EventState();
            state.setEventEspnId(espnId);
            state.setStatus(STATE_LOCKED);
            state.setLockedAt(Instant.now());
            state.setWeekNumber(weekNumber);
            state.setSeasonYear(seasonYear);
            state.setSeasonType(seasonType);
            eventStateRepository.save(state);
        }

        if (STATE_LOCKED.equals(state.getStatus())
                || STATE_IN_PROGRESS.equals(state.getStatus())) {
            try {
                ingestionClient.syncActualFantasyPoints(espnId);
                if (STATE_LOCKED.equals(state.getStatus())) {
                    state.setStatus(STATE_IN_PROGRESS);
                    eventStateRepository.save(state);
                }
            } catch (Exception e) {
                log.warn("Failed to sync actual FP for event {}: {}", espnId, e.getMessage());
            }
        }
    }

    private void handleCompleted(
            String espnId,
            EventState state,
            int weekNumber,
            int seasonYear,
            int seasonType) {

        if (state != null && STATE_FINALIZED.equals(state.getStatus())) {
            return;
        }

        if (state == null) {
            state = new EventState();
            state.setEventEspnId(espnId);
            state.setLockedAt(Instant.now());
            state.setWeekNumber(weekNumber);
            state.setSeasonYear(seasonYear);
            state.setSeasonType(seasonType);
        }

        try {
            ingestionClient.syncActualFantasyPoints(espnId);
            stockMarketClient.updateFinalPrices(espnId);
            ingestionClient.markEventCompleted(espnId);

            state.setStatus(STATE_FINALIZED);
            state.setFinalizedAt(Instant.now());
            eventStateRepository.save(state);

            log.info("Finalized event {}", espnId);
        } catch (Exception e) {
            log.error("Failed to finalize event {}: {}", espnId, e.getMessage());
        }
    }

    private boolean isNearKickoff(Instant eventDate, Instant now) {
        if (eventDate == null) {
            return false;
        }
        Instant windowStart = now.minus(30, ChronoUnit.MINUTES);
        Instant windowEnd = now.plus(6, ChronoUnit.HOURS);
        return !eventDate.isBefore(windowStart) && !eventDate.isAfter(windowEnd);
    }
}
