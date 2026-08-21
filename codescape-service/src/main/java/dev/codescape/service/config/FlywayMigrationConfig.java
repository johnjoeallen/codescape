package dev.codescape.service.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Flyway manually. Spring Boot 4 dropped Flyway auto-configuration
 * entirely (no spring-boot-*-flyway module exists, and
 * spring-boot-autoconfigure no longer references it) — this replicates,
 * minimally, what that auto-configuration used to do: run migrations,
 * then make sure the JPA EntityManagerFactory doesn't initialize (and
 * validate the schema) until after they've applied.
 *
 * <p>Schema lives entirely in db/migration/ now; see application.yml's
 * ddl-auto=validate.
 */
@Configuration
public class FlywayMigrationConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .load();
    }

    /**
     * Static, and returning the post-processor type directly (not via a
     * factory that touches other beans) — required so Spring instantiates
     * this before regular bean creation, rather than triggering premature
     * initialization of this configuration class itself.
     */
    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            for (String name : beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false)) {
                BeanDefinition definition = beanFactory.getBeanDefinition(name);
                String[] existing = definition.getDependsOn();
                String[] withFlyway = new String[existing.length + 1];
                System.arraycopy(existing, 0, withFlyway, 0, existing.length);
                withFlyway[existing.length] = "flyway";
                definition.setDependsOn(withFlyway);
            }
        };
    }
}
