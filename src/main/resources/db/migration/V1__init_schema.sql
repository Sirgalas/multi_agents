-- Base schema definition for AI Orchestrator

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);

-- Projects table
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_projects_user_id ON projects(user_id);

-- Project contexts (code files and specifications)
CREATE TABLE project_contexts (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_content TEXT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_context_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_project_contexts_project_id ON project_contexts(project_id);
CREATE INDEX idx_project_contexts_file_type ON project_contexts(file_type);

-- Project MCP servers
CREATE TABLE project_mcp_servers (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    server_url VARCHAR(500) NOT NULL,
    transport_type VARCHAR(20) NOT NULL DEFAULT 'HTTP',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_mcp_server_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_project_mcp_servers_project_id ON project_mcp_servers(project_id);
CREATE INDEX idx_project_mcp_servers_is_active ON project_mcp_servers(is_active);

-- Agent steps (Execution history and LLM traces)
CREATE TABLE agent_steps (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    step_name VARCHAR(50) NOT NULL,
    step_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    prompt TEXT NOT NULL,
    response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_agent_step_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_steps_project_id ON agent_steps(project_id);
CREATE INDEX idx_agent_steps_step_name ON agent_steps(step_name);
CREATE INDEX idx_agent_steps_created_at ON agent_steps(created_at);