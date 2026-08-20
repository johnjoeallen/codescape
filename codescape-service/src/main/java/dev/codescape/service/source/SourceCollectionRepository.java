package dev.codescape.service.source;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceCollectionRepository extends JpaRepository<SourceCollection, String> {

    boolean existsByName(String name);
}
