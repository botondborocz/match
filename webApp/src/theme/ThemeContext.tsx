import React, { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark' | 'system';

interface ThemeContextType {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // 1. Initialize state from localStorage, default to 'system'
  const [theme, setTheme] = useState<Theme>(() => {
    return (localStorage.getItem('app_theme') as Theme) || 'system';
  });

  // 2. The magic effect that actually changes the CSS classes
  useEffect(() => {
    const root = window.document.documentElement;
    
    // Remove old classes
    root.classList.remove('light', 'dark');

    // Handle "System" setting
    if (theme === 'system') {
      const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      root.classList.add(systemTheme);
    } else {
      // Handle explicit "Light" or "Dark"
      root.classList.add(theme);
    }

    // Save to local storage
    localStorage.setItem('app_theme', theme);
  }, [theme]);

  // 3. Optional: Listen for system theme changes in real-time
  useEffect(() => {
    if (theme === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handleChange = () => {
        const root = window.document.documentElement;
        root.classList.remove('light', 'dark');
        root.classList.add(mediaQuery.matches ? 'dark' : 'light');
      };
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within a ThemeProvider');
  return context;
};