CREATE TABLE file_structure_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    structure_tree JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE task_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    content TEXT NOT NULL,
    file_structure_template_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_template_file_structure 
        FOREIGN KEY (file_structure_template_id) 
        REFERENCES file_structure_templates(id) ON DELETE SET NULL
);

ALTER TABLE projects 
    ADD COLUMN task_template_id BIGINT,
    ADD CONSTRAINT fk_project_task_template 
        FOREIGN KEY (task_template_id) 
        REFERENCES task_templates(id) ON DELETE SET NULL;

CREATE INDEX idx_projects_task_template_id ON projects(task_template_id);

-- Предзагрузка эталонного дерева файлов Clean Architecture (Java 21 + Spring Boot)
INSERT INTO file_structure_templates (name, description, structure_tree) VALUES
('Clean Architecture (Java 21 + Spring Boot)', 
 'Эталонная структура микросервиса на Java 21 с разделением на слои Clean Architecture',
 '{
   "type": "directory",
   "name": "root",
   "children": [
     {
       "type": "directory",
       "name": "src",
       "children": [
         {
           "type": "directory",
           "name": "main",
           "children": [
             {
               "type": "directory",
               "name": "java",
               "children": [
                 {
                   "type": "directory",
                   "name": "ru",
                   "children": [
                     {
                       "type": "directory",
                       "name": "sergalas",
                       "children": [
                         {
                           "type": "directory",
                           "name": "project",
                           "children": [
                             {"type": "file", "name": "Application.java"},
                             {
                               "type": "directory",
                               "name": "config",
                               "children": []
                             },
                             {
                               "type": "directory",
                               "name": "controller",
                               "children": []
                             },
                             {
                               "type": "directory",
                               "name": "dto",
                               "children": [
                                 {"type": "directory", "name": "request"},
                                 {"type": "directory", "name": "response"}
                               ]
                             },
                             {
                               "type": "directory",
                               "name": "entity",
                               "children": [
                                 {"type": "directory", "name": "enums"}
                               ]
                             },
                             {
                               "type": "directory",
                               "name": "exception",
                               "children": []
                             },
                             {
                               "type": "directory",
                               "name": "repository",
                               "children": []
                             },
                             {
                               "type": "directory",
                               "name": "service",
                               "children": []
                             }
                           ]
                         }
                       ]
                     }
                   ]
                 }
               ]
             },
             {
               "type": "directory",
               "name": "resources",
               "children": [
                 {"type": "file", "name": "application.yaml"},
                 {
                   "type": "directory",
                   "name": "db",
                   "children": [
                     {"type": "directory", "name": "migration"}
                   ]
                 },
                 {
                   "type": "directory",
                   "name": "static",
                   "children": [
                     {"type": "directory", "name": "css"},
                     {"type": "directory", "name": "js"}
                   ]
                 },
                 {
                   "type": "directory",
                   "name": "templates",
                   "children": []
                 }
               ]
             }
           ]
         },
         {
           "type": "directory",
           "name": "test",
           "children": [
             {
               "type": "directory",
               "name": "java",
               "children": []
             }
           ]
         }
       ]
     },
     {"type": "file", "name": "build.gradle"},
     {"type": "file", "name": "settings.gradle"},
     {"type": "file", "name": "Dockerfile"},
     {"type": "file", "name": "docker-compose.yml"},
     {"type": "file", "name": ".gitignore"},
     {"type": "file", "name": "README.md"},
     {"type": "file", "name": ".env.example"}
   ]
 }'::jsonb
);

-- Предзагрузка эталонного ТЗ мультиагентного сервиса
INSERT INTO task_templates (name, description, content, file_structure_template_id) VALUES
('AI Multi-Agent Orchestrator', 
 'Эталонное Техническое Задание для создания Spring Boot микросервиса мультиагентной оркестрации',
 '# ТЕХНИЧЕСКОЕ ЗАДАНИЕ: Spring AI Multi-Agent Orchestrator Service

## 1. НАЗНАЧЕНИЕ СИСТЕМЫ
Разработка веб-сервиса на Java 21 LTS и Spring Boot 3.4 для интерактивного формирования требований и авто-генерации компилируемых микросервисов с помощью мультиагентного конвейера LLM.

## 2. ФУНКЦИОНАЛЬНЫЕ ТРЕБОВАНИЯ
- Авторизация и аутентификация пользователей (Spring Security, BCrypt).
- 3-шаговый мастер создания проектов с встроенным WYSIWYG Markdown редактором (EasyMDE).
- Динамический сбор контекста с внешних MCP-серверов документации (Context7).
- Пять специализированных ИИ-агентов (Interviewer, Architect, Worker, Tester, Helper).
- Human-in-the-Loop (HITL): приостановка конвейера при архитектурных вопросах и возобновление после ответов пользователя.
- Автоматический парсинг файлов с маркерами `[FILE: path]` и создание ZIP-архива всего проекта.

## 3. ТЕХНИЧЕСКИЕ ТРЕБОВАНИЯ И СТЕК
- Java 21 LTS, Spring Boot 3.4.x, Spring AI OpenAI starter.
- PostgreSQL 17 + Flyway migrations + JSONB storage.
- AdminLTE 3 UI + WebJars (Bootstrap 5, FontAwesome 6, Select2, EasyMDE).',
 (SELECT id FROM file_structure_templates WHERE name = 'Clean Architecture (Java 21 + Spring Boot)' LIMIT 1)
);