# Landing (React + Vite)

Landing con tracking client-side para GA4 y Meta Pixel.

## Requisitos
- Node 18+
- npm 9+

## Instalacion y ejecucion local
```bash
cd frontend/landing
npm install
npm run dev
```
La app queda en `http://localhost:5173`.

## Variables de entorno
Crea un `.env` local desde `.env.example` (no se versiona en git):
```bash
cp .env.example .env
```

Valores esperados en `.env.example`:
```env
VITE_GA_MEASUREMENT_ID=G-XXXXXXXXXX
VITE_META_PIXEL_ID=123456789012345
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PAYMENT_LINK=https://buy.stripe.com/your_payment_link
```

En produccion, `VITE_API_URL` debe apuntar al backend Railway:
```env
VITE_API_URL=https://s02-26-equipo-15-web-app-development-desarrollo.up.railway.app
```

Configurar en Vercel:
1. Ir a Project Settings -> Environment Variables.
2. Crear `VITE_API_URL` para los entornos necesarios (Preview/Production).
3. Re-deploy para que Vite tome la variable al momento del build.

Importante: en Vite solo se exponen al cliente las variables con prefijo `VITE_`.

Si cambias valores de `.env`, reinicia `npm run dev` para que Vite recargue `import.meta.env`.

## Tracking (GA4 + Meta Pixel)
- Inicializacion en `src/main.jsx`: `initTracking()` corre antes de `trackPageView()`.
- `page_view` se envia manualmente desde `trackPageView()` (fuente unica para GA4 y Meta).
- GA4 se inicializa con `send_page_view: false` para evitar auto `page_view`.
- Meta Pixel se inicializa con `fbq('init', PIXEL_ID)` sin `fbq('track', 'PageView')` en init.

### Verificacion en navegador
- DevTools -> Network: filtra `collect?v=2` para GA4.
- DevTools -> Network: filtra `fbevents.js` y `tr?id=` para Meta Pixel.
- DevTools -> Console (F12): valida en runtime `import.meta.env.VITE_GA_MEASUREMENT_ID` y `import.meta.env.VITE_META_PIXEL_ID`.

Nota: `import.meta.env.*` no funciona directo en PowerShell/terminal; se valida en el navegador.

## Eventos implementados
- `trackPageView(path?)` -> GA4 `page_view`, Meta `PageView`.
- `trackCTA({ eventId?, label? })` -> GA4 `click_cta`, Meta `ClickCTA`.
- `trackBeginCheckout({ value?, currency?, eventId? })` -> GA4 `begin_checkout`, Meta `InitiateCheckout`.

## Notas
- Si faltan `VITE_GA_MEASUREMENT_ID` o `VITE_META_PIXEL_ID`, no se cargan scripts ni se envian eventos.
- `.env`, `node_modules` y `dist` estan ignorados por git.
