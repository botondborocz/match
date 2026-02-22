import { useState, useEffect } from 'react';
import Sidebar from './components/Navigation/SideBar';
import TopBar from './components/Navigation/TopBar';
import { SharedRes } from './shared/SharedRes.ts';
import './App.css';

function App() {
  const [activeTab, setActiveTab] = useState('home');
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  // Handle Resize
  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth <= 768;
      setIsMobile(mobile);
      if (!mobile) setIsMenuOpen(false); // Reset menu state if resizing to desktop
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Helper to get title for TopBar
  const getPageTitle = () => {
    const titles: Record<string, string> = {
      home: SharedRes.strings.home,
      map: SharedRes.strings.map,
      coach: SharedRes.strings.aiCoach,
      match: SharedRes.strings.match,
      profile: SharedRes.strings.profile
    };
    return titles[activeTab] || SharedRes.strings.appName;
  };

  return (
    <div className="app-container">
      {/* The Sidebar is always rendered, but styles change based on isMobile.
        On Desktop: It sits relatively in the flex layout.
        On Mobile: It becomes a fixed drawer (hidden unless isOpen is true).
      */}
      <Sidebar 
        activeTab={activeTab} 
        onTabChange={setActiveTab}
        isMobile={isMobile}
        isOpen={isMenuOpen}
        onClose={() => setIsMenuOpen(false)}
      />

      {/* TopBar is only visible on Mobile */}
      {isMobile && (
        <TopBar 
          onMenuClick={() => setIsMenuOpen(true)} 
          title={getPageTitle()}
        />
      )}

      {/* Main Content Area */}
      <main className="main-content">
        <div style={{ padding: '20px', textAlign: 'center' }}>
           {/* Placeholder Content */}
           <h2>Welcome to {getPageTitle()}</h2>
           <p>This is the content area.</p>
        </div>
      </main>
    </div>
  );
}

export default App;