# Frontend - Sistema de Gestión de Turnos

Esqueleto técnico base para el frontend de la plataforma de gestión de turnos.

## Stack Tecnológico

- **React:** 19 (JSX)
- **Herramienta de Build:** Vite 8 / 6
- **Estilos:** Tailwind CSS 4 + CSS personalizado
- **Enrutamiento:** React Router 7
- **Manejo de Estado del Servidor:** TanStack React Query 5
- **Formularios & Validación:** React Hook Form + Zod
- **Primitivas UI & Iconos:** Radix UI + Lucide React
- **Cliente HTTP:** Fetch nativo (`src/services/api.js`)
- **Testing:** Vitest + React Testing Library

---

## Estructura de Directorios

```
frontend/
├── src/
│   ├── app/            # Providers y configuración de router
│   ├── components/     # Componentes visuales reutilizables
│   ├── layouts/        # Layouts de la aplicación (Header, Footer, Outlet)
│   ├── pages/          # Páginas y vistas principales
│   ├── features/       # Módulos del sistema (turnos, agenda, disponibilidad, clientes)
│   ├── hooks/          # Custom hooks reutilizables
│   ├── services/       # Cliente HTTP y servicios API
│   ├── schemas/        # Esquemas de validación Zod
│   ├── utils/          # Funciones de ayuda y utilidades
│   ├── styles/         # Estilos globales y configuración Tailwind
│   ├── test/           # Setup y suites de pruebas
│   ├── App.jsx         # Componente raíz
│   └── main.jsx        # Punto de entrada
├── index.html
├── vite.config.js
├── package.json
├── .env.example
└── README.md
```

---

## Requisitos Previos

- Node.js v20+ o v22+
- npm v10+

---

## Instalación y Ejecución

1. **Instalar dependencias:**
   ```bash
   npm install
   ```

2. **Configurar variables de entorno:**
   ```bash
   cp .env.example .env
   ```

3. **Iniciar servidor de desarrollo:**
   ```bash
   npm run dev
   ```

4. **Ejecutar pruebas unitarias:**
   ```bash
   npm test
   ```

5. **Compilar para producción:**
   ```bash
   npm run build
   ```

6. **Previsualizar compilación:**
   ```bash
   npm run preview
   ```

