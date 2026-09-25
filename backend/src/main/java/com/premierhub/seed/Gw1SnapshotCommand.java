package com.premierhub.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "premierhub.snapshot.mode")
public class Gw1SnapshotCommand implements ApplicationRunner {
    private final Gw1SnapshotService snapshots;

    public Gw1SnapshotCommand(Gw1SnapshotService snapshots) {
        this.snapshots = snapshots;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String mode = args.getOptionValues("premierhub.snapshot.mode").getFirst();
        if ("export".equals(mode)) {
            if (!args.containsOption("premierhub.snapshot.file")) {
                throw new IllegalArgumentException("Missing --premierhub.snapshot.file for local export.");
            }
            Path output = Path.of(args.getOptionValues("premierhub.snapshot.file").getFirst());
            snapshots.exportLocal(output);
            System.out.println("GW1 SNAPSHOT exported=" + output);
        } else if ("import".equals(mode)) {
            var result = snapshots.importBundled();
            System.out.println("GW1 SNAPSHOT inserted=" + result.inserted()
                    + " verifiedFixtures=" + result.verifiedFixtures());
        } else {
            throw new IllegalArgumentException("Unsupported premierhub.snapshot.mode: " + mode);
        }
    }
}
