CREATE TABLE stream (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL, 
    description VARCHAR(500),
    category VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    stream_key VARCHAR(255) NOT NULL UNIQUE,
    viewer_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);