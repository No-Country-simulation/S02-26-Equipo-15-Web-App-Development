# NoCountry Admin Panel

Panel de monitoreo administrativo para trazabilidad del funnel y pagos, construido con:

- React + Vite + TypeScript
- TailwindCSS
- Componentes estilo shadcn/ui
- React Router
- React Query
- Recharts
- TanStack Table
- Axios

## Endpoints consumidos

Solo se usan endpoints read-only existentes del backend:

- `GET /api/admin/sessions`
- `GET /api/admin/sessions/{eventId}`
- `GET /api/admin/events`
- `GET /api/admin/metrics`

## Variables de entorno

Crear `frontend/admin/.env`:

```bash
cp .env.example .env
```

Configurar:

```bash
VITE_API_URL=http://localhost:8080
```

- `VITE_API_URL`: base URL del backend.

## Ejecucion local

```bash
cd frontend/admin
npm install
npm run dev
```

Abrir: `http://localhost:5173`

## Build de produccion

```bash
cd frontend/admin
npm run build
npm run preview
```

## Deploy en Vercel

- Root Directory del proyecto en Vercel: `frontend/admin`
- Build Command: `npm run build`
- Output Directory: `dist`
- Este directorio incluye `vercel.json` con rewrites para SPA (`/admin` y `/admin/*` -> `/index.html`), evitando 404 al entrar por URL directa.

## Credenciales login admin

- El login valida contra `GET /api/admin/health` usando HTTP Basic.
- Usar el mismo usuario/password configurado en backend (`ADMIN_USER` y `ADMIN_PASS`).
- En local, si backend corre con profile `local|dev` y no seteaste vars, los defaults son:
  - Usuario: `admin`
  - Password: `admin123`

## Notas funcionales

- El endpoint `GET /api/admin/sessions/{eventId}` incluye `integrations` (fuente: `integrations_log`) para mostrar estado real de GA4 y Meta.
- El trace view combina `tracking_session`, `tracking_event`, `orders` e integraciones; cuando falta data de webhook, infiere un paso desde `payloadJson` de `purchase`.
- El dashboard usa estado de negocio canonico: `SUCCESS`, `PENDING`, `FAILED`, `UNKNOWN`.
