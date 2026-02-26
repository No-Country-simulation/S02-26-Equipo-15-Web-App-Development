/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        background: '#070F2B',
        surface: '#0A1438',
        card: '#111F4A',
        muted: '#7CB6FF',
        border: '#254E97',
        accent: '#2D8CFF',
        success: '#10b981',
        danger: '#ef4444',
        warning: '#f59e0b',
        info: '#3b82f6',
      },
      boxShadow: {
        panel: '0 10px 40px -20px rgba(45, 140, 255, 0.35)',
      },
      backgroundImage: {
        grid:
          'radial-gradient(circle at 1px 1px, rgba(124,182,255,0.16) 1px, transparent 0)',
      },
    },
  },
  plugins: [],
}
