package dev.codescape.service;

import dev.codescape.service.source.ManagedRootProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ManagedRootProperties.class)
public class CodescapeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodescapeServiceApplication.class, args);
    }
}
