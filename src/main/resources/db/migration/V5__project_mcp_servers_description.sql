-- 1. Добавление колонки description в project_mcp_servers
ALTER TABLE project_mcp_servers ADD COLUMN description TEXT;

-- 2. Обновление эталонных описаний правил в таблице mcp_servers
UPDATE mcp_servers SET description = 'Spring Boot 3.4+, Java 21 LTS records for DTOs, constructor injection, @Valid, RFC 7807 ProblemDetail, Spring Data JPA with explicit transactions.' WHERE name = 'Spring Boot Guidelines';
UPDATE mcp_servers SET description = 'Avoid N+1 queries using EntityGraph/JOIN FETCH, use database indexes on FKs, immutable projections, bidirectional helper methods, validate ddl-auto.' WHERE name = 'Hibernate ORM Best Practices';
UPDATE mcp_servers SET description = 'Idiomatic Java 21, sealed interfaces, record patterns, pattern matching for switch, virtual threads for I/O, immutable collections.' WHERE name = 'Java Modern Conventions';
UPDATE mcp_servers SET description = 'Use UUID/bigserial PKs, JSONB for flexible attributes, TIMESTAMP WITH TIME ZONE, Flyway schema migrations, proper index coverage.' WHERE name = 'PostgreSQL Best Practices';
UPDATE mcp_servers SET description = 'Functional components, TypeScript strict types, custom hooks for business logic, memoization for expensive renders, clean state management.' WHERE name = 'React Guidelines & Hooks';
UPDATE mcp_servers SET description = 'App Router architecture, React Server Components by default, Client Components with ''use client'', Server Actions for mutations.' WHERE name = 'Next.js Fullstack Architecture';
UPDATE mcp_servers SET description = 'Modular components, platform-specific adaptations (iOS/Android), offline-first caching, background services handling, smooth animations.' WHERE name = 'React Native Mobile Standards';
UPDATE mcp_servers SET description = 'State management (Bloc/Riverpod), const widgets for rebuild optimization, responsive layouts, repository pattern for API abstraction.' WHERE name = 'Flutter Framework & UI Widgets';
UPDATE mcp_servers SET description = 'Strict null-safety, effective Dart naming, async/await with error handling, immutable data models.' WHERE name = 'Dart Language Conventions';
UPDATE mcp_servers SET description = 'Strict mode enabled, no ''any'' types, discriminated unions for state, explicit return types for API contracts.' WHERE name = 'TypeScript Strict Typing & Config';
UPDATE mcp_servers SET description = 'ES2024+ syntax, async/await, modular ES modules, immutability patterns, structured error handling.' WHERE name = 'JavaScript (ES6+ / Modern JS)';
UPDATE mcp_servers SET description = 'Semantic HTML5 elements (header, main, section, nav, article), accessible ARIA labels, standards-compliant layout.' WHERE name = 'HTML5 & Web Components';
UPDATE mcp_servers SET description = 'Mobile-first responsive design, utility-first CSS via Tailwind, flexbox/grid for layouts, accessible contrast ratios.' WHERE name = 'CSS3 & Modern Styling (Tailwind / Flexbox / Grid)';

-- 3. Синхронизация description в project_mcp_servers из mcp_servers для уже существующих записей
UPDATE project_mcp_servers pms
SET description = ms.description
FROM mcp_servers ms
WHERE pms.name = ms.name;
