package dev.codescape.service.source;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Location of CodeScape's managed-data root (default {@code ~/.codescape}),
 * configurable via {@code codescape.home} — see application.yml. A record so
 * Spring can bind {@code home} without requiring the {@code -parameters}
 * compiler flag for reflection.
 */
@ConfigurationProperties(prefix = "codescape")
public record ManagedRootProperties(String home) {

    public Path root() {
        return Path.of(home);
    }

    public Path sourcesRoot() {
        return root().resolve("sources");
    }

    public Path indexRoot() {
        return root().resolve("index");
    }

    public Path workspacesRoot() {
        return root().resolve("workspaces");
    }
}
