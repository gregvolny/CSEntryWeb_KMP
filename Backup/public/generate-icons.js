/**
 * PWA Icon Generator Script
 * 
 * Run this script to generate PNG icons from the SVG source.
 * Requires: npm install sharp
 * 
 * Usage: node generate-icons.js
 */

const fs = require('fs');
const path = require('path');

// Check if sharp is available
let sharp;
try {
    sharp = require('sharp');
} catch (e) {
    console.log('Sharp not installed. Install with: npm install sharp');
    console.log('Alternatively, use an online tool to convert icon.svg to PNG files.');
    process.exit(1);
}

const sizes = [
    { name: 'favicon-16x16.png', size: 16 },
    { name: 'favicon-32x32.png', size: 32 },
    { name: 'badge-72x72.png', size: 72 },
    { name: 'apple-touch-icon.png', size: 180 },
    { name: 'icon-192x192.png', size: 192 },
    { name: 'icon-512x512.png', size: 512 },
];

const iconsDir = path.join(__dirname, 'icons');
const svgPath = path.join(iconsDir, 'icon.svg');

async function generateIcons() {
    console.log('Generating PWA icons...\n');
    
    if (!fs.existsSync(svgPath)) {
        console.error('Error: icon.svg not found in icons directory');
        process.exit(1);
    }
    
    for (const { name, size } of sizes) {
        const outputPath = path.join(iconsDir, name);
        
        try {
            await sharp(svgPath)
                .resize(size, size)
                .png()
                .toFile(outputPath);
            
            console.log(`✓ Generated ${name} (${size}x${size})`);
        } catch (error) {
            console.error(`✗ Failed to generate ${name}: ${error.message}`);
        }
    }
    
    console.log('\nDone! Icons generated in public/icons/');
}

generateIcons().catch(console.error);
