-- Baseline schema for SourceCollection (see dev.codescape.service.source).
-- Matches the schema Hibernate's ddl-auto=update had been generating —
-- captured from a live H2 instance via org.h2.tools.Script rather than
-- hand-derived, then written in portable SQL (the auto-generated
-- constraint names like FK3PTTJJLQQ62Q64W795V3DE7YJ are H2-internal
-- synthetic identifiers, not something to depend on).
--
-- This replaces ddl-auto=update, which isn't idempotent for constraints
-- against H2: re-running it against an already-migrated database tries
-- to re-add foreign keys that already exist and fails with "Constraint
-- ... already exists" on every restart (harmless — logged, not fatal —
-- but permanent noise, and a sign schema evolution needs a real tool
-- before more entities/stages land, not Hibernate auto-DDL indefinitely).

create table source_collection (
    id varchar(255) not null primary key,
    name varchar(255),
    type varchar(255) check (type in ('GIT', 'DIRECTORY')),
    source_path varchar(255),
    managed_path varchar(255)
);

create table source_capability (
    source_id varchar(255) not null,
    capabilities varchar(255) check (capabilities in
        ('FILESYSTEM', 'SEARCH', 'SNAPSHOT', 'WORKSPACE', 'BUILD', 'GIT', 'BRANCHES', 'HISTORY', 'GITHUB')),
    constraint uk_source_capability unique (source_id, capabilities),
    constraint fk_source_capability_source foreign key (source_id) references source_collection (id)
);
