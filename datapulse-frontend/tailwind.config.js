/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{html,ts,scss}"],
  theme: {
    extend: {
      fontFamily: {
        display: ['Inter', 'sans-serif'],
        sans: ['Inter', 'sans-serif'],
      },
      colors: {
        // TerraPulse — Curated Atelier
        background: {
          DEFAULT: '#FBF8F4', // Level 0 — base canvas (warm ivory)
          sub: '#F2EDE6',     // Level 1 — recessed sections (warm taupe)
          raised: '#FFFFFF',  // Level 2 — cards (clean white)
          accent: '#FFF3ED',  // Level 3 — featured / hero (warm peach)
        },
        primary: {
          DEFAULT: '#E8560C', // signature orange
          hover: '#D14A08',
          container: '#FFDBC8',
          'on-container': '#3A1705',
        },
        'on-primary': '#FFFFFF',
        secondary: {
          DEFAULT: '#7A5A47', // warm brown
          container: '#F2EDE6',
        },
        'text-primary': '#2C2118',   // deep espresso
        'text-secondary': '#6B5B4F', // muted taupe
        'text-tertiary': '#9C8878',
        outline: {
          DEFAULT: '#E6DDD2',   // warm divider
          variant: '#E2BFB3',   // ghost border color
        },
        success: '#4A7C59',
        warning: '#D4894B',
        danger: '#B94A3B',
        info: '#5B7A9B',
      },
      borderRadius: {
        'xl': '1rem',       // 16px — cards
        '2xl': '1.25rem',   // 20px
        '3xl': '2rem',      // 32px — hero / modal
      },
      boxShadow: {
        'atelier': '0 20px 40px -10px rgba(44, 33, 24, 0.08)',
        'atelier-sm': '0 8px 20px -8px rgba(44, 33, 24, 0.06)',
        'atelier-lg': '0 30px 60px -15px rgba(44, 33, 24, 0.12)',
        'focus': '0 0 0 4px #FFF3ED',
      },
      fontSize: {
        'display-xl': ['3.5rem', { lineHeight: '1.05', letterSpacing: '-0.02em', fontWeight: '600' }],
        'display-lg': ['2.75rem', { lineHeight: '1.1', letterSpacing: '-0.02em', fontWeight: '600' }],
        'display-md': ['2.25rem', { lineHeight: '1.15', letterSpacing: '-0.02em', fontWeight: '600' }],
        'display-sm': ['1.75rem', { lineHeight: '1.2', letterSpacing: '-0.01em', fontWeight: '600' }],
      },
      animation: {
        'ping-slow': 'ping 2s cubic-bezier(0, 0, 0.2, 1) infinite',
        'fade-in': 'fadeIn 0.4s ease-out',
        'slide-up': 'slideUp 0.4s ease-out',
        'slide-in-right': 'slideInRight 0.3s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideInRight: {
          '0%': { opacity: '0', transform: 'translateX(32px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
      },
    },
  },
  plugins: [],
};
