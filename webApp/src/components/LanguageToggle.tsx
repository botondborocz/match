import { useTranslation } from 'react-i18next';

export const LanguageToggle = () => {
  const { i18n } = useTranslation();

  const toggleLanguage = () => {
    // If currently English, switch to Hungarian, otherwise switch to English
    const newLang = i18n.language === 'en' ? 'hu' : 'en';
    i18n.changeLanguage(newLang);
  };

  return (
    <button 
      onClick={toggleLanguage}
      style={{ padding: '8px 16px', borderRadius: '8px', cursor: 'pointer' }}
    >
      {i18n.language === 'en' ? '🇭🇺 Váltás Magyarra' : '🇬🇧 Switch to English'}
    </button>
  );
};