package dev.mainul35.vsd.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * VSD live runtime. Serves a published site snapshot, owns visitor sessions and state,
 * hosts plugin event handlers and pushes state changes to browsers.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class RuntimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuntimeApplication.class, args);
    }
}
