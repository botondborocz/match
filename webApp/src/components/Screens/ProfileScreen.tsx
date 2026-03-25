import { useState, useEffect } from 'react';
import {
    Wrench, Globe, Palette, Settings as SettingsIcon, LogOut,
    ChevronDown, ChevronRight, Check,
    Map
} from 'lucide-react';
import { fetchUserProfile, clearProfileCache, UserProfile } from '../../services/UserService.ts'; // Adjust path
import './ProfileScreen.css';
import { useTheme } from '../../theme/ThemeContext';
import { SharedRes } from '../../shared/SharedRes.ts';
import { useTranslation } from 'react-i18next';
import i18n from '../../i18n.tsx';
import { SERVER_IP } from '../../constants.ts';

interface ProfileScreenProps {
    onLogout: () => void;
}

export default function ProfileScreen({ onLogout }: ProfileScreenProps) {
    const { t } = useTranslation();
    // --- Data State ---
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // --- Dropdown State ---
    const [openDropdown, setOpenDropdown] = useState<'language' | 'theme' | null>(null);
    const [language, setLanguage] = useState(t('english'));
    const { theme, setTheme } = useTheme();

    const getThemeDisplayText = () => {
        if (theme === 'light') return t('light');
        if (theme === 'dark') return t('dark');
        return t('system_default');
    };

    // Detect if the user is on iOS
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) ||
        (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

    // 👇 1. Add Snackbar state and handler
    const [showToast, setShowToast] = useState(false);

    const handleResetMapChoice = () => {
        localStorage.removeItem('preferred_map_app'); // Clear the preference
        setShowToast(true); // Show the snackbar

        // Hide it automatically after 3 seconds
        setTimeout(() => {
            setShowToast(false);
        }, 3000);
    };

    // Fetch data exactly once when the screen loads
    useEffect(() => {
        fetchUserProfile()
            .then((data) => {
                setProfile(data);
                if (data.preferredLanguage) {
                    handleSelectLanguage(data.preferredLanguage);
                } else {
                    // 2. If no DB preference, check the System Language!
                    // navigator.language returns things like "hu-HU" or "en-US"
                    const systemLang = navigator.language.toLowerCase();

                    // If their system is Hungarian, set 'hu'. Otherwise, fallback to 'en'.
                    if (systemLang.startsWith('hu')) {
                        handleSelectLanguage('hu');
                    } else {
                        handleSelectLanguage('en');
                    }
                }
                setIsLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load profile data.");
                setIsLoading(false);
            });
    }, []);

    const handleLogoutClick = () => {
        clearProfileCache(); // Wipe memory cache
        onLogout();          // Call App.tsx logout (wipes localStorage)
    };

    const toggleDropdown = (menu: 'language' | 'theme') => {
        setOpenDropdown(openDropdown === menu ? null : menu);
    };

    const handleSelectLanguage = async (lang: string, save_to_db: boolean = true) => {
        const newLang = lang === 'en' ? 'en' : 'hu';
        i18n.changeLanguage(newLang);
        setLanguage(newLang === 'en' ? 'English' : 'Magyar');
        setOpenDropdown(null);
        if (!save_to_db) return; // Skip DB call if we're just syncing with profile data
        // 2. Save to Database in the background
        try {
            const token = localStorage.getItem('auth_token');
            if (!token) return; // If they aren't logged in, skip the DB call

            // Note: Make sure this URL matches your Ktor routing! 
            // If your route block is inside /api/users, use `${SERVER_IP}/api/users/language`
            const response = await fetch(`${SERVER_IP}/api/users/language`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ language: newLang })
            });

            if (!response.ok) {
                console.error("Failed to sync language to database:", await response.text());
                // Optional: Show a tiny error toast to the user here
            } else {
                console.log("Language successfully saved to database!");
            }

        } catch (error) {
            console.error("Network error while saving language:", error);
        }
    };

    const handleSelectTheme = (thm: 'light' | 'dark' | 'system') => {
        setTheme(thm);
        setOpenDropdown(null);
    };

    // Helper to get initials (e.g., "player_2" -> "P2", "János Doe" -> "JD")
    const getInitials = (name: string) => {
        const parts = name.split(/[ _-]/); // Split by space, underscore, or dash
        if (parts.length >= 2) {
            return (parts[0][0] + parts[1][0]).toUpperCase();
        }
        return name.substring(0, 2).toUpperCase();
    };

    if (isLoading) {
        return <div className="profile-container flex items-center justify-center pt-20"><p className="text-muted">Betöltés...</p></div>;
    }

    if (error || !profile) {
        return <div className="profile-container flex items-center justify-center pt-20"><p className="text-red-500">{error}</p></div>;
    }

    return (
        <div className="profile-container pb-24">
            {/* --- TOP SECTION: AVATAR & INFO (Animates immediately) --- */}
            <div className="profile-header card-surface animate-slide-up">
                <div className="avatar-wrapper">
                    <div className="avatar-gradient-ring">
                        <div className="avatar-inner">{getInitials(profile.name)}</div>
                    </div>
                </div>

                <div className="header-info">
                    <h1 className="username">{profile.name}</h1>
                    <div className="badges-row">
                        <span className="elo-badge">ELO {profile.elo}</span>
                        <span className="member-since">Win Rate: {profile.winRate}</span>
                    </div>
                </div>
            </div>

            {/* --- SECTION: MY EQUIPMENT (Delayed by 0.1s) --- */}
            <div className="content-section animate-slide-up delay-1">
                <div className="section-title">
                    <Wrench size={22} className="text-muted" />
                    <h2>{t('my_gear')}</h2>
                </div>

                <div className="item-list equipment-grid">
                    {/* Blade */}
                    <div className="item-card">
                        <div className="icon-box">
                            <span role="img" aria-label="paddle" className="text-xl">🏓</span>
                        </div>
                        <div className="item-info">
                            <span className="item-label">{t('blade')}</span>
                            <p className="item-value">Butterfly Viscaria</p>
                        </div>
                    </div>

                    {/* Forehand Rubber */}
                    <div className="item-card">
                        <div className="icon-box">
                            <div className="rubber-dot red"></div>
                        </div>
                        <div className="item-info">
                            <span className="item-label">{t('forehand')}</span>
                            <p className="item-value">Tenergy 05</p>
                        </div>
                    </div>

                    {/* Backhand Rubber */}
                    <div className="item-card">
                        <div className="icon-box">
                            <div className="rubber-dot black"></div>
                        </div>
                        <div className="item-info">
                            <span className="item-label">{t('backhand')}</span>
                            <p className="item-value">Dignics 09C</p>
                        </div>
                    </div>
                </div>
            </div>

            {/* --- SECTION: SETTINGS (Delayed by 0.2s) --- */}
            <div className="content-section mt-6 animate-slide-up delay-2">
                <div className="item-list settings-grid">

                    {/* --- LANGUAGE DROPDOWN --- */}
                    <div className={`expandable-card ${openDropdown === 'language' ? 'open' : ''}`}>
                        <div className="expandable-header" onClick={() => toggleDropdown('language')}>
                            <Globe size={22} className="text-muted ml-1 mr-4" />
                            <span className="item-title">{t('language')}</span>
                            <div className="item-action">
                                <span className="text-muted">{language}</span>
                                <ChevronDown size={20} className="text-muted chevron-icon" />
                            </div>
                        </div>

                        {/* The Animated Content */}
                        <div className="expandable-content-wrapper">
                            <div className="expandable-content">
                                <div className="options-list">
                                    <button
                                        className={`option-btn ${language === 'Magyar' ? 'selected' : ''}`}
                                        onClick={() => handleSelectLanguage('hu')}
                                    >
                                        {t('hungarian')}
                                    </button>
                                    <button
                                        className={`option-btn ${language === 'English' ? 'selected' : ''}`}
                                        onClick={() => handleSelectLanguage('en')}
                                    >
                                        {t('english')}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* --- THEME DROPDOWN --- */}
                    <div className={`expandable-card ${openDropdown === 'theme' ? 'open' : ''}`}>
                        <div className="expandable-header" onClick={() => toggleDropdown('theme')}>
                            <Palette size={22} className="text-muted ml-1 mr-4" />
                            <span className="item-title">{t('theme')}</span>
                            <div className="item-action">
                                {/* Show the dynamic display text */}
                                <span className="text-muted">{getThemeDisplayText()}</span>
                                <ChevronDown size={20} className="text-muted chevron-icon" />
                            </div>
                        </div>

                        <div className="expandable-content-wrapper">
                            <div className="expandable-content">
                                <div className="options-list">
                                    <button
                                        className={`option-btn ${theme === 'light' ? 'selected' : ''}`}
                                        onClick={() => { setTheme('light'); setOpenDropdown(null); }}
                                    >
                                        {t('light')}
                                    </button>
                                    <button
                                        className={`option-btn ${theme === 'dark' ? 'selected' : ''}`}
                                        onClick={() => { setTheme('dark'); setOpenDropdown(null); }}
                                    >
                                        {t('dark')}
                                    </button>
                                    <button
                                        className={`option-btn ${theme === 'system' ? 'selected' : ''}`}
                                        onClick={() => { setTheme('system'); setOpenDropdown(null); }}
                                    >
                                        {t('system_default')}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* --- RESET MAP PREFERENCE (iOS Only) --- */}
                    {isIOS && (
                        <div className="item-card clickable" onClick={handleResetMapChoice}>
                            <Map size={22} className="text-muted ml-1 mr-4" />
                            <span className="item-title">{t('reset_map_preference', 'Reset Default Map App')}</span>
                            <div className="item-action">
                                <ChevronRight size={20} className="text-muted" />
                            </div>
                        </div>
                    )}

                    {/* Account Settings */}
                    <div className="item-card clickable">
                        <SettingsIcon size={22} className="text-muted ml-1 mr-4" />
                        <span className="item-title">Account Settings</span>
                        <div className="item-action">
                            <ChevronRight size={20} className="text-muted" />
                        </div>
                    </div>

                </div>
                {/* --- SNACKBAR / TOAST NOTIFICATION --- */}
                {showToast && (
                    <div
                        className="fixed bottom-24 left-1/2 transform -translate-x-1/2 bg-green-500 text-white px-5 py-3 rounded-full shadow-lg flex items-center gap-2 z-50"
                        style={{ animation: 'fadeInOut 3s ease-in-out' }}
                    >
                        <Check size={18} />
                        <span className="font-medium text-sm">
                            {t('map_reset_success', 'Map preference reset!')}
                        </span>
                    </div>
                )}
            </div>

            {/* --- LOGOUT BUTTON (Delayed by 0.3s) --- */}
            <div className="content-section mt-8 desktop-logout-container animate-slide-up delay-3">
                <button className="logout-btn" onClick={handleLogoutClick}>
                    <LogOut size={20} />
                    <span>{t('logout')}</span>
                </button>
            </div>

        </div>
    );
}