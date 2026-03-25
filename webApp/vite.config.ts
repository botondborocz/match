import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'url';
import path from 'path';

const __dirname = path.dirname(fileURLToPath(import.meta.url))

console.log("VITE SEARCHING FOR ENV IN:", path.resolve(__dirname));

export default defineConfig({
  root: '.',
  plugins: [react()],
  envDir: __dirname,
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  server: { port: 1111 },
});