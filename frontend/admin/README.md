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
VITE_API_URL=http://localhost:8080
VITE_ADMIN_DEMO_PASSWORD=nocountry-admin
```

- `VITE_API_URL`: base URL del backend.
- `VITE_ADMIN_DEMO_PASSWORD`: password demo para login local (opcional, default `nocountry-admin`).

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

## Credenciales demo login

- Usuario: no requerido
- Password: `nocountry-admin` (o el valor en `VITE_ADMIN_DEMO_PASSWORD`)

## Notas funcionales

- El backend actual no expone `integrations_log` en los endpoints `/api/admin/*`, por lo que estado GA4/Meta en tablas y trace se muestra como `N/A` o inferido.
- El trace view construye timeline con `tracking_session`, `tracking_event`, `orders` y pasos inferidos desde `payloadJson` cuando existe.
