/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#fbf7ff',
          100: '#f5eeff',
          200: '#ebdcff',
          300: '#dbc0fe',
          400: '#c297fc',
          500: '#a767f7',
          600: '#8e3ef0',
          700: '#7929d9',
          800: '#6422b4',
          900: '#521d92',
          950: '#340b68',
        },
        dark: {
          900: '#0B0D13',
          800: '#12151F',
          700: '#1A1E2C',
          600: '#262B3D',
          500: '#383E54',
        }
      },
    },
  },
  plugins: [],
}
