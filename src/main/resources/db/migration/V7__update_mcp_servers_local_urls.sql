-- 1. Переключение эталонных MCP серверов на локальный DevDocs оффлайн-сервер документации
UPDATE mcp_servers SET url = 'http://localhost:8888/spring_boot' WHERE name = 'Spring Boot Guidelines';
UPDATE mcp_servers SET url = 'http://localhost:8888/hibernate' WHERE name = 'Hibernate ORM Best Practices';
UPDATE mcp_servers SET url = 'http://localhost:8888/openjdk~21' WHERE name = 'Java Modern Conventions';
UPDATE mcp_servers SET url = 'http://localhost:8888/postgresql~16' WHERE name = 'PostgreSQL Best Practices';
UPDATE mcp_servers SET url = 'http://localhost:8888/react' WHERE name = 'React Guidelines & Hooks';
UPDATE mcp_servers SET url = 'http://localhost:8888/nextjs' WHERE name = 'Next.js Fullstack Architecture';
UPDATE mcp_servers SET url = 'http://localhost:8888/react_native' WHERE name = 'React Native Mobile Standards';
UPDATE mcp_servers SET url = 'http://localhost:8888/flutter' WHERE name = 'Flutter Framework & UI Widgets';
UPDATE mcp_servers SET url = 'http://localhost:8888/dart' WHERE name = 'Dart Language Conventions';
UPDATE mcp_servers SET url = 'http://localhost:8888/typescript' WHERE name = 'TypeScript Strict Typing & Config';
UPDATE mcp_servers SET url = 'http://localhost:8888/javascript' WHERE name = 'JavaScript (ES6+ / Modern JS)';
UPDATE mcp_servers SET url = 'http://localhost:8888/html' WHERE name = 'HTML5 & Web Components';
UPDATE mcp_servers SET url = 'http://localhost:8888/css' WHERE name = 'CSS3 & Modern Styling (Tailwind / Flexbox / Grid)';
