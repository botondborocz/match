import fs from 'fs/promises';
import path from 'path';
import xml2js from 'xml2js';
import { fileURLToPath } from 'url';

// 1. Setup paths
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Target the root composeResources folder instead of a specific XML file
const composeResourcesPath = path.resolve(__dirname, '../../../ttproject/shared/src/commonMain/composeResources');
const reactLocalesPath = path.resolve(__dirname, '../src/locales');

interface StringEntry {
    $: { name: string };
    _?: string;
}

interface ParsedXml {
    resources?: {
        string?: StringEntry[];
    };
}

async function syncI18n() {
    try {
        await fs.mkdir(reactLocalesPath, { recursive: true });

        // 2. Find all directories inside composeResources
        const items = await fs.readdir(composeResourcesPath, { withFileTypes: true });
        
        // Filter for folders starting with "values" (e.g., "values", "values-hu")
        const valueDirs = items.filter(item => item.isDirectory() && item.name.startsWith('values'));

        // 3. Loop through each language folder
        for (const dir of valueDirs) {
            // Determine language code: 'values' is default (en), 'values-hu' is 'hu'
            const langCode = dir.name === 'values' ? 'en' : dir.name.replace('values-', '');
            const xmlPath = path.join(composeResourcesPath, dir.name, 'string.xml');
            
            try {
                // Read and Parse XML
                const data = await fs.readFile(xmlPath, 'utf8');
                const parser = new xml2js.Parser();
                const result: ParsedXml = await parser.parseStringPromise(data);
                
                // Convert to JSON
                const resources = result.resources?.string || [];
                const jsonOutput: Record<string, string> = {};

                resources.forEach(entry => {
                    if (entry.$ && entry.$.name) {
                        jsonOutput[entry.$.name] = entry._ || "";
                    }
                });

                // Save JSON
                const jsonPath = path.join(reactLocalesPath, `${langCode}.json`);
                await fs.writeFile(jsonPath, JSON.stringify(jsonOutput, null, 2));
                
                console.log(`✅ Synced: ${dir.name}/string.xml -> ${langCode}.json`);
            } catch (e) {
                // Skip if a values folder exists but has no strings.xml (like colors only)
                console.log(`⚠️ Skipped ${dir.name} (No string.xml found)`);
            }
        }
    } catch (err) {
        console.error("❌ Failed to sync i18n:", err);
        process.exit(1);
    }
}

syncI18n();