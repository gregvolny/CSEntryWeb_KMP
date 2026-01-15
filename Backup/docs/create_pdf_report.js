/**
 * Markdown to PDF Converter
 * Converts the porting report to PDF format
 * 
 * Usage: node create_pdf_report.js
 * 
 * Prerequisites:
 *   npm install markdown-pdf puppeteer
 *   Or for simpler approach:
 *   npm install md-to-pdf
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Read the markdown file
const mdPath = path.join(__dirname, 'ANDROID_TO_KMP_PORTING_REPORT.md');
const pdfPath = path.join(__dirname, 'ANDROID_TO_KMP_PORTING_REPORT.pdf');

// Check if md-to-pdf is available
try {
    const { mdToPdf } = await import('md-to-pdf');
    
    (async () => {
        console.log('Converting Markdown to PDF...');
        
        const pdf = await mdToPdf(
            { path: mdPath },
            { 
                dest: pdfPath,
                pdf_options: {
                    format: 'A4',
                    margin: {
                        top: '20mm',
                        right: '20mm',
                        bottom: '20mm',
                        left: '20mm'
                    }
                },
                css: `
                    body { 
                        font-family: 'Segoe UI', Arial, sans-serif; 
                        line-height: 1.6;
                        color: #333;
                    }
                    h1 { 
                        color: #1a5490; 
                        border-bottom: 2px solid #1a5490;
                        padding-bottom: 10px;
                    }
                    h2 { 
                        color: #2c7bb6; 
                        border-bottom: 1px solid #ddd;
                        padding-bottom: 5px;
                        margin-top: 30px;
                    }
                    h3 { 
                        color: #444;
                        margin-top: 20px;
                    }
                    table { 
                        border-collapse: collapse; 
                        width: 100%;
                        margin: 15px 0;
                    }
                    th, td { 
                        border: 1px solid #ddd; 
                        padding: 8px 12px; 
                        text-align: left;
                    }
                    th { 
                        background-color: #f5f5f5; 
                        font-weight: bold;
                    }
                    tr:nth-child(even) {
                        background-color: #fafafa;
                    }
                    code { 
                        background: #f4f4f4; 
                        padding: 2px 6px; 
                        border-radius: 3px;
                        font-family: 'Consolas', monospace;
                    }
                    pre { 
                        background: #f8f8f8; 
                        padding: 15px; 
                        border-radius: 5px;
                        border: 1px solid #e0e0e0;
                        overflow-x: auto;
                    }
                    pre code {
                        background: none;
                        padding: 0;
                    }
                    blockquote {
                        border-left: 4px solid #1a5490;
                        margin: 15px 0;
                        padding: 10px 20px;
                        background: #f9f9f9;
                    }
                `
            }
        );
        
        if (pdf) {
            fs.writeFileSync(pdfPath, pdf.content);
            console.log(`PDF created: ${pdfPath}`);
        }
    })();
    
} catch (e) {
    // Fallback: Create HTML version that can be printed to PDF
    console.log('md-to-pdf error:', e.message);
    console.log('Creating HTML version instead...');
    console.log('To install: npm install md-to-pdf');
    
    let marked = null;
    try {
        const markedModule = await import('marked');
        marked = markedModule.marked;
    } catch (e2) {
        // marked not available
    }
    
    if (marked) {
        const markdown = fs.readFileSync(mdPath, 'utf8');
        const html = `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>CSPro Android to KMP Porting Report</title>
    <style>
        body { 
            font-family: 'Segoe UI', Arial, sans-serif; 
            line-height: 1.6;
            color: #333;
            max-width: 900px;
            margin: 0 auto;
            padding: 40px;
        }
        h1 { color: #1a5490; border-bottom: 2px solid #1a5490; padding-bottom: 10px; }
        h2 { color: #2c7bb6; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-top: 30px; }
        h3 { color: #444; margin-top: 20px; }
        table { border-collapse: collapse; width: 100%; margin: 15px 0; }
        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
        th { background-color: #f5f5f5; font-weight: bold; }
        tr:nth-child(even) { background-color: #fafafa; }
        code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: 'Consolas', monospace; }
        pre { background: #f8f8f8; padding: 15px; border-radius: 5px; border: 1px solid #e0e0e0; overflow-x: auto; }
        pre code { background: none; padding: 0; }
        @media print {
            body { max-width: none; margin: 0; padding: 20px; }
        }
    </style>
</head>
<body>
${marked(markdown)}
</body>
</html>`;
        
        const htmlPath = path.join(__dirname, 'ANDROID_TO_KMP_PORTING_REPORT.html');
        fs.writeFileSync(htmlPath, html);
        console.log(`HTML created: ${htmlPath}`);
        console.log('Open in browser and print to PDF (Ctrl+P)');
    } else {
        console.log('No markdown parser found.');
        console.log('To convert to PDF:');
        console.log('1. npm install md-to-pdf');
        console.log('2. node create_pdf_report.js');
        console.log('');
        console.log('Or use online tools like:');
        console.log('- https://md2pdf.netlify.app/');
        console.log('- https://www.markdowntopdf.com/');
    }
}
