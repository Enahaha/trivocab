CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(100),
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    daily_goal INT NOT NULL DEFAULT 20,
    learning_mode VARCHAR(16) NOT NULL DEFAULT 'SIMPLE',
    spelling_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    meaning_display VARCHAR(16) NOT NULL DEFAULT 'SIMPLIFIED',
    theme VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    selected_book_id BIGINT,
    time_zone VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS word_books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    total_words INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_word_books_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_id VARCHAR(64),
    book_id BIGINT NOT NULL,
    priority_rank INT NOT NULL,
    word VARCHAR(160) NOT NULL,
    phonetic VARCHAR(255),
    part_of_speech VARCHAR(120),
    chinese_meaning TEXT NOT NULL,
    korean_meaning TEXT NOT NULL,
    korean_equivalents TEXT,
    korean_definition TEXT,
    korean_source_flag VARCHAR(40),
    english_example TEXT,
    korean_example TEXT,
    learning_stage VARCHAR(120),
    selection_basis VARCHAR(255),
    source_name VARCHAR(255),
    source_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_words_book FOREIGN KEY (book_id) REFERENCES word_books (id),
    CONSTRAINT uk_words_book_word_id UNIQUE (book_id, word_id),
    CONSTRAINT uk_words_book_priority UNIQUE (book_id, priority_rank)
);

CREATE TABLE IF NOT EXISTS study_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    new_count INT NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_sessions_book FOREIGN KEY (book_id) REFERENCES word_books (id)
);

CREATE TABLE IF NOT EXISTS user_word_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'LEARNING',
    ease_factor DECIMAL(4, 2) NOT NULL DEFAULT 2.50,
    interval_days INT NOT NULL DEFAULT 0,
    last_interval_days INT NOT NULL DEFAULT 0,
    repetitions INT NOT NULL DEFAULT 0,
    next_review_at TIMESTAMP NOT NULL,
    last_reviewed_at TIMESTAMP,
    correct_count INT NOT NULL DEFAULT 0,
    wrong_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_progress_word FOREIGN KEY (word_id) REFERENCES words (id),
    CONSTRAINT uk_progress_user_word UNIQUE (user_id, word_id)
);

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
);

CREATE TABLE IF NOT EXISTS checkins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    checkin_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_checkins_user_date UNIQUE (user_id, checkin_date),
    CONSTRAINT fk_checkins_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS review_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_review_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    session_id BIGINT,
    rating VARCHAR(16) NOT NULL,
    response_ms BIGINT NOT NULL DEFAULT 0,
    reviewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_logs_client_id UNIQUE (client_review_id),
    CONSTRAINT fk_review_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_review_logs_word FOREIGN KEY (word_id) REFERENCES words (id),
    CONSTRAINT fk_review_logs_session FOREIGN KEY (session_id) REFERENCES study_sessions (id)
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS login_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(255) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_login_events_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    admin_reply TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_words_book_priority ON words (book_id, priority_rank, id);
CREATE INDEX idx_words_book_word_id ON words (book_id, word_id);
CREATE INDEX idx_words_book_word ON words (book_id, word);
CREATE INDEX idx_progress_due ON user_word_progress (user_id, next_review_at, status, word_id);
CREATE INDEX idx_user_book_settings_user ON user_book_settings (user_id);
CREATE INDEX idx_checkins_user_date ON checkins (user_id, checkin_date);
CREATE INDEX idx_users_selected_book ON users (selected_book_id);
CREATE INDEX idx_review_logs_user_time ON review_logs (user_id, reviewed_at);
CREATE INDEX idx_reset_tokens_user_expiry ON password_reset_tokens (user_id, expires_at, used_at);
CREATE INDEX idx_login_events_created ON login_events (created_at, id);
CREATE INDEX idx_messages_user_created ON messages (user_id, created_at, id);
CREATE INDEX idx_messages_status_created ON messages (status, created_at, id);
