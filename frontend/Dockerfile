# Multi-stage Dockerfile para frontend React (Vite)

# 1. Etapa de compilación
FROM node:22-alpine AS builder
WORKDIR /app

# Copiar manifiestos e instalar dependencias
COPY package*.json ./
RUN npm ci

# Copiar código fuente y compilar
COPY . .
RUN npm run build

# 2. Etapa de producción con Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Copiar archivos estáticos compilados y configuración de Nginx
COPY --from=builder /app/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=5s --retries=3 --start-period=5s \
  CMD wget -qO- http://127.0.0.1/ > /dev/null || exit 1


CMD ["nginx", "-g", "daemon off;"]

