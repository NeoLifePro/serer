package steam.vm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import steam.vm.repo.ProfileRepository;

@Service
public class FarmedResetScheduler {
    private static final Logger log = LoggerFactory.getLogger(FarmedResetScheduler.class);

    private final ProfileRepository profileRepo;

    public FarmedResetScheduler(ProfileRepository profileRepo) {
        this.profileRepo = profileRepo;
    }

    @Scheduled(cron = "0 0 4 * * WED", zone = "${app.farm-reset.zone:Europe/Riga}")
    @Transactional
    public void resetWeeklyFarmed() {
        int updated = profileRepo.resetAllFarmed();
        log.info("Weekly farmed reset completed. Updated profiles: {}", updated);
    }
}
