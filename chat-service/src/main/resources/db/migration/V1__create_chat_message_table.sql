CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    stream_id BIGINT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    username VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_message_stream_id_created_at ON chat_message (stream_id, created_at);
