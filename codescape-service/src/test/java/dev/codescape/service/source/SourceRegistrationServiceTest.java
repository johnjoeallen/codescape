package dev.codescape.service.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SourceRegistrationServiceTest {

    @Autowired
    private SourceRegistrationService registrationService;

    @Test
    void registeringADirectorySourceCreatesTheManagedDirectory() {
        SourceCollection source = registrationService.register(
                "example-dir", SourceType.DIRECTORY, "/opt/vendor/example");

        assertThat(source.getId()).isNotBlank();
        assertThat(source.getSourcePath()).isEqualTo("/opt/vendor/example");
        assertThat(source.getManagedPath()).endsWith("content/example-dir/base");
        assertThat(source.getCapabilities()).containsExactly(Capability.FILESYSTEM);
        assertThat(Files.isDirectory(Path.of(source.getManagedPath()))).isTrue();
    }

    @Test
    void gitSourcesGetGitCapability() {
        SourceCollection source = registrationService.register(
                "example-repo", SourceType.GIT, "/home/user/git/example-repo");

        assertThat(source.getCapabilities()).contains(Capability.FILESYSTEM, Capability.GIT);
    }

    @Test
    void duplicateNamesAreRejected() {
        registrationService.register("dup", SourceType.DIRECTORY, "/tmp/a");

        assertThatThrownBy(() -> registrationService.register("dup", SourceType.DIRECTORY, "/tmp/b"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
