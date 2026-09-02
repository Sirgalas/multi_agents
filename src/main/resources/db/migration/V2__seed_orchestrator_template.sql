-- Insert file structure template for Clean Architecture Java 21 project
INSERT INTO file_structure_templates (name, description, structure_tree) VALUES (
    'Clean Architecture Java 21 Spring Boot',
    'Эталонная структура мультиагентного оркестратора с Spring AI, PostgreSQL, Docker',
    '├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ru/sergalas/orchestrator/
│   │   │       ├── entity/
│   │   │       │   ├── User.java
│   │   │       │   ├── Project.java
│   │   │       │   ├── TaskTemplate.java
│   │   │       │   ├── FileStructureTemplate.java
│   │   │       │   ├── ProjectContext.java
│   │   │       │   ├── ProjectMcpServer.java
│   │   │       │   ├── AgentStep.java
│   │   │       │   └── enums/
│   │   │       │       ├── Role.java
│   │   │       │       ├── FileType.java
│   │   │       │       ├── StepName.java
│   │   │       │       ├── StepStatus.java
│   │   │       │       └── TransportType.java
│   │   │       ├── repository/
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   └── response/
│   │   │       ├── service/
│   │   │       │   └── agent/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── security/
│   │   │       └── util/
│   │   ├── resources/
│   │   │   ├── application.yaml
│   │   │   ├── application-local.yaml
│   │   │   ├── db/migration/
│   │   │   ├── templates/
│   │   │   │   ├── layout/
│   │   │   │   ├── auth/
│   │   │   │   ├── admin/
│   │   │   │   ├── project/
│   │   │   │   └── wizard/
│   │   │   └── static/
│   │   │       ├── css/
│   │   │       └── js/
│   └── test/
│       └── java/
├── docker/
│   └── harness/
│       ├── Dockerfile
│       ├── config/
│       └── data/
├── output/
├── docker-compose.yml
├── build.gradle
├── .env.example
├── .env
├── README.md
└── .gitignore'
);

-- Insert task template for orchestrator project
INSERT INTO task_templates (name, description, content, file_structure_template_id) VALUES (
    'AI Orchestrator Multi-Agent System',
    'Spring Boot приложение для оркестрации мультиагентной разработки с Spring AI, MCP Context7, PostgreSQL и Docker',
    '# ТЕХНИЧЕСКОЕ ЗАДАНИЕ: Spring AI Multi-Agent Orchestrator Service

## Технологический стек
- Java 21 LTS, Spring Boot 3.4+
- Spring AI с OpenAI API совместимостью (AnyModel API)
- PostgreSQL 17, Spring Data JPA, Flyway
- Spring Security 6 (BCrypt, форм-аутентификация)
- Thymeleaf + AdminLTE 3.2 + WebJars
- Docker Compose (PostgreSQL + DeepSeek Harness)
- MCP Context7 интеграция для документации и правил кодирования

## Архитектура агентов
1. **ARCHITECT** (cc/claude-sonnet-4-6): Анализ задачи, спецификация, контракты, DTO, проверка/создание файловой структуры
2. **WORKER** (ag/gemini-3.7-flash-high): Реализация кода по спецификации
3. **TESTER** (ag/gemini-3.7-flash-high): JUnit 5 + Mockito тесты
4. **HELPER** (ag/gemini-3.6-flash-high): Docker, Gradle, конфигурация

## Функциональные требования
- Авторизация через Spring Security
- Создание администратора через CommandLineRunner (app.admin.user.name/password)
- 3-шаговый мастер создания проекта (выбор шаблона ТЗ → редактирование в WYSIWYG → финализация)
- Управление MCP-серверами правил (Context7 и кастомные)
- Генерация кода агентами с сохранением истории в PostgreSQL
- Архивация сгенерированных проектов в ZIP
- Администрирование пользователей

## Требования к генерации
- Все файлы с маркерами [FILE: path]
- Strict Java 21 syntax
- Clean Architecture
- JPA entities строго в пакете ru.sergalas.orchestrator.entity
- Enums в ru.sergalas.orchestrator.entity.enums

## Окружение
- База данных: PostgreSQL 17 в Docker
- Harness: Изолированный контейнер на Node.js 22
- Приложение: Локальный запуск на Java 21 (./gradlew bootRun)
- Конфигурация через .env файл
',
    (SELECT id FROM file_structure_templates WHERE name = 'Clean Architecture Java 21 Spring Boot')
);