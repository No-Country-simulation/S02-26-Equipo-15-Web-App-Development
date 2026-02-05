import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import './index.css';
import { initTracking } from './lib/tracking/initTracking';
import { trackPageView } from './lib/tracking/events';

initTracking();
trackPageView();

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
