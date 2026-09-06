package ru.semavin.telegrambot.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationIntegrationTest {

    private static final DatabaseConfig DATABASE = DatabaseConfig.load();
    private String schema;

    @AfterEach
    void dropTemporarySchema() throws SQLException {
        if (schema == null) {
            return;
        }
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void migratesCleanSchemaAndIsRepeatable() throws SQLException {
        schema = uniqueSchema("clean");
        Flyway flyway = flyway(schema);

        assertEquals(4, flyway.migrate().migrationsExecuted);
        assertEquals(0, flyway.migrate().migrationsExecuted);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertTrue(exists(statement, """
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.tables
                        WHERE table_schema = '%s' AND table_name = 'schedule_changes'
                    )
                    """.formatted(schema)));
            assertTrue(exists(statement, """
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = '%s'
                          AND table_name = 'absences'
                          AND column_name = 'start_date'
                          AND is_nullable = 'NO'
                    )
                    """.formatted(schema)));
            assertTrue(exists(statement, """
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.tables
                        WHERE table_schema = '%s' AND table_name = 'notification_history'
                    )
                    """.formatted(schema)));
            assertTrue(exists(statement, """
                    SELECT EXISTS (
                        SELECT 1 FROM pg_indexes
                        WHERE schemaname = '%s'
                          AND indexname = 'uk_schedule_change_group_request'
                    )
                    """.formatted(schema)));
        }
    }

    @Test
    void baselinesLegacySchemaWithoutLosingRows() throws SQLException {
        schema = uniqueSchema("legacy");
        createLegacySchema(schema);

        assertEquals(4, flyway(schema).migrate().migrationsExecuted);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            try (ResultSet row = statement.executeQuery("""
                    SELECT subject_name, control_sum, occurrence_id, series_id
                    FROM %s.schedule WHERE id = 20
                    """.formatted(schema))) {
                assertTrue(row.next());
                assertEquals("Legacy subject", row.getString("subject_name"));
                assertEquals("legacy-checksum", row.getString("control_sum"));
                assertNotNull(row.getObject("occurrence_id"));
                assertNull(row.getObject("series_id"));
                assertFalse(row.next());
            }
            assertEquals(1, scalarInt(statement, """
                    SELECT count(*) FROM %s.schedule_changes WHERE id = 30
                    """.formatted(schema)));
            assertTrue(exists(statement, """
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = '%s'
                          AND table_name = 'users'
                          AND column_name = 'telegram_write_access_granted'
                    )
                    """.formatted(schema)));
            assertTrue(exists(statement, """
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_schema = '%s'
                          AND table_name = 'schedule_changes'
                          AND column_name = 'client_request_id'
                    )
                    """.formatted(schema)));
            assertEquals(1, scalarInt(statement, """
                    SELECT count(*) FROM %s.flyway_schema_history
                    WHERE type = 'BASELINE' AND version = '0'
                    """.formatted(schema)));
        }
    }

    private Flyway flyway(String schema) {
        return Flyway.configure()
                .dataSource(DATABASE.url(), DATABASE.username(), DATABASE.password())
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .locations("classpath:db/migration")
                .load();
    }

    private void createLegacySchema(String schema) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("""
                    CREATE TABLE %s.groups (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        group_name VARCHAR(20) NOT NULL,
                        starosta_id BIGINT
                    )
                    """.formatted(schema));
            statement.execute("""
                    CREATE TABLE %s.schedule (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        group_id BIGINT,
                        subject_name VARCHAR(255),
                        lesson_type VARCHAR(255),
                        teacher_id BIGINT,
                        classroom VARCHAR(255),
                        lesson_date DATE,
                        start_time TIME(6),
                        end_time TIME(6),
                        lesson_week INTEGER,
                        control_sum VARCHAR(255)
                    )
                    """.formatted(schema));
            statement.execute("""
                    CREATE TABLE %s.schedule_changes (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        subject_name VARCHAR(255),
                        lesson_type VARCHAR(255),
                        teacher_name VARCHAR(255),
                        classroom VARCHAR(255),
                        old_lesson_date DATE,
                        old_start_time TIME(6),
                        old_end_time TIME(6),
                        new_lesson_date DATE,
                        new_start_time TIME(6),
                        new_end_time TIME(6),
                        description VARCHAR(255),
                        deleted BOOLEAN NOT NULL DEFAULT FALSE,
                        old_control_sum VARCHAR(255),
                        group_id BIGINT
                    )
                    """.formatted(schema));
            statement.execute("""
                    INSERT INTO %s.groups (id, group_name) VALUES (10, 'LEGACY')
                    """.formatted(schema));
            statement.execute("""
                    INSERT INTO %s.schedule (
                        id, group_id, subject_name, lesson_type, lesson_date,
                        start_time, end_time, lesson_week, control_sum
                    ) VALUES (
                        20, 10, 'Legacy subject', 'LECTURE', DATE '2026-09-01',
                        TIME '09:00', TIME '10:30', 1, 'legacy-checksum'
                    )
                    """.formatted(schema));
            statement.execute("""
                    INSERT INTO %s.schedule_changes (
                        id, subject_name, old_lesson_date, old_start_time,
                        old_end_time, deleted, group_id
                    ) VALUES (
                        30, 'Legacy subject', DATE '2026-09-01', TIME '09:00',
                        TIME '10:30', FALSE, 10
                    )
                    """.formatted(schema));
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                DATABASE.url(), DATABASE.username(), DATABASE.password());
    }

    private String uniqueSchema(String prefix) {
        return "story00_" + prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean exists(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private int scalarInt(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private record DatabaseConfig(String url, String username, String password) {
        static DatabaseConfig load() {
            Properties local = new Properties();
            Path localEnvironment = Path.of("test.env");
            if (Files.exists(localEnvironment)) {
                try (InputStream input = Files.newInputStream(localEnvironment)) {
                    local.load(input);
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot read local test.env", exception);
                }
            }
            return new DatabaseConfig(
                    required("SPRING_DATASOURCE_URL", local),
                    required("SPRING_DATASOURCE_USERNAME", local),
                    required("SPRING_DATASOURCE_PASSWORD", local));
        }

        private static String required(String name, Properties local) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                value = local.getProperty(name);
            }
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(name + " is required for migration integration tests");
            }
            return value;
        }
    }
}
