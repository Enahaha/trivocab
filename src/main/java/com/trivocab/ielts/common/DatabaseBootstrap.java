package com.trivocab.ielts.common;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.database-bootstrap", havingValue = "true", matchIfMissing = true)
public class DatabaseBootstrap implements InitializingBean {
    private static final List<SeedBook> EXTRA_BOOKS = List.of(
            new SeedBook("FH_GITHUB_IELTS_3611", 3611, "db/data-book-fh-ielts.sql")
    );

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;
    private final String demoPassword;

    public DatabaseBootstrap(
            DataSource dataSource,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.bootstrap-admin.username:Enahaha}") String adminUsername,
            @Value("${app.auth.bootstrap-admin.email:enahaha@local.trivocab}") String adminEmail,
            @Value("${app.auth.bootstrap-admin.password:123456}") String adminPassword,
            @Value("${app.auth.demo-password:123456}") String demoPassword
    ) {
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername.trim();
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
        this.demoPassword = demoPassword;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        if (!tableExists("words")) {
            executeScript("db/schema.sql");
        }

        migrateAuthenticationSchema();

        if (wordCount() == 0) {
            executeScript("db/data-h2.sql");
        }
        ensureWordBusinessIds();

        initializeLocalAccounts();
        ensureSeedBooks();
    }

    /**
     * Existing file-based H2 databases predate authentication. This migration
     * intentionally uses JDBC metadata instead of a database-specific
     * information-schema query, so it is safe to run repeatedly on H2 and
     * MySQL without replacing the user's vocabulary progress.
     */
    private void migrateAuthenticationSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            addColumnIfMissing(connection, "users", "email", "VARCHAR(255)");
            addColumnIfMissing(connection, "users", "password_hash", "VARCHAR(100)");
            addColumnIfMissing(connection, "users", "role", "VARCHAR(16) NOT NULL DEFAULT 'USER'");
            addColumnIfMissing(connection, "users", "enabled", "BOOLEAN NOT NULL DEFAULT TRUE");
            addColumnIfMissing(connection, "users", "last_login_at", "TIMESTAMP");
            addColumnIfMissing(connection, "users", "selected_book_id", "BIGINT");
            addColumnIfMissing(connection, "words", "word_id", "VARCHAR(64)");
            addColumnIfMissing(connection, "words", "korean_source_flag", "VARCHAR(40)");
            if (actualTableName(connection, "word_books") != null) {
                addColumnIfMissing(
                        connection,
                        "word_books",
                        "updated_at",
                        "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                );
            }

            execute(connection, """
                    CREATE TABLE IF NOT EXISTS password_reset_tokens (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        code_hash VARCHAR(100) NOT NULL,
                        expires_at TIMESTAMP NOT NULL,
                        used_at TIMESTAMP,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
                    )
                    """);
            execute(connection, """
                    CREATE TABLE IF NOT EXISTS login_events (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT,
                        username VARCHAR(255) NOT NULL,
                        event_type VARCHAR(32) NOT NULL,
                        ip_address VARCHAR(64),
                        user_agent VARCHAR(512),
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_login_events_user FOREIGN KEY (user_id) REFERENCES users (id)
                    )
                    """);
            execute(connection, """
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        content TEXT NOT NULL,
                        status VARCHAR(16) NOT NULL DEFAULT 'NEW',
                        admin_reply TEXT,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users (id)
                    )
                    """);
            if (actualTableName(connection, "word_books") != null) {
                execute(connection, """
                        CREATE TABLE IF NOT EXISTS user_book_settings (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            book_id BIGINT NOT NULL,
                            daily_goal INT NOT NULL DEFAULT 20,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT uk_user_book_settings_user_book UNIQUE (user_id, book_id),
                            CONSTRAINT fk_user_book_settings_user FOREIGN KEY (user_id) REFERENCES users (id),
                            CONSTRAINT fk_user_book_settings_book FOREIGN KEY (book_id) REFERENCES word_books (id)
                        )
                        """);
            }
            execute(connection, """
                    CREATE TABLE IF NOT EXISTS checkins (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        checkin_date DATE NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT uk_checkins_user_date UNIQUE (user_id, checkin_date),
                        CONSTRAINT fk_checkins_user FOREIGN KEY (user_id) REFERENCES users (id)
                    )
                    """);

            if (!uniqueIndexForColumnExists(connection, "users", "email")) {
                execute(connection, "CREATE UNIQUE INDEX uk_users_email ON users (email)");
            }
            createIndexIfMissing(connection, "password_reset_tokens", "idx_reset_tokens_user_expiry",
                    "CREATE INDEX idx_reset_tokens_user_expiry ON password_reset_tokens (user_id, expires_at, used_at)");
            createIndexIfMissing(connection, "login_events", "idx_login_events_created",
                    "CREATE INDEX idx_login_events_created ON login_events (created_at, id)");
            createIndexIfMissing(connection, "messages", "idx_messages_user_created",
                    "CREATE INDEX idx_messages_user_created ON messages (user_id, created_at, id)");
            createIndexIfMissing(connection, "messages", "idx_messages_status_created",
                    "CREATE INDEX idx_messages_status_created ON messages (status, created_at, id)");
            if (columnExists(connection, "words", "book_id") && columnExists(connection, "words", "word_id")) {
                dropConstraintIfExists(connection, "words", "uk_words_book_word");
                createIndexIfMissing(connection, "words", "idx_words_book_word_id",
                        "CREATE INDEX idx_words_book_word_id ON words (book_id, word_id)");
            }
            if (actualTableName(connection, "user_book_settings") != null) {
                createIndexIfMissing(connection, "user_book_settings", "idx_user_book_settings_user",
                        "CREATE INDEX idx_user_book_settings_user ON user_book_settings (user_id)");
            }
            if (actualTableName(connection, "checkins") != null) {
                createIndexIfMissing(connection, "checkins", "idx_checkins_user_date",
                        "CREATE INDEX idx_checkins_user_date ON checkins (user_id, checkin_date)");
            }
            createIndexIfMissing(connection, "users", "idx_users_selected_book",
                    "CREATE INDEX idx_users_selected_book ON users (selected_book_id)");
        }
    }

    /**
     * Seeds additional word books (for example the GitHub IELTS list) into
     * databases that already contain the original IELTS book. Fresh databases
     * receive every book from db/data-h2.sql; this method only fills books
     * that are missing, so existing user progress is never touched.
     */
    private void ensureSeedBooks() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists("word_books")) {
                return;
            }
            for (SeedBook seedBook : EXTRA_BOOKS) {
                if (!bookExists(connection, seedBook.code())
                        || countWordsByCode(connection, seedBook.code()) != seedBook.expectedWords()) {
                    executeScript(seedBook.script());
                }
            }
            execute(connection, """
                    UPDATE users
                    SET selected_book_id = (
                        SELECT MIN(id) FROM word_books
                    )
                    WHERE selected_book_id IS NULL
                       OR NOT EXISTS (
                           SELECT 1 FROM word_books wb WHERE wb.id = users.selected_book_id
                       )
                    """);
        }
    }

    private boolean bookExists(Connection connection, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM word_books WHERE code = ?
                """)) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private long countWordsByCode(Connection connection, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM words w
                JOIN word_books wb ON wb.id = w.book_id
                WHERE wb.code = ?
                """)) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void ensureWordBusinessIds() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!columnExists(connection, "words", "word_id")
                    || !columnExists(connection, "words", "book_id")
                    || !columnExists(connection, "words", "priority_rank")) {
                return;
            }
            try (PreparedStatement select = connection.prepareStatement("""
                    SELECT id, book_id, priority_rank
                    FROM words
                    WHERE word_id IS NULL OR TRIM(word_id) = ''
                    ORDER BY book_id, priority_rank, id
                    """);
                 ResultSet rows = select.executeQuery();
                 PreparedStatement update = connection.prepareStatement("""
                    UPDATE words
                    SET word_id = ?
                    WHERE id = ?
                    """)) {
                while (rows.next()) {
                    long bookId = rows.getLong("book_id");
                    int priorityRank = rows.getInt("priority_rank");
                    String prefix = bookId == 1L ? "IELTS" : "BOOK" + bookId;
                    update.setString(1, prefix + "-" + String.format("%04d", priorityRank));
                    update.setLong(2, rows.getLong("id"));
                    update.addBatch();
                }
                update.executeBatch();
            }
        }
    }

    private void initializeLocalAccounts() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            AccountCredentials demo = findAccount(connection, "demo");
            if (demo != null) {
                updateAccountCredentials(
                        connection,
                        demo,
                        "demo@local.trivocab",
                        demoPassword,
                        "USER",
                        false
                );
            }

            AccountCredentials admin = findAccount(connection, adminUsername);
            if (admin == null) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO users (
                            username, display_name, email, password_hash, role, enabled, daily_goal
                        ) VALUES (?, ?, ?, ?, 'ADMIN', TRUE, 20)
                        """)) {
                    statement.setString(1, adminUsername);
                    statement.setString(2, adminUsername);
                    statement.setString(3, adminEmail);
                    statement.setString(4, passwordEncoder.encode(adminPassword));
                    statement.executeUpdate();
                }
            } else {
                updateAccountCredentials(connection, admin, adminEmail, adminPassword, "ADMIN", true);
            }
        }
    }

    private AccountCredentials findAccount(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, email, password_hash
                FROM users
                WHERE LOWER(username) = LOWER(?)
                """)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new AccountCredentials(
                        result.getLong("id"),
                        result.getString("email"),
                        result.getString("password_hash")
                );
            }
        }
    }

    private void updateAccountCredentials(
            Connection connection,
            AccountCredentials account,
            String fallbackEmail,
            String fallbackPassword,
            String role,
            boolean forceEnabled
    ) throws SQLException {
        String email = isBlank(account.email()) ? fallbackEmail : account.email();
        String passwordHash = isBlank(account.passwordHash())
                ? passwordEncoder.encode(fallbackPassword)
                : account.passwordHash();
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE users
                SET email = ?,
                    password_hash = ?,
                    role = ?,
                    enabled = CASE WHEN ? THEN TRUE ELSE enabled END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)) {
            statement.setString(1, email);
            statement.setString(2, passwordHash);
            statement.setString(3, role);
            statement.setBoolean(4, forceEnabled);
            statement.setLong(5, account.id());
            statement.executeUpdate();
        }
    }

    private void addColumnIfMissing(
            Connection connection,
            String tableName,
            String columnName,
            String definition
    ) throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void createIndexIfMissing(
            Connection connection,
            String tableName,
            String indexName,
            String sql
    ) throws SQLException {
        if (!indexExists(connection, tableName, indexName)) {
            execute(connection, sql);
        }
    }

    private boolean tableExists(String expectedTable) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return actualTableName(connection, expectedTable) != null;
        }
    }

    private boolean columnExists(Connection connection, String tableName, String expectedColumn) throws SQLException {
        String actualTable = actualTableName(connection, tableName);
        if (actualTable == null) {
            return false;
        }
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, actualTable, null)) {
            while (columns.next()) {
                if (expectedColumn.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean indexExists(Connection connection, String tableName, String expectedIndex) throws SQLException {
        String actualTable = actualTableName(connection, tableName);
        if (actualTable == null) {
            return false;
        }
        try (ResultSet indexes = connection.getMetaData()
                .getIndexInfo(connection.getCatalog(), null, actualTable, false, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null && expectedIndex.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean uniqueIndexForColumnExists(
            Connection connection,
            String tableName,
            String expectedColumn
    ) throws SQLException {
        String actualTable = actualTableName(connection, tableName);
        if (actualTable == null) {
            return false;
        }
        try (ResultSet indexes = connection.getMetaData()
                .getIndexInfo(connection.getCatalog(), null, actualTable, true, false)) {
            while (indexes.next()) {
                String columnName = indexes.getString("COLUMN_NAME");
                if (columnName != null && expectedColumn.equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void dropConstraintIfExists(
            Connection connection,
            String tableName,
            String constraintName
    ) throws SQLException {
        String actualTable = actualTableName(connection, tableName);
        if (actualTable == null) {
            return;
        }
        boolean found = false;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE LOWER(TABLE_NAME) = LOWER(?)
                  AND LOWER(CONSTRAINT_NAME) = LOWER(?)
                LIMIT 1
                """)) {
            statement.setString(1, actualTable);
            statement.setString(2, constraintName);
            try (ResultSet result = statement.executeQuery()) {
                found = result.next();
            }
        }
        if (found) {
            execute(connection, "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
        }
    }

    private String actualTableName(Connection connection, String expectedTable) throws SQLException {
        try (ResultSet tables = connection.getMetaData()
                .getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (expectedTable.equalsIgnoreCase(tableName)) {
                    return tableName;
                }
            }
        }
        return null;
    }

    private long wordCount() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM words")) {
            result.next();
            return result.getLong(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void executeScript(String classpathLocation) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(new ClassPathResource(classpathLocation));
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record AccountCredentials(long id, String email, String passwordHash) {
    }

    private record SeedBook(String code, int expectedWords, String script) {
    }
}
