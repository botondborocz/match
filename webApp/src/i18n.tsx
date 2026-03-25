import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import enTranslations from './locales/en.json';
import huTranslations from './locales/hu.json';

const getInitialLanguage = () => {
  // Check if they previously manually selected a language
  const savedLang = localStorage.getItem('app_language');
  if (savedLang) return savedLang;

  // If no saved language, use the system default!
  const systemLang = navigator.language.toLowerCase();
  console.log(systemLang);
  if (systemLang.startsWith('hu')) {
      return 'hu';
  } else {
      return 'en';
  }
};

i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: enTranslations },
      hu: { translation: huTranslations }
    },
    lng: getInitialLanguage(),
    fallbackLng: "en", // If a string is missing in HU, use EN
    interpolation: {
      escapeValue: false 
    }
  });

export default i18n;