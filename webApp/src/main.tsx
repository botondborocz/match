console.log("DEBUG ENV:", import.meta.env);
console.log(import.meta.env.VITE_GOOGLE_OAUTH_CLIENT_ID)

import React from 'react';
import ReactDOM from 'react-dom/client';
import App  from './App.tsx';
import { ThemeProvider } from './theme/ThemeContext';

import './i18n';
import { GoogleOAuthProvider } from '@react-oauth/google';

const clientId = import.meta.env.VITE_GOOGLE_OAUTH_CLIENT_ID || "";
console.log("Client ID Check:", import.meta.env.VITE_GOOGLE_OAUTH_CLIENT_ID);

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('Failed to find the root element');

ReactDOM.createRoot(rootElement).render(
  <ThemeProvider>
    <GoogleOAuthProvider clientId={clientId}>
      <React.StrictMode>
        <App />
      </React.StrictMode>
    </GoogleOAuthProvider>
  </ThemeProvider>
);