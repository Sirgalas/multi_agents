-- 1. Добавляем колонку mcp_server_id в project_mcp_servers
ALTER TABLE project_mcp_servers ADD COLUMN mcp_server_id BIGINT;

-- 2. Связываем существующие серверы проекта с mcp_servers по имени
UPDATE project_mcp_servers pms
SET mcp_server_id = ms.id
FROM mcp_servers ms
WHERE LOWER(pms.name) = LOWER(ms.name);

-- 3. Если в project_mcp_servers были серверы, которых нет в mcp_servers, переносим их в mcp_servers
INSERT INTO mcp_servers (name, url, target, token, description, created_at, updated_at)
SELECT DISTINCT pms.name, pms.server_url, COALESCE(pms.target, 'COMMON'), pms.token, pms.description, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM project_mcp_servers pms
WHERE pms.mcp_server_id IS NULL;

-- Допривязываем перенесенные серверы
UPDATE project_mcp_servers pms
SET mcp_server_id = ms.id
FROM mcp_servers ms
WHERE LOWER(pms.name) = LOWER(ms.name) AND pms.mcp_server_id IS NULL;

-- 4. Удаляем возможные дубликаты записей (один и тот же сервер в одном проекте)
DELETE FROM project_mcp_servers a USING project_mcp_servers b
WHERE a.id < b.id AND a.project_id = b.project_id AND a.mcp_server_id = b.mcp_server_id;

-- 5. Делаем mcp_server_id обязательным и вешаем Foreign Key и Unique Constraint
ALTER TABLE project_mcp_servers ALTER COLUMN mcp_server_id SET NOT NULL;

ALTER TABLE project_mcp_servers
    ADD CONSTRAINT fk_project_mcp_servers_mcp_server
    FOREIGN KEY (mcp_server_id) REFERENCES mcp_servers(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_project_mcp_servers_unique ON project_mcp_servers(project_id, mcp_server_id);

-- 6. Удаляем избыточные дублирующие колонки из project_mcp_servers
ALTER TABLE project_mcp_servers
    DROP COLUMN name,
    DROP COLUMN server_url,
    DROP COLUMN transport_type,
    DROP COLUMN target,
    DROP COLUMN token,
    DROP COLUMN description,
    DROP COLUMN config;
