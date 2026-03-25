import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Ensure this path still points to your SharedRes.kt
const kotlinFilePath = path.resolve(__dirname, '../../../ttproject/shared/src/commonMain/kotlin/org/ttproject/SharedRes.kt');

// 👇 Notice we are outputting a .css file now!
const reactThemePath = path.resolve(__dirname, '../src/theme/SharedTheme.css');

async function syncTheme() {
    try {
        await fs.mkdir(path.dirname(reactThemePath), { recursive: true });
        const data = await fs.readFile(kotlinFilePath, 'utf8');
        const regex = /val\s+([a-zA-Z0-9_]+)\s*=\s*"([^"]+)"/g;
        let match;
        
        let cssContent = `/* AUTO-GENERATED FILE. DO NOT EDIT DIRECTLY. */\n/* Synced from SharedRes.kt */\n\n@theme {\n`;
        let count = 0;

        while ((match = regex.exec(data)) !== null) {
            const colorName = match[1]; 
            const hexValue = match[2];  
            // Tailwind v4 strictly requires custom colors to start with --color-
            cssContent += `  --color-${colorName}: ${hexValue};\n`;
            count++;
        }

        cssContent += `}\n`;

        if (count === 0) {
            console.log("⚠️ No colors found. Check your Regex or file path.");
            return;
        }

        await fs.writeFile(reactThemePath, cssContent);
        console.log(`✅ Successfully synced ${count} colors to SharedTheme.css!`);

    } catch (err) {
        console.error("❌ Failed to sync colors:", err);
        process.exit(1);
    }
}

syncTheme();