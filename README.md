# Appointment Management System (`api-turnos`)

Modular appointment management system built with **Java 25**, **Spring Boot 4.1.x**, **PostgreSQL 17**, **Flyway**, **Docker**, and **React 19 Frontend**.

## Project Vision

This project starts as an independent appointment management system designed with a clean modular architecture (Turnos, Agenda, Disponibilidad, Clientes, Notificaciones, Auditoría).

---

## Tech Stack

### Backend
- **Java:** 25 LTS
- **Framework:** Spring Boot 4.1.x
- **Dependency Management:** Maven
- **Database:** PostgreSQL 17
- **Persistence & ORM:** Spring Data JPA / Hibernate (`ddl-auto: validate`)
- **Database Migrations:** Flyway
- **Validation:** Jakarta Bean Validation
- **Metrics & Health:** Spring Boot Actuator
- **API Documentation:** OpenAPI 3 / Swagger UI (`springdoc-openapi`)
- **Testing:** JUnit 5, Mockito, Spring Boot Test, Testcontainers PostgreSQL

### Frontend
- **React:** 19 (JSX)
- **Build Tool:** Vite
- **Styling:** Tailwind CSS 4 + Custom CSS
- **Routing:** React Router 7
- **Server State:** TanStack React Query 5
- **Forms & Validation:** React Hook Form + Zod
- **UI & Icons:** Radix UI + Lucide React
- **HTTP Client:** Fetch nativo
- **Testing:** Vitest + React Testing Library

### Containers & Orchestration
- **Docker & Docker Compose**

---

## Project Structure

```
.
├── pom.xml                                 # Backend dependencies & build
├── Dockerfile                              # Backend multi-stage Dockerfile
├── compose.yaml                            # Docker Compose (PostgreSQL 17 + API)
├── .env.example                            # Backend environment variables
├── .gitignore                              # Git exclusion rules
├── README.md                               # Project documentation
├── src/                                    # Backend source code
│   ├── main/
│   │   ├── java/com/turnos/api/
│   │   │   ├── ApiTurnosApplication.java   # Spring Boot entry point
│   │   │   ├── turno/                      # Turnos module
│   │   │   ├── agenda/                     # Agenda module
│   │   │   ├── disponibilidad/             # Disponibilidad module
│   │   │   ├── cliente/                    # Clientes module
│   │   │   ├── notificacion/               # Notificaciones module
│   │   │   ├── auditoria/                  # Auditoría module
│   │   │   └── shared/                     # Shared kernel & utilities
│   │   └── resources/
│   │       ├── application.yml             # Main Spring Boot configuration
│   │       └── db/migration/               # Flyway SQL migrations
│   └── test/
│       └── java/com/turnos/api/            # Unit & integration tests
└── frontend/                               # React 19 Frontend
    ├── index.html
    ├── vite.config.js
    ├── package.json
    ├── .env.example
    ├── README.md
    └── src/
        ├── app/                            # Providers & Router
        ├── components/                     # Reusable UI components
        ├── layouts/                        # Base Layouts
        ├── pages/                          # Main pages
        ├── features/                       # Modular business features
        ├── services/                       # API HTTP client (Fetch)
        ├── styles/                         # Tailwind CSS 4 styles
        └── test/                           # Tests (Vitest)
```

---

## Quick Start

### 1. Backend with Docker Compose
```bash
cp .env.example .env
docker compose up --build
```

### 2. Frontend Development
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

---

## Endpoints

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Schema:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator Health:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
