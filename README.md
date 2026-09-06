# Spring AI Multi-Agent Orchestrator

Production-ready веб-сервис на **Java 21 LTS** и **Spring Boot 3.4** для автоматизированной мультиагентной оркестрации генерации микросервисных приложений через единый API AnyModel и изоляционную среду DeepSeek Harness.

---

## 🏗 Архитектура и стек технологий

- **Язык & Фреймворк**: Java 21 LTS, Spring Boot 3.4.1
- **Интеграция ИИ**: Spring AI 1.0.0-M5 (OpenAI Chat Client abstraction)
- **База данных**: PostgreSQL 17 + JSONB + Flyway Migrations
- **Изолированная среда генерации**: DeepSeek Harness (Node.js 22, pnpm, MCP-Client)
- **UI / Frontend**: Thymeleaf + AdminLTE 3 + WebJars (Bootstrap 5, Select2, EasyMDE Markdown Editor)
- **Безопасность**: Spring Security 6 (Form Login, BCrypt, RBAC)

---

## ⚙️ Настройка окружения

1. Скопируйте шаблон переменных окружения:
   ```bash
   cp .env.example .env
   ```

2. Откройте `.env` и задайте актуальный ключ доступа API:
   ```env
   ANYMODEL_API_KEY=your_real_anymodel_key_here
   ANYMODEL_BASE_URL=https://anymodel.org/v1
   ```

---

## 🚀 Запуск через Docker Compose (PostgreSQL + Harness)

1. Запустите инфраструктурные контейнеры PostgreSQL и DeepSeek Harness:
   ```bash
   docker compose up -d --build
   ```

2. Проверьте статус запущенных контейнеров:
   ```bash
   docker compose ps
   ```

---

## 💻 Локальный запуск приложения (Spring Boot)

1. Убедитесь, что PostgreSQL доступен на порту `5432` (запущен через Docker Compose).
2. Запустите Spring Boot приложение с помощью Gradle Wrapper:
   ```bash
   ./gradlew bootRun
   ```

3. После запуска откройте браузер:
   - **URL**: `http://localhost:8080`
   - **Логин по умолчанию**: `admin`
   - **Пароль по умолчанию**: `admin123` (или значение `APP_ADMIN_USER_PASSWORD` из `.env`)

---

## 🧪 Запуск тестов

Для запуска модульных и интеграционных тестов с проверкой каскадного разрешения параметров, HITL-пайплайна и парсинга архивов: