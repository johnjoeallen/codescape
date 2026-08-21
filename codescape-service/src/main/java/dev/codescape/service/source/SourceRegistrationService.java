package dev.codescape.service.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Registers a source and creates its managed directory under
 * {@code ~/.codescape/content/<name>/base/}. Nothing else touches the
 * source yet (no clone/copy of content) — that lands with the filesystem
 * capability (see ROADMAP.md Stage 1).
 */
@Service
public class SourceRegistrationService {

    private final SourceCollectionRepository repository;
    private final Path managedRoot;

    public SourceRegistrationService(SourceCollectionRepository repository, ManagedRootProperties properties) {
        this.repository = repository;
        this.managedRoot = properties.contentRoot();
    }

    public SourceCollection register(String name, SourceType type, String sourcePath) {
        if (repository.existsByName(name)) {
            throw new IllegalArgumentException("A source named '" + name + "' is already registered");
        }
        Path managedPath = managedRoot.resolve(name).resolve("base");
        try {
            Files.createDirectories(managedPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create managed directory " + managedPath, e);
        }
        Set<Capability> capabilities = baseCapabilities(type);
        SourceCollection source = new SourceCollection(name, type, sourcePath, managedPath.toString(), capabilities);
        return repository.save(source);
    }

    public List<SourceCollection> list() {
        return repository.findAll();
    }

    private static Set<Capability> baseCapabilities(SourceType type) {
        return switch (type) {
            case DIRECTORY -> EnumSet.of(Capability.FILESYSTEM);
            case GIT -> EnumSet.of(Capability.FILESYSTEM, Capability.GIT);
        };
    }
}
