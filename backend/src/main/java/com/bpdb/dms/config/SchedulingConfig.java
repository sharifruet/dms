package com.bpdb.dms.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns Spring's scheduler on.
 *
 * <p>It has never been on. The application carries fourteen {@code @Scheduled} methods and
 * {@code @EnableScheduling} appeared nowhere, so not one of them has ever run — including
 * the expiry alerts, which is why REQ-E3 was unmet even though alert code existed.
 *
 * <p>Enabling it is deliberately opt-in, because the switch is not selective: it starts
 * <em>every</em> scheduled method at once, and several of the dormant ones are not obviously
 * safe to wake up in an environment that has never run them —
 *
 * <ul>
 *   <li>{@code ReportingService.cleanupOldData} and
 *       {@code NotificationService.cleanupOldNotifications} delete records</li>
 *   <li>{@code DisasterRecoveryService.scheduledBackup} starts writing backups</li>
 *   <li>{@code MultiTenancyService} reads and writes {@code tenants}, whose schema is
 *       known to disagree with its entity, so it would throw on every run</li>
 * </ul>
 *
 * <p>So: review those before switching this on in an environment that matters. Set
 * {@code app.scheduling.enabled=true} once that is done. The procurement expiry warnings
 * can be run on demand in the meantime through
 * {@code POST /api/procurement/expiries/run-warnings}, which is also how they are tested.
 */
@Configuration
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true")
@EnableScheduling
public class SchedulingConfig {
}
