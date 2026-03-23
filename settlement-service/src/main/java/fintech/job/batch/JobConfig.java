package fintech.job.batch;

import fintech.application.ReconciliationService;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@AllArgsConstructor
public class JobConfig {
    private final ReconciliationService reconciliationService;

    @Scheduled(cron = "0 0 0 * * *")
    public void runReconciliation() {
        reconciliationService.reconcileDaily(LocalDate.now());
    }
}