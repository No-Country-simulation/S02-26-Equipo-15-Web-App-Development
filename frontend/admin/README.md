# NoCountry Admin Panel

Panel de observabilidad para auditar el funnel por `eventId`, revisar conversiones y seguir el estado de integraciones.

## Deploy

- https://s02-26-equipo-15-web-app-admin.vercel.app/admin/login

## Configuracion

Crear `.env` desde `.env.example`:

```bash
cp .env.example .env
```

Variable requerida:

```bash
VITE_API_URL=http://localhost:8080
```

- `VITE_API_URL`: URL base del backend (Railway en cloud, localhost en desarrollo).

## Login y autenticacion

- El login usa credenciales reales del backend (HTTP Basic).
- Valida acceso contra `GET /api/admin/health`.
- Si la validacion es correcta, usa el header `Authorization: Basic ...` en las llamadas a `/api/admin/**`.
- Credenciales: mismas de backend (`ADMIN_USER`, `ADMIN_PASS`).

## Metricas que muestra

- Sesiones (`tracking_session`) y detalle por `eventId`.
- Eventos (`tracking_event`) por rango/tipo.
- Ordenes y revenue (`orders`).
- Distribucion de `orders.business_status` (`SUCCESS`, `PENDING`, `FAILED`, `UNKNOWN`).
- Estado de integraciones desde `Integrations log`.

## Correr local

```bash
cd frontend/admin
npm install
npm run dev
```

- URL local: `http://localhost:5173` (o puerto disponible de Vite).

## Build

```bash
cd frontend/admin
npm run build
npm run preview
```
