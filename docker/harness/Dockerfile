FROM node:22-bookworm-slim

WORKDIR /app

# Установка системных утилит и официального пакета DeepSeek Harness (@deepseek-ai/dsh) через стабильное зеркало
RUN apt-get update && apt-get install -y --no-install-recommends socat ca-certificates curl && \
    npm install -g @deepseek-ai/dsh --registry=https://registry.npmmirror.com && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Копирование пользовательских пресетов и конфигураций
COPY config/ /app/custom-config/

EXPOSE 3080

ENV PORT=3080
ENV NODE_ENV=production

# dsh web слушает на 127.0.0.1 (безопасность dsh), socat перенаправляет порт 3080 контейнера на 127.0.0.1:3081
CMD ["sh", "-c", "socat TCP-LISTEN:3080,fork,reuseaddr TCP:127.0.0.1:3081 & exec dsh web --no-open --port 3081"]