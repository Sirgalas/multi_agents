-- Добавление типа/масштаба проекта (FULLSTACK, BACKEND_ONLY, FRONTEND_ONLY)
ALTER TABLE projects ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'FULLSTACK';

CREATE INDEX idx_projects_type ON projects(type);
