package com.premierhub.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily retention job removing raw hashed visitor rows older than 400 days.
 * Keeps aggregated {@code site_page_views_daily} untouched.
 */
@Component
public class AnalyticsRetentionJob {
    private final AnalyticsPageviewService pageviews;
    private final boolean enabled;
    private final int retentionDays;

    public AnalyticsRetentionJob(
            AnalyticsPageviewService pageviews,
            @Value("${premierhub.analytics.retention.enabled:false}") boolean enabled,
            @Value("${premierhub.analytics.retention-days:400}") int retentionDays) {
        this.pageviews = pageviews;
        this.enabled = enabled;
        this.retentionDays = Math.max(1, retentionDays);
    }

    @Scheduled(cron = "${premierhub.analytics.retention.cron:0 15 3 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void scheduledCleanup() {
        if (!enabled) {
            return;
        }
        pageviews.deleteVisitorsOlderThan(retentionDays);
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "premierhub.analytics.retention.enabled", havingValue = "true")
    static class SchedulingConfiguration {
    }
}
