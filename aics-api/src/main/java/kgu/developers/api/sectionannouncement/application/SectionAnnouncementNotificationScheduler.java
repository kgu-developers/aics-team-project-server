package kgu.developers.api.sectionannouncement.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SectionAnnouncementNotificationScheduler {

    private static final long FIXED_DELAY_MILLIS = 60_000L;

    private final SectionAnnouncementFacade sectionAnnouncementFacade;

    @Scheduled(fixedDelay = FIXED_DELAY_MILLIS)
    public void publishScheduledAnnouncements() {
        sectionAnnouncementFacade.publishScheduledAnnouncements();
    }
}
