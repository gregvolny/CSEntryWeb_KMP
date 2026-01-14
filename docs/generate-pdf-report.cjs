/**
 * PDF Report Generator for CSPro Logic Functions & Action Invoker Report
 * 
 * This script converts the Markdown report to PDF format.
 * 
 * Usage:
 *   npm install puppeteer marked
 *   node generate-pdf-report.js
 * 
 * Or use VS Code's built-in Markdown PDF extension:
 *   1. Install "Markdown PDF" extension (yzane.markdown-pdf)
 *   2. Open CSPro_Logic_Functions_ActionInvoker_Report.md
 *   3. Press Ctrl+Shift+P → "Markdown PDF: Export (pdf)"
 */

const fs = require('fs');
const path = require('path');

async function generatePDF() {
    try {
        // Check if puppeteer is available
        const puppeteer = require('puppeteer');
        const { marked } = require('marked');
        
        const mdPath = path.join(__dirname, 'CSPro_Logic_Functions_ActionInvoker_Report.md');
        const pdfPath = path.join(__dirname, 'CSPro_Logic_Functions_ActionInvoker_Report.pdf');
        
        console.log('Reading Markdown file...');
        const markdown = fs.readFileSync(mdPath, 'utf-8');
        
        console.log('Converting to HTML...');
        const html = marked(markdown);
        
        const styledHtml = `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>CSPro Logic Functions & Action Invoker API Reference</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            font-size: 11px;
        }
        h1 {
            color: #2c3e50;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
        }
        h2 {
            color: #2980b9;
            border-bottom: 1px solid #bdc3c7;
            padding-bottom: 5px;
            margin-top: 30px;
        }
        h3 {
            color: #34495e;
            margin-top: 20px;
        }
        table {
            border-collapse: collapse;
            width: 100%;
            margin: 15px 0;
            font-size: 10px;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 6px;
            text-align: left;
        }
        th {
            background-color: #3498db;
            color: white;
        }
        tr:nth-child(even) {
            background-color: #f2f2f2;
        }
        code {
            background-color: #f4f4f4;
            padding: 2px 5px;
            border-radius: 3px;
            font-family: 'Consolas', 'Courier New', monospace;
            font-size: 10px;
        }
        pre {
            background-color: #2d2d2d;
            color: #f8f8f2;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
            font-size: 9px;
        }
        pre code {
            background-color: transparent;
            color: inherit;
        }
        blockquote {
            border-left: 4px solid #3498db;
            padding-left: 15px;
            color: #666;
            margin: 15px 0;
        }
        .page-break {
            page-break-after: always;
        }
        @media print {
            h1, h2 {
                page-break-after: avoid;
            }
            table, pre {
                page-break-inside: avoid;
            }
        }
    </style>
</head>
<body>
${html}
</body>
</html>
        `;
        
        console.log('Launching browser...');
        const browser = await puppeteer.launch({
            headless: 'new'
        });
        
        const page = await browser.newPage();
        await page.setContent(styledHtml, { waitUntil: 'networkidle0' });
        
        console.log('Generating PDF...');
        await page.pdf({
            path: pdfPath,
            format: 'A4',
            margin: {
                top: '20mm',
                right: '15mm',
                bottom: '20mm',
                left: '15mm'
            },
            printBackground: true,
            displayHeaderFooter: true,
            headerTemplate: '<div style="font-size: 9px; width: 100%; text-align: center; color: #666;">CSPro Logic Functions & Action Invoker API Reference</div>',
            footerTemplate: '<div style="font-size: 9px; width: 100%; text-align: center; color: #666;">Page <span class="pageNumber"></span> of <span class="totalPages"></span></div>'
        });
        
        await browser.close();
        
        console.log(`PDF generated successfully: ${pdfPath}`);
        
    } catch (error) {
        if (error.code === 'MODULE_NOT_FOUND') {
            console.log('\n=== PDF Generation Instructions ===\n');
            console.log('Option 1: Install dependencies and run this script');
            console.log('  npm install puppeteer marked');
            console.log('  node generate-pdf-report.js\n');
            console.log('Option 2: Use VS Code Markdown PDF Extension');
            console.log('  1. Install "Markdown PDF" extension (yzane.markdown-pdf)');
            console.log('  2. Open CSPro_Logic_Functions_ActionInvoker_Report.md');
            console.log('  3. Press Ctrl+Shift+P → "Markdown PDF: Export (pdf)"\n');
            console.log('Option 3: Use online converter');
            console.log('  - https://www.markdowntopdf.com/');
            console.log('  - https://md2pdf.netlify.app/\n');
        } else {
            console.error('Error generating PDF:', error);
        }
    }
}

generatePDF();
