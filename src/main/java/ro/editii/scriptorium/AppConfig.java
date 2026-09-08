package ro.editii.scriptorium;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.editii.scriptorium.tei.CombinedTeiRepo;
import ro.editii.scriptorium.tei.TeiRepo;

import java.util.Map;

/**
 * equiv of old application-context.xml but in java
 */
@Configuration @Log
public class AppConfig {

    @Bean
    public TeiRepo teiRepo(@Value("${repo.tei.dirs}") String[] teiDirs,
                           @Value("${repo.tei.filter:}") String repoTeiFilter) {
        return new CombinedTeiRepo(
                Map.of(TeiRepo.PROP_KEY_FILTER, repoTeiFilter),
                    teiDirs);
    }
}
