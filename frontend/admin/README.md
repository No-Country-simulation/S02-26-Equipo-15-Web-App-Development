# TrackSure Intelligence Dashboard

Panel administrativo de TrackSure para monitoreo operativo y trazabilidad de conversiones.

## Que muestra

- KPI de sesiones, eventos, ordenes y revenue.
- Estado de negocio de ordenes (`SUCCESS`, `PENDING`, `FAILED`, `UNKNOWN`).
- Salud de integraciones (`GA4_MP`, `META_CAPI`) basada en `integrations_log`.
- Traza detallada por `eventId` para auditoria.

## Filtros y navegacion

### Vista Sessions

- Busqueda por `eventId`
- Rango de fechas (`from`, `to`)
- Filtro por `business_status`
- Paginacion (`Anterior`, `Siguiente`)

### Vista Events

- Busqueda por `eventId`
- Rango de fechas (`from`, `to`)
- Filtro por `eventType`
- Paginacion

## Autenticacion (Basic Auth)

El dashboard usa las mismas credenciales del backend:

- `ADMIN_USER`
- `ADMIN_PASS`

Flujo:

1. Login valida contra `GET /api/admin/health`.
2. Si es correcto, las llamadas a `/api/admin/**` se envian con `Authorization: Basic ...`.

## Configuracion `VITE_API_URL`

Crear `.env` desde `.env.example`:

```bash
cp .env.example .env
```

Ejemplo local:

```bash
VITE_API_URL=http://localhost:8080
```

Ejemplo produccion (Vercel):

```bash
VITE_API_URL=https://s02-26-equipo-15-web-app-development.onrender.com
```

## Desarrollo local

```bash
cd frontend/admin
npm install
npm run dev
```

URL local: `http://localhost:5173` (o puerto libre de Vite).

## Build

```bash
cd frontend/admin
npm run build
npm run preview
```

## Deploy

- Plataforma: Vercel
- URL: `https://s02-26-equipo-15-web-app-admin.vercel.app/admin/login`
- Requisito clave: `VITE_API_URL` debe apuntar a TrackSure API en Render.
