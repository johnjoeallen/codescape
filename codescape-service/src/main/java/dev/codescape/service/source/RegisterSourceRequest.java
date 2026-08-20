package dev.codescape.service.source;

import jakarta.validation.constraints.NotBlank;

public record RegisterSourceRequest(
        @NotBlank String name,
        @NotBlank String sourcePath,
        @NotBlank String type
) {
}
