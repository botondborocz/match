import React from 'react';
import { Home, Map, Brain, Flame, User as UserIcon, Settings, X, LucideIcon } from 'lucide-react';
import { SharedRes } from '../../shared/SharedRes';
import '../../App.css';
import { useTranslation } from 'react-i18next';
import { LanguageToggle } from '../LanguageToggle';

interface SidebarProps {
  activeTab: string;
  onTabChange: (tabId: string) => void;
  isOpen: boolean;       
  onClose: () => void;   
  isMobile: boolean;     
}

interface NavItem {
  id: string;
  label: string;
  icon: LucideIcon;
  isPro?: boolean;
  notification?: boolean;
}

const Sidebar: React.FC<SidebarProps> = ({ activeTab, onTabChange, isOpen, onClose, isMobile }) => {
  const { t } = useTranslation();
  
  const menuItems: NavItem[] = [
    { id: 'home', label: t('home'), icon: Home },
    { id: 'map', label: t('map'), icon: Map },
    { id: 'coach', label: t('ai_coach'), icon: Brain, notification: true },
    { id: 'match', label: t('match'), icon: Flame },
    { id: 'profile', label: t('profile'), icon: UserIcon },
  ];

  const sidebarClass = isMobile 
    ? `sidebar mobile-drawer ${isOpen ? 'open' : ''}`
    : 'sidebar desktop';

  // Find the index of the currently active tab
  const activeIndex = Math.max(0, menuItems.findIndex(item => item.id === activeTab));

  return (
    <>
      {isMobile && isOpen && (
        <div className="mobile-overlay" onClick={onClose} />
      )}

      <aside className={sidebarClass}>
        {isMobile && (
          <button className="close-btn" onClick={onClose}>
            <X size={24} color="#9CA3AF" />
          </button>
        )}

        {/* Logo Area */}
        <div className="logo-section">
          <div className="logo-icon"></div>
          <span className="logo-text">SpinSync</span>
        </div>

        {/* Navigation Items */}
        <nav className="nav-column">
          {/* THE ANIMATED SLIDING LINE */}
          <div 
            className="active-indicator" 
            style={{ 
              transform: `translateY(calc(${activeIndex} * 56px + 12px))` 
            }} 
          />

          {menuItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <div
                key={item.id}
                onClick={() => {
                  onTabChange(item.id);
                  if (isMobile) onClose();
                }}
                className={`nav-item ${isActive ? 'active' : ''}`}
              >
                <item.icon 
                  size={20} 
                  color={isActive ? "#FF5A36" : "#9CA3AF"} 
                  /* Optional: animate the icon color smoothly */
                  // style={{ transition: 'color 0.2s' }}
                />
                <span className="nav-text">{item.label}</span>
                {item.isPro && <span className="pro-badge">{SharedRes.strings.pro}</span>}
                {item.notification && <div className="notification-dot"></div>}
              </div>
            );
          })}
        </nav>
        
        <div style={{ padding: '0 16px' }}>
          <LanguageToggle />
        </div>
        
        {/* Footer */}
        <div className="user-footer">
          <div className="avatar">JD</div>
          <div className="user-info">
            <div className="user-name">János Doe</div>
            <div className="user-role">ELO 1380</div>
          </div>
          <Settings size={18} color="#9CA3AF" />
        </div>
      </aside>
    </>
  );
};

export default Sidebar;