package com.trivocab.ielts;

import com.trivocab.ielts.common.DatabaseBootstrap;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseBootstrapMigrationTests {

    @Test
    void migratesAnExistingPreAuthenticationDatabaseIdempotently() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:trivocab-legacy-migration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE users (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(80) NOT NULL UNIQUE,
                        display_name VARCHAR(120) NOT NULL,
                        daily_goal INT NOT NULL DEFAULT 20,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("INSERT INTO users (id, username, display_name) VALUES (1, 'demo', 'Legacy Demo')");
            statement.execute("CREATE TABLE words (id BIGINT AUTO_INCREMENT PRIMARY KEY)");
            statement.execute("INSERT INTO words (id) VALUES (1)");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        DatabaseBootstrap bootstrap = new DatabaseBootstrap(
                dataSource,
                encoder,
                "Enahaha",
                "enahaha@local.trivocab",
                "123456",
                "123456"
        );

        bootstrap.afterPropertiesSet();
        bootstrap.afterPropertiesSet();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertThat(columnExists(connection, "users", "email")).isTrue();
            assertThat(columnExists(connection, "users", "password_hash")).isTrue();
            assertThat(columnExists(connection, "users", "role")).isTrue();
            assertThat(columnExists(connection, "users", "enabled")).isTrue();
            assertThat(columnExists(connection, "users", "last_login_at")).isTrue();
            assertThat(columnExists(connection, "words", "word_id")).isTrue();
            assertThat(tableExists(connection, "password_reset_tokens")).isTrue();
            assertThat(tableExists(connection, "login_events")).isTrue();
            assertThat(tableExists(connection, "messages")).isTrue();

            try (ResultSet admin = statement.executeQuery("""
                    SELECT password_hash, role, enabled
                    FROM users
                    WHERE username = 'Enahaha'
                    """)) {
                assertThat(admin.next()).isTrue();
                assertThat(encoder.matches("123456", admin.getString("password_hash"))).isTrue();
                assertThat(admin.getString("role")).isEqualTo("ADMIN");
                assertThat(admin.getBoolean("enabled")).isTrue();
            }
            try (ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'Enahaha'")) {
                count.next();
                assertThat(count.getInt(1)).isEqualTo(1);
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, null, null)) {
            while (columns.next()) {
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
