package dev.codescape.service.source;

/**
 * How a registered source was provided. Git sources are the only ones that
 * additionally carry Git/GitHub capabilities.
 */
public enum SourceType {
    GIT,
    DIRECTORY
}
