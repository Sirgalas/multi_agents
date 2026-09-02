-- Schema extension for File Structure Templates and Task Templates + Seeding

-- File structure templates table
CREATE TABLE file_structure_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    structure_tree TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Task templates table
CREATE TABLE task_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    content TEXT NOT NULL,
    file_structure_template_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_task_template_file_structure 
        FOREIGN KEY (file_structure_template_id) 
        REFERENCES file_structure_templates(id) ON DELETE SET NULL
);

-- Add task_template_id foreign key to projects table
ALTER TABLE projects 
ADD COLUMN task_template_id BIGINT,
ADD CONSTRAINT fk_project_task_template 
    FOREIGN KEY (task_template_id) 
    REFERENCES task_templates(id) ON DELETE SET NULL;

CREATE INDEX idx_projects_task_template_id ON projects(task_template_id);

-- Seed file structure template for Clean Architecture Java 21 Spring Boot
INSERT INTO file_structure_templates (name, description, structure_tree) VALUES (
    'Clean Architecture Java 21 Spring Boot',
    'Эталонное дерево директорий и файлов для Clean Architecture проекта с Spring Boot 3.4+, Spring AI, PostgreSQL, WebJars и Docker',
    '├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ru/sergalas/orchestrator/
│   │   │       ├── AiOrchestratorApplication.java
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
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── ProjectRepository.java
│   │   │       │   ├── TaskTemplateRepository.java
│   │   │       │   ├── FileStructureTemplateRepository.java
│   │   │       │   ├── ProjectContextRepository.java
│   │   │       │   ├── ProjectMcpServerRepository.java
│   │   │       │   └── AgentStepRepository.java
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   └── response/
│   │   │       ├── service/
│   │   │       │   ├── UserService.java
│   │   │       │   ├── ProjectService.java
│   │   │       │   ├── OrchestratorService.java
│   │   │       │   ├── McpService.java
│   │   │       │   ├── ArchiveService.java
│   │   │       │   └── agent/
│   │   │       │       ├── ArchitectAgent.java
│   │   │       │       ├── WorkerAgent.java
│   │   │       │       ├── TesterAgent.java
│   │   │       │       └── HelperAgent.java
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── SpringAiConfig.java
│   │   │       │   ├── AdminRegister.java
│   │   │       │   └── McpPresets.java
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── AdminController.java
│   │   │       │   ├── ProjectController.java
│   │   │       │   ├── ProjectWizardController.java
│   │   │       │   └── McpController.java
│   │   │       ├── security/
│   │   │       │   └── UserDetailsServiceImpl.java
│   │   │       └── util/
│   │   │           ├── Either.java
│   │   │           ├── FileParser.java
│   │   │           └── PromptBuilder.java
│   │   ├── resources/
│   │   │   ├── application.yaml
│   │   │   ├── application-local.yaml
│   │   │   ├── db/migration/
│   │   │   │   ├── V1__init_schema.sql
│   │   │   │   └── V2__task_templates_and_seed.sql
│   │   │   └── templates/
│   │   │       ├── layout/
│   │   │       │   └── main.html
│   │   │       ├── auth/
│   │   │       │   └── login.html
│   │   │       ├── admin/
│   │   │       │   ├── users.html
│   │   │       │   └── user-form.html
│   │   │       ├── project/
│   │   │       │   ├── list.html
│   │   │       │   └── view.html
│   │   │       └── wizard/
│   │   │           ├── step1-template.html
│   │   │           ├── step2-edit.html
│   │   │           └── step3-finalize.html
│   └── test/
│       └── java/
│           └── ru/sergalas/orchestrator/
├── docker/
│   └── harness/
│       ├── Dockerfile
│       └── config/
│           ├── preset.yml
│           └── agent.cordis.yml
├── output/
├── docker-compose.yml
├── build.gradle
├── settings.gradle
├── .env.example
├── .env
└── README.md'
);

-- Seed reference task template for multi-agent orchestrator service
INSERT INTO task_templates (name, description, content, file_structure_template_id) VALUES (
    'AI Orchestrator Multi-Agent System',
    'Эталонное ТЗ на разработку сервиса оркестрации мультиагентной разработки с Spring AI, MCP Context7, PostgreSQL и Docker',
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