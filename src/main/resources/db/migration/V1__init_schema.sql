CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL DEFAULT 'ROLE_USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projects_user_id ON projects(user_id);

CREATE TABLE project_contexts (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    file_name    VARCHAR(255) NOT NULL,
    file_content TEXT         NOT NULL,
    file_type    VARCHAR(32)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_file_type CHECK (file_type IN ('TASK','CONTEXT_CODE','SPEC','ENV_FILE'))
);

CREATE INDEX idx_project_contexts_project_id ON project_contexts(project_id);

CREATE TABLE project_mcp_servers (
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    server_url     TEXT         NOT NULL,
    transport_type VARCHAR(16)  NOT NULL DEFAULT 'SSE',
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_transport_type CHECK (transport_type IN ('SSE','HTTP','STDIO'))
);

CREATE INDEX idx_mcp_servers_project_id ON project_mcp_servers(project_id);

CREATE TABLE agent_steps (
    id          BIGSERIAL    PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    step_name   VARCHAR(32)  NOT NULL,
    prompt      TEXT         NOT NULL,
    response    TEXT,
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_step_name CHECK (step_name IN ('ARCHITECT','WORKER','TESTER','HELPER')),
    CONSTRAINT chk_step_status CHECK (status IN ('PENDING','RUNNING','DONE','ERROR'))
);

CREATE INDEX idx_agent_steps_project_id ON agent_steps(project_id);
CREATE INDEX idx_agent_steps_created_at ON agent_steps(project_id, created_at);