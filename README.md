# AI Agent Orchestrator

Мультиагентная система оркестрации генерации кода и архитектурного проектирования на базе Spring Boot 3.4 (Java 21 LTS), Spring AI и DeepSeek Harness.

## 🚀 Архитектура и цепочка агентов

1. **ARCHITECT** (`cc/claude-sonnet-4-6`): Формирует техническую спецификацию. При необходимости задает уточняющие вопросы с префиксом `CLARIFICATION_NEEDED:`.
2. **WORKER** (`ag/gemini-3.7-flash-high`): Генерирует основной производственный код на основе спецификации.
3. **TESTER** (`ag/gemini-3.7-flash-high`): Пишет юнит и интеграционные тесты (JUnit 5 + Mockito).
4. **HELPER** (`ag/gemini-3.6-flash-high`): Генерирует сборочные файлы, манифесты деплоя и `docker-compose.yml`.

---

## 🛠️ Требования

* **Java**: 21 LTS
* **Node.js**: 22 (внутри Docker)
* **Docker & Docker Compose**
* **Gradle**: 8.x (или встроенный `./gradlew`)

---

## ⚙️ Переменные окружения

Создайте файл `.env` на основе шаблона `.env.example`: