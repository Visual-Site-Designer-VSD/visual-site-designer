package dev.mainul35.cms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs (the live preview uses one to stop abandoned previews). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
