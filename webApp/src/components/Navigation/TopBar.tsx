import React, { useMemo } from 'react';
import { Menu } from 'lucide-react';
import '../../App.css';
import { useTheme } from '../../theme/ThemeContext';

interface TopBarProps {
  onMenuClick: () => void;
  title: string;
}

const TopBar: React.FC<TopBarProps> = ({ onMenuClick, title }) => {
  const { theme } = useTheme();

  const isDarkMode = useMemo(() => {
    if (theme === 'dark') return true;
    if (theme === 'light') return false;
    // If it's 'system', ask the browser/phone what the OS is currently set to!
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }, [theme]);
    
  return (
    <header className="mobile-topbar">
      <button onClick={onMenuClick} className="icon-btn">
        <Menu size={24} color="var(--text-primary)" />
      </button>

      <h1 className="topbar-title">
        {isDarkMode ? (
          <img src="/assets/match_logo_long.png" alt="Match Logo" className="logo-image" />)
          : (
            <img src="/assets/match_logo_long_dark.png" alt="Match Logo" className="logo-image" />
          )
        }
      </h1>

      <div className="topbar-avatar">
        <span>JD</span>
      </div>
    </header>
  );
};

export default TopBar;