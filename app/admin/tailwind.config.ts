import type { Config } from 'tailwindcss'

export default {
  darkMode: 'class',
  content: [
    './app/components/**/*.{js,vue,ts}',
    './app/layouts/**/*.vue',
    './app/pages/**/*.vue',
    './app/plugins/**/*.{js,ts}',
    './app/app.vue',
  ],
  theme: {
    extend: {
      colors: {
        mi: {
          orange: '#FF6900',
        },
      },
      fontFamily: {
        sans: ['MiSans', 'system-ui', '-apple-system', 'sans-serif'],
      },
    },
  },
} satisfies Config
