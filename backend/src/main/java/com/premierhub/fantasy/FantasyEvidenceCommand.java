package com.premierhub.fantasy;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name="premierhub.fantasy-evidence.enabled",havingValue="true")
public class FantasyEvidenceCommand implements ApplicationRunner {
    private final FantasyEvidenceImporter importer;
    public FantasyEvidenceCommand(FantasyEvidenceImporter importer) { this.importer=importer; }
    @Override public void run(ApplicationArguments args) throws Exception {
        var result=importer.importFiles(path(args,"stats-file",false),path(args,"unrated-file",false),path(args,"source-file",true));
        System.out.printf("FANTASY_EVIDENCE fixtures=%d fixture_changes=%d unrated_changes=%d%n",
                result.fixtures(),result.fixtureChanges(),result.unratedChanges());
    }
    private static Path path(ApplicationArguments args,String key,boolean required) {
        var values=args.getOptionValues("premierhub.fantasy-evidence."+key);
        if(values==null && !required) return null;
        if(values==null || values.size()!=1 || values.getFirst().isBlank()) throw new IllegalArgumentException("Set one fantasy-evidence."+key);
        return Path.of(values.getFirst());
    }
}
