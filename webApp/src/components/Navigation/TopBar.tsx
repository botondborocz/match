import React from 'react';
import { Menu } from 'lucide-react';
import '../../App.css';

interface TopBarProps {
  onMenuClick: () => void;
  title: string;
}

const TopBar: React.FC<TopBarProps> = ({ onMenuClick, title }) => {
  return (
    <header className="mobile-topbar">
      <button onClick={onMenuClick} className="icon-btn">
        <Menu size={24} color="#FFFFFF" />
      </button>

      <h1 className="topbar-title">{title}</h1>

      <div className="topbar-avatar">
        <span>JD</span>
      </div>
    </header>
  );
};

export default TopBar;