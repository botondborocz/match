import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import enTranslations from './locales/en.json';
import huTranslations from './locales/hu.json';

i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: enTranslations },
      hu: { translation: huTranslations }
    },
    lng: "en", // Default language on startup
    fallbackLng: "en", // If a string is missing in HU, use EN
    interpolation: {
      escapeValue: false 
    }
  });

export default i18n;