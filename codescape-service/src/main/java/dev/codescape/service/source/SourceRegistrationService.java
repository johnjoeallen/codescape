package dev.codescape.service.source;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Stage 0 registration: records a source's identity and where its managed
 * copy will live. Does not touch the filesystem — creating the managed copy
 * is Stage 1 (filesystem capability) and later.
 */
@Service
public class SourceRegistrationService {

    private final SourceCollectionRepository repository;
    private final Path managedRoot;

    public SourceRegistrationService(SourceCollectionRepository repository, ManagedRootProperties properties) {
        this.repository = repository;
        this.managedRoot = properties.sourcesRoot();
    }

    public SourceCollection register(String name, SourceType type, String sourcePath) {
        if (repository.existsByName(name)) {
            throw new IllegalArgumentException("A source named '" + name + "' is already registered");
        }
        Path managedPath = managedRoot.resolve(name).resolve("base");
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
