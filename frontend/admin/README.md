# TrackSure Admin

Panel operativo para monitorear trazabilidad de funnel, estado de ordenes y salud de integraciones por `eventId`.

## Que muestra el dashboard

- KPIs de sesiones, eventos y ordenes.
- Revenue acumulado y distribucion de `business_status`.
- Estado de integraciones (`GA4_MP`, `META_CAPI`) desde `integrations_log`.
- Trazabilidad completa por sesion (`eventId`).

## Filtros disponibles

### Sessions

- Busqueda por `eventId`
- Rango de fechas (`from`, `to`)
- Filtro por `business_status`
- Paginacion (`Anterior` / `Siguiente`)

### Events

- Busqueda por `eventId`
- Rango de fechas (`from`, `to`)
- Filtro por `eventType`
- Paginacion

## Como validar trazabilidad end-to-end

1. Abrir `Sessions` y buscar un `eventId`.
2. Entrar al detalle de sesion.
3. Verificar secuencia de eventos (`landing_view -> click_cta -> begin_checkout -> purchase`).
4. Confirmar orden en `orders` con `business_status`.
5. Confirmar logs de integraciones con estado y HTTP status.

## Configuracion

Crear `.env` desde `.env.example`:

```bash
cp .env.example .env
```

Variable requerida:

```bash
VITE_API_URL=http://localhost:8080
```

Para nube:

- `VITE_API_URL` debe apuntar al backend de Render.

## Autenticacion

- Login con credenciales del backend (`ADMIN_USER` / `ADMIN_PASS`).
- Validacion inicial contra `GET /api/admin/health`.
- Luego usa `Authorization: Basic ...` para `/api/admin/**`.

## Correr local

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
- URL actual: `https://s02-26-equipo-15-web-app-admin.vercel.app/admin/login`
- Variable clave en Vercel: `VITE_API_URL=https://s02-26-equipo-15-web-app-development.onrender.com`
