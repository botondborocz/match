import type { Config } from 'tailwindcss';

// Import the auto-generated theme file from our script!
// (Double check this path matches where syncTheme.js saves the file)
import { SharedTheme } from './src/theme/SharedTheme'; 

export default {
  // 1. Tell Tailwind where to look for your classes
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  
  // 2. Enable class-based dark mode (so you can toggle it, or rely on OS)
  darkMode: 'class', 
  
  theme: {
    extend: {
      // 3. Dump EVERY Kotlin color directly into Tailwind!
      colors: {
        ...SharedTheme,
      }
    },
  },
  plugins: [],
} satisfies Config;