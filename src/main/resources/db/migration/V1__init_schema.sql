CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    archive_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_status ON projects(status);

CREATE TABLE project_contexts (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000),
    file_content TEXT,
    file_type VARCHAR(50) NOT NULL,
    iteration INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_context_project 
        FOREIGN KEY (project_id) 
        REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_project_contexts_project_id ON project_contexts(project_id);
CREATE INDEX idx_project_contexts_file_type ON project_contexts(file_type);

CREATE TABLE project_mcp_servers (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    server_url VARCHAR(1000) NOT NULL,
    transport_type VARCHAR(50) NOT NULL DEFAULT 'SSE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    config JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_mcp_server_project 
        FOREIGN KEY (project_id) 
        REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_project_mcp_servers_project_id ON project_mcp_servers(project_id);
CREATE INDEX idx_project_mcp_servers_active ON project_mcp_servers(project_id, is_active);

CREATE TABLE agent_steps (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    step_name VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    prompt TEXT NOT NULL,
    response TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_agent_step_project 
        FOREIGN KEY (project_id) 
        REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_steps_project_id ON agent_steps(project_id);
CREATE INDEX idx_agent_steps_step_name ON agent_steps(step_name);
CREATE INDEX idx_agent_steps_status ON agent_steps(status);

CREATE TABLE architect_questions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    agent_step_id BIGINT NOT NULL,
    questions JSONB NOT NULL,
    answers JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answered_at TIMESTAMP,
    CONSTRAINT fk_architect_question_project 
        FOREIGN KEY (project_id) 
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_architect_question_step 
        FOREIGN KEY (agent_step_id) 
        REFERENCES agent_steps(id) ON DELETE CASCADE
);

CREATE INDEX idx_architect_questions_project_id ON architect_questions(project_id);
CREATE INDEX idx_architect_questions_status ON architect_questions(status);