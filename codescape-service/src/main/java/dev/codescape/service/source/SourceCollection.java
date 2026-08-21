package dev.codescape.service.source;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.EnumSet;
import java.util.Set;

/**
 * A registered source collection: the developer-owned {@code sourcePath} is
 * read-only input; all mutation happens against {@code managedPath}, a copy
 * CodeScape owns under {@code ~/.codescape/content/<name>/}. See AGENTS.md
 * for the full safety model.
 */
@Entity
public class SourceCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private SourceType type;

    /** Developer-owned original. Never written to. */
    private String sourcePath;

    /** CodeScape-owned copy under {@code ~/.codescape/content/<name>/}. */
    private String managedPath;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "source_capability", joinColumns = @jakarta.persistence.JoinColumn(name = "source_id"))
    @Enumerated(EnumType.STRING)
    private Set<Capability> capabilities = EnumSet.noneOf(Capability.class);

    protected SourceCollection() {
        // JPA
    }

    public SourceCollection(String name, SourceType type, String sourcePath, String managedPath,
                             Set<Capability> capabilities) {
        this.name = name;
        this.type = type;
        this.sourcePath = sourcePath;
        this.managedPath = managedPath;
        this.capabilities = EnumSet.copyOf(capabilities);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SourceType getType() {
        return type;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getManagedPath() {
        return managedPath;
    }

    public Set<Capability> getCapabilities() {
        return capabilities;
    }
}
