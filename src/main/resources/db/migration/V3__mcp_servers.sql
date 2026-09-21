-- 1. Таблица глобальных MCP серверов документации и архитектурных правил
CREATE TABLE mcp_servers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    url VARCHAR(1000) NOT NULL,
    target VARCHAR(50) NOT NULL DEFAULT 'COMMON',
    token VARCHAR(1000),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mcp_servers_target ON mcp_servers(target);

-- 2. Предзагрузка эталонных MCP серверов документации Context7
-- Бэкенд и Базы данных (BACKEND)
INSERT INTO mcp_servers (name, url, target, description) VALUES
('Spring Boot Guidelines', 'https://context7.com/spring-projects/spring-boot', 'BACKEND', 'Spring Boot 3.4+, Java 21 LTS records, REST API, Security, Data JPA'),
('Hibernate ORM Best Practices', 'https://context7.com/hibernate/hibernate-orm', 'BACKEND', 'Оптимизация запросов, EntityGraph, индексы на FK, решение N+1 проблемы'),
('Java Modern Conventions', 'https://context7.com/java/openjdk', 'BACKEND', 'Идиоматический Java 21, sealed interfaces, record patterns, virtual threads'),
('PostgreSQL Best Practices', 'https://context7.com/postgres/postgres', 'BACKEND', 'Проектирование схем, JSONB, индексы, транзакции, миграции');

-- Фронтенд, Мобильные платформы и Веб-стандарты (FRONTEND)
INSERT INTO mcp_servers (name, url, target, description) VALUES
('React Guidelines & Hooks', 'https://context7.com/facebook/react', 'FRONTEND', 'Функциональные компоненты, TypeScript, кастомные хуки, мемоизация'),
('Next.js Fullstack Architecture', 'https://context7.com/vercel/next.js', 'FRONTEND', 'App Router, Server Components, Server Actions, Client Components'),
('React Native Mobile Standards', 'https://context7.com/facebook/react-native', 'FRONTEND', 'Мобильная разработка, навигация, фоновые службы, разрешения, кэширование'),
('Flutter Framework & UI Widgets', 'https://context7.com/flutter/flutter', 'FRONTEND', 'Архитектура Flutter, BLoC/Riverpod, реактивные виджеты, репозитории'),
('Dart Language Conventions', 'https://context7.com/dart-lang/sdk', 'FRONTEND', 'Строгая типизация, null-safety, асинхронные потоки, эффективный Dart'),
('TypeScript Strict Typing & Config', 'https://context7.com/microsoft/typescript', 'FRONTEND', 'Strict mode, Discriminated Unions, DTO контракты, generic types'),
('JavaScript (ES6+ / Modern JS)', 'https://context7.com/tc39/ecma262', 'FRONTEND', 'ES2024+ синтаксис, async/await, модули, функциональные паттерны'),
('HTML5 & Web Components', 'https://context7.com/whatwg/html', 'FRONTEND', 'Семантическая разметка, доступность ARIA, веб-компоненты'),
('CSS3 & Modern Styling (Tailwind / Flexbox / Grid)', 'https://context7.com/tailwindlabs/tailwindcss', 'FRONTEND', 'Mobile-first адаптивность, Flexbox/Grid, Utility CSS, Tailwind');

-- 3. Добавление колонок target и token в таблицу серверов проекта
ALTER TABLE project_mcp_servers
    ADD COLUMN target VARCHAR(50) NOT NULL DEFAULT 'COMMON',
    ADD COLUMN token VARCHAR(1000);

-- Классификация уже привязанных к проектам серверов
UPDATE project_mcp_servers
SET target = 'BACKEND'
WHERE name IN ('Spring Boot Guidelines', 'Hibernate ORM Best Practices', 'Java Modern Conventions', 'PostgreSQL Best Practices');

UPDATE project_mcp_servers
SET target = 'FRONTEND'
WHERE name IN ('React Guidelines & Hooks', 'Next.js Fullstack Architecture', 'React Native Mobile Standards',
              'Flutter Framework & UI Widgets', 'Dart Language Conventions', 'TypeScript Strict Typing & Config',
              'JavaScript (ES6+ / Modern JS)', 'HTML5 & Web Components', 'CSS3 & Modern Styling (Tailwind / Flexbox / Grid)');
