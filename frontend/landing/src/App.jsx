import { useEffect, useState } from 'react';
import './App.css';
import { ensureEventId, readAttribution } from './lib/attribution';
import { redirectToStripe } from './lib/stripe';

function App() {
  const [loading, setLoading] = useState(false);
  const [trackingData, setTrackingData] = useState(null);
  const [eventId, setEventId] = useState(null);

  useEffect(() => {
    // Capture attribution on first paint and ensure event id exists.
    const attribution = readAttribution();
    setTrackingData(attribution);

    const localEventId = ensureEventId();
    setEventId(localEventId);

    const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080';
    const payload = {
      eventType: 'landing_view',
      utm_source: attribution?.utm_source,
      utm_medium: attribution?.utm_medium,
      utm_campaign: attribution?.utm_campaign,
      utm_term: attribution?.utm_term,
      utm_content: attribution?.utm_content,
      gclid: attribution?.gclid,
      fbclid: attribution?.fbclid,
      landing_path: window.location.pathname,
    };

    if (apiBase) {
      fetch(`${apiBase}/api/track`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
        .then((res) => res.json().catch(() => ({})))
        .then((data) => {
          if (data?.eventId) setEventId(data.eventId);
        })
        .catch((err) => {
          console.warn('No se pudo enviar landing_view al backend:', err);
        });
    }
  }, []);

  const trackAndGoToStripe = async () => {
    if (loading) return;
    setLoading(true);

    const currentAttribution = trackingData || readAttribution();
    const currentEventId = eventId || ensureEventId();

    try {
      redirectToStripe(currentAttribution, currentEventId);
    } catch (error) {
      console.error('Error redirigiendo a Stripe', error);
      alert('No pudimos iniciar el checkout. Intenta nuevamente en unos segundos.');
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <header className="hero">
        <p className="eyebrow">Lanza en EE.UU. sin fricción</p>
        <h1>Incorporación, impuestos y contabilidad en EE.UU., todo en un solo lugar</h1>
        <p className="lead">
          Ayudamos a emprendedores y empresas a crear y gestionar su negocio en Estados Unidos,
          incluyendo incorporación, tax filing y bookkeeping, de forma simple y 100% online.
        </p>
        <div className="actions">
          <button className="cta" onClick={trackAndGoToStripe} disabled={loading}>
            {loading ? 'Creando checkout...' : 'Comenzar ahora'}
          </button>
          <span className="cta-note">Sin costos ocultos · soporte en español</span>
        </div>
        <div className="pill-row">
          <span className="pill">LLC o C-Corp en Delaware</span>
          <span className="pill">EIN + agente registrado</span>
          <span className="pill">Tax filing y bookkeeping</span>
        </div>
      </header>

      <section className="grid">
        <div className="card highlight">
          <h2>Todo en un panel</h2>
          <p>
            Documentos, pagos de impuestos y reportes financieros en un tablero único. Integrado con
            Stripe, Mercury y los bancos más usados por startups.
          </p>
          <ul>
            <li>Checklist guiado paso a paso.</li>
            <li>Recordatorios de declaraciones y vencimientos.</li>
            <li>Soporte humano en horario extendido.</li>
          </ul>
        </div>
        <div className="card">
          <h3>¿Qué incluye?</h3>
          <ul>
            <li><strong>Incorporación:</strong> constitución, bylaws/operating agreement y EIN.</li>
            <li><strong>Impuestos:</strong> preparación y presentación federal/estatal.</li>
            <li><strong>Contabilidad:</strong> conciliación mensual y reportes listos para inversores.</li>
          </ul>
        </div>
        <div className="card">
          <h3>Para quién</h3>
          <ul>
            <li>Founders globales que venden en EE.UU.</li>
            <li>Agencias y SaaS que necesitan pasarela en dólares.</li>
            <li>Equipos remotos que buscan cobertura fiscal.</li>
          </ul>
        </div>
      </section>
    </div>
  );
}

export default App;
