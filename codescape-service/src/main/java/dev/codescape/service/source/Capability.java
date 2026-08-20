package dev.codescape.service.source;

/**
 * Capabilities a source may support, per the capability-based design in
 * AGENTS.md. Drives what operations/UI a given source exposes instead of
 * branching on {@link SourceType} directly.
 */
public enum Capability {
    FILESYSTEM,
    SEARCH,
    SNAPSHOT,
    WORKSPACE,
    BUILD,
    GIT,
    BRANCHES,
    HISTORY,
    GITHUB
}
