package steam.vm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class MachineOfflineScheduler {
    private static final Logger log = LoggerFactory.getLogger(MachineOfflineScheduler.class);
    private final MachineService service;
    private final long offlineAfterSeconds;

    public MachineOfflineScheduler(
            MachineService service,
            @Value("${app.machines.offline-after-seconds:20}") long offlineAfterSeconds) {
        this.service = service;
        this.offlineAfterSeconds = offlineAfterSeconds;
    }

    @Scheduled(fixedDelayString = "${app.machines.offline-check-ms:1000}")
    public void markStaleMachinesOffline() {
        try {
            int updated = service.markOffline(LocalDateTime.now().minusSeconds(offlineAfterSeconds));
            if (updated > 0) log.info("Marked {} machines offline", updated);
        } catch (org.springframework.dao.CannotAcquireLockException exception) {
            log.debug("Offline check briefly collided with heartbeat; retrying on the next tick");
        }
    }
}
