/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        background: '#050816',
        surface: '#0b1020',
        card: '#10192d',
        muted: '#8b97b5',
        border: '#23324f',
        accent: '#22d3ee',
        success: '#10b981',
        danger: '#ef4444',
        warning: '#f59e0b',
        info: '#3b82f6',
      },
      boxShadow: {
        panel: '0 10px 40px -20px rgba(2, 8, 23, 0.8)',
      },
      backgroundImage: {
        grid:
          'radial-gradient(circle at 1px 1px, rgba(139,151,181,0.15) 1px, transparent 0)',
      },
    },
  },
  plugins: [],
}
