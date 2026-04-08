package sportstock.scheduler.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sportstock.scheduler.job.DailyCatchupJob;
import sportstock.scheduler.job.GameDayPollingJob;
import sportstock.scheduler.job.InitialStipendCheckJob;
import sportstock.scheduler.job.PreseasonBootstrapJob;
import sportstock.scheduler.job.ProjectionSyncJob;
import sportstock.scheduler.job.WeeklyLifecycleJob;

@RestController
@RequestMapping("/api/v1/scheduler")
@RequiredArgsConstructor
public class SchedulerAdminController {

    private final WeeklyLifecycleJob weeklyLifecycleJob;
    private final ProjectionSyncJob projectionSyncJob;
    private final GameDayPollingJob gameDayPollingJob;
    private final DailyCatchupJob dailyCatchupJob;
    private final InitialStipendCheckJob initialStipendCheckJob;
    private final PreseasonBootstrapJob preseasonBootstrapJob;

    @PostMapping("/trigger/weekly-lifecycle")
    public String triggerWeeklyLifecycle() {
        weeklyLifecycleJob.run();
        return "triggered";
    }

    @PostMapping("/trigger/projection-sync")
    public String triggerProjectionSync() {
        projectionSyncJob.run();
        return "triggered";
    }

    @PostMapping("/trigger/game-poll")
    public String triggerGamePoll() {
        gameDayPollingJob.run();
        return "triggered";
    }

    @PostMapping("/trigger/daily-catchup")
    public String triggerDailyCatchup() {
        dailyCatchupJob.run();
        return "triggered";
    }

    @PostMapping("/trigger/initial-stipend-check")
    public String triggerInitialStipendCheck() {
        initialStipendCheckJob.run();
        return "triggered";
    }

    @PostMapping("/trigger/preseason-bootstrap")
    public String triggerPreseasonBootstrap(
            @RequestParam(required = false) Integer seasonYear) {
        preseasonBootstrapJob.run(seasonYear);
        return "triggered";
    }
}
