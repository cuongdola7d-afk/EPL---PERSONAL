package com.premierhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PremierHubApplication {
    public static void main(String[] args) {
        var context = SpringApplication.run(PremierHubApplication.class, args);
        for (String arg : args) {
            if (arg.equals("--premierhub.sync.enabled=true")
                    || arg.equals("--premierhub.fixture-evidence.enabled=true")) {
                System.exit(SpringApplication.exit(context));
            }
        }
    }
}
