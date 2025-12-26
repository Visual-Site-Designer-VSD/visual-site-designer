import { PageDefinition, ComponentInstance } from '../types/builder';
import { Page } from '../types/site';
import JSZip from 'jszip';

/**
 * StaticExportService - Exports site pages as deployable static HTML/CSS/JS files
 */

interface ExportOptions {
  includeCss: boolean;
  includeJs: boolean;
  minify: boolean;
  singlePage: boolean;
}

interface ExportedFile {
  name: string;
  content: string;
  type: 'html' | 'css' | 'js' | 'json';
}

interface SiteExportData {
  pages: Array<{
    page: Page;
    definition: PageDefinition;
  }>;
  siteName: string;
}

/**
 * Generate CSS for a component based on its styles
 */
function generateComponentCSS(component: ComponentInstance, selector: string): string {
  const styles = component.styles || {};
  const cssProperties: string[] = [];

  for (const [key, value] of Object.entries(styles)) {
    if (value !== undefined && value !== null && value !== '') {
      // Convert camelCase to kebab-case
      const cssKey = key.replace(/([A-Z])/g, '-$1').toLowerCase();
      cssProperties.push(`  ${cssKey}: ${value};`);
    }
  }

  if (cssProperties.length === 0) return '';

  return `${selector} {\n${cssProperties.join('\n')}\n}`;
}

/**
 * Generate HTML for a component
 */
function generateComponentHTML(component: ComponentInstance, isPreview: boolean = false): string {
  const { componentId, props, styles, instanceId, children } = component;
  const id = `component-${instanceId}`;
  const className = `component component-${componentId.toLowerCase()}`;

  // Generate inline styles if not using external CSS
  const inlineStyle = Object.entries(styles || {})
    .map(([key, value]) => {
      const cssKey = key.replace(/([A-Z])/g, '-$1').toLowerCase();
      return `${cssKey}: ${value}`;
    })
    .join('; ');

  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  switch (componentId) {
    case 'Label':
      const labelTag = props.variant === 'h1' ? 'h1' :
                       props.variant === 'h2' ? 'h2' :
                       props.variant === 'h3' ? 'h3' :
                       props.variant === 'h4' ? 'h4' :
                       props.variant === 'h5' ? 'h5' :
                       props.variant === 'h6' ? 'h6' :
                       props.variant === 'paragraph' ? 'p' : 'span';
      return `<${labelTag} id="${id}" class="${className}"${styleAttr}>${escapeHtml(props.text || '')}</${labelTag}>`;

    case 'Button':
      const btnVariant = props.variant || 'primary';
      const btnSize = props.size || 'medium';
      const btnClass = `${className} btn btn-${btnVariant} btn-${btnSize}`;
      const disabled = props.disabled ? ' disabled' : '';
      const onClick = props.events?.onClick?.action?.config?.url
        ? ` onclick="window.location.href='${props.events.onClick.action.config.url}'"`
        : '';
      return `<button id="${id}" class="${btnClass}"${styleAttr}${disabled}${onClick}>${escapeHtml(props.text || 'Click Me')}</button>`;

    case 'Image':
      const imgSrc = props.src || props.url || 'https://via.placeholder.com/300x200';
      const imgAlt = props.alt || '';
      return `<img id="${id}" class="${className}" src="${imgSrc}" alt="${escapeHtml(imgAlt)}"${styleAttr} />`;

    case 'Textbox':
      return `<div id="${id}" class="${className} textbox"${styleAttr}>${props.content || props.text || ''}</div>`;

    case 'Container':
    case 'ScrollableContainer':
      const childrenHtml = (children || [])
        .map(child => generateComponentHTML(child, isPreview))
        .join('\n');
      const containerClass = componentId === 'ScrollableContainer'
        ? `${className} scrollable-container`
        : className;
      return `<div id="${id}" class="${containerClass}"${styleAttr}>\n${childrenHtml}\n</div>`;

    case 'Navbar':
    case 'NavbarDefault':
    case 'NavbarCentered':
    case 'NavbarMinimal':
    case 'NavbarDark':
    case 'NavbarGlass':
    case 'NavbarSticky':
      return generateNavbarHTML(component, id, className, styleAttr);

    default:
      // Generic fallback
      if (children && children.length > 0) {
        const childrenHtml = children.map(child => generateComponentHTML(child, isPreview)).join('\n');
        return `<div id="${id}" class="${className}"${styleAttr}>\n${childrenHtml}\n</div>`;
      }
      return `<div id="${id}" class="${className}"${styleAttr}>${escapeHtml(props.text || props.content || componentId)}</div>`;
  }
}

/**
 * Generate Navbar HTML
 */
function generateNavbarHTML(component: ComponentInstance, id: string, className: string, styleAttr: string): string {
  const { props } = component;
  const brandName = props.brandName || props.brand || 'Brand';
  const navItems = props.navItems || props.items || [];
  const variant = props.variant || 'default';

  const navItemsHtml = navItems.map((item: any) => {
    const activeClass = item.active ? ' active' : '';
    const href = item.href || '#';
    return `      <li class="nav-item${activeClass}"><a class="nav-link" href="${href}">${escapeHtml(item.label || item.text || '')}</a></li>`;
  }).join('\n');

  return `<nav id="${id}" class="${className} navbar navbar-${variant}"${styleAttr}>
  <div class="navbar-container">
    <a class="navbar-brand" href="/">${escapeHtml(brandName)}</a>
    <button class="navbar-toggle" aria-label="Toggle navigation">
      <span class="navbar-toggle-icon"></span>
    </button>
    <ul class="navbar-nav">
${navItemsHtml}
    </ul>
  </div>
</nav>`;
}

/**
 * Escape HTML special characters
 */
function escapeHtml(text: string): string {
  const htmlEntities: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  };
  return text.replace(/[&<>"']/g, char => htmlEntities[char]);
}

/**
 * Generate the base CSS for all components
 */
function generateBaseCSS(): string {
  return `/* Base Styles - Generated by Visual Site Builder */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  line-height: 1.6;
  color: #333;
}

.component {
  position: relative;
}

/* Button Styles */
.btn {
  display: inline-block;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  vertical-align: middle;
  user-select: none;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 14px;
  line-height: 1.5;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
}

.btn-primary { background-color: #007bff; color: white; }
.btn-primary:hover { background-color: #0056b3; }
.btn-secondary { background-color: #6c757d; color: white; }
.btn-secondary:hover { background-color: #545b62; }
.btn-success { background-color: #28a745; color: white; }
.btn-success:hover { background-color: #218838; }
.btn-danger { background-color: #dc3545; color: white; }
.btn-danger:hover { background-color: #c82333; }
.btn-warning { background-color: #ffc107; color: #212529; }
.btn-warning:hover { background-color: #e0a800; }
.btn-outline { background-color: transparent; color: #007bff; border: 2px solid #007bff; }
.btn-outline:hover { background-color: #007bff; color: white; }

.btn-small { padding: 6px 12px; font-size: 13px; }
.btn-medium { padding: 8px 16px; font-size: 14px; }
.btn-large { padding: 12px 24px; font-size: 16px; }

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* Navbar Styles */
.navbar {
  display: flex;
  align-items: center;
  padding: 1rem;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.navbar-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.navbar-brand {
  font-size: 1.25rem;
  font-weight: 700;
  color: #333;
  text-decoration: none;
}

.navbar-nav {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 0.5rem;
}

.nav-item {
  margin: 0;
}

.nav-link {
  display: block;
  padding: 0.5rem 1rem;
  color: #666;
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.2s;
}

.nav-link:hover {
  color: #007bff;
  background-color: #f8f9fa;
}

.nav-item.active .nav-link {
  color: #007bff;
  font-weight: 600;
}

.navbar-toggle {
  display: none;
  background: none;
  border: none;
  padding: 0.5rem;
  cursor: pointer;
}

.navbar-dark {
  background-color: #1a1a2e;
}

.navbar-dark .navbar-brand,
.navbar-dark .nav-link {
  color: #fff;
}

.navbar-dark .nav-link:hover {
  background-color: rgba(255,255,255,0.1);
}

/* Container Styles */
.component-container {
  display: flex;
  flex-direction: column;
}

.scrollable-container {
  overflow: auto;
}

/* Image Styles */
.component-image {
  max-width: 100%;
  height: auto;
}

/* Textbox Styles */
.textbox {
  padding: 1rem;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .navbar-nav {
    display: none;
    flex-direction: column;
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    background: inherit;
    padding: 1rem;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  }

  .navbar-nav.active {
    display: flex;
  }

  .navbar-toggle {
    display: block;
  }

  .navbar-toggle-icon {
    display: block;
    width: 24px;
    height: 2px;
    background-color: #333;
    position: relative;
  }

  .navbar-toggle-icon::before,
  .navbar-toggle-icon::after {
    content: '';
    position: absolute;
    width: 100%;
    height: 2px;
    background-color: #333;
    left: 0;
  }

  .navbar-toggle-icon::before { top: -6px; }
  .navbar-toggle-icon::after { top: 6px; }

  .navbar-dark .navbar-toggle-icon,
  .navbar-dark .navbar-toggle-icon::before,
  .navbar-dark .navbar-toggle-icon::after {
    background-color: #fff;
  }
}
`;
}

/**
 * Generate JavaScript for interactive features
 */
function generateBaseJS(): string {
  return `// Site JavaScript - Generated by Visual Site Builder

document.addEventListener('DOMContentLoaded', function() {
  // Mobile navbar toggle
  const navbarToggles = document.querySelectorAll('.navbar-toggle');
  navbarToggles.forEach(function(toggle) {
    toggle.addEventListener('click', function() {
      const navbar = this.closest('.navbar');
      const navList = navbar.querySelector('.navbar-nav');
      if (navList) {
        navList.classList.toggle('active');
      }
    });
  });

  // Smooth scroll for anchor links
  document.querySelectorAll('a[href^="#"]').forEach(function(anchor) {
    anchor.addEventListener('click', function(e) {
      const href = this.getAttribute('href');
      if (href && href !== '#') {
        e.preventDefault();
        const target = document.querySelector(href);
        if (target) {
          target.scrollIntoView({ behavior: 'smooth' });
        }
      }
    });
  });

  console.log('Site loaded successfully!');
});
`;
}

/**
 * Generate a complete HTML page
 */
function generateHTMLPage(
  pageDefinition: PageDefinition,
  pageMeta: Page,
  allPages: Page[],
  options: ExportOptions
): string {
  const { pageName, components, globalStyles } = pageDefinition;

  // Generate component HTML
  const componentsHtml = components
    .map(comp => generateComponentHTML(comp, false))
    .join('\n\n');

  // Generate custom CSS
  let customCss = '';
  if (globalStyles?.customCSS) {
    customCss = `\n<style>\n${globalStyles.customCSS}\n</style>`;
  }

  // Generate navigation for multi-page sites
  const hasMultiplePages = allPages.length > 1;

  // Determine CSS and JS includes
  const cssInclude = options.includeCss
    ? '<link rel="stylesheet" href="css/styles.css">'
    : `<style>\n${generateBaseCSS()}\n</style>`;
  const jsInclude = options.includeJs
    ? '<script src="js/main.js" defer></script>'
    : `<script>\n${generateBaseJS()}\n</script>`;

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(pageName)}</title>
  ${cssInclude}${customCss}
</head>
<body>
  <main class="page-content">
${componentsHtml}
  </main>
  ${jsInclude}
</body>
</html>`;
}

/**
 * Export a single page as HTML
 */
export function exportSinglePage(
  pageDefinition: PageDefinition,
  options: Partial<ExportOptions> = {}
): string {
  const defaultOptions: ExportOptions = {
    includeCss: false,
    includeJs: false,
    minify: false,
    singlePage: true,
  };
  const mergedOptions = { ...defaultOptions, ...options };

  const pageMeta: Page = {
    id: 1,
    siteId: 0,
    pageName: pageDefinition.pageName,
    pageSlug: pageDefinition.pageName.toLowerCase().replace(/\s+/g, '-'),
    pageType: 'standard',
    routePath: '/',
    displayOrder: 0,
    isPublished: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  return generateHTMLPage(pageDefinition, pageMeta, [pageMeta], mergedOptions);
}

/**
 * Export entire site as a ZIP file
 */
export async function exportSiteAsZip(
  siteData: SiteExportData,
  options: Partial<ExportOptions> = {}
): Promise<Blob> {
  const defaultOptions: ExportOptions = {
    includeCss: true,
    includeJs: true,
    minify: false,
    singlePage: false,
  };
  const mergedOptions = { ...defaultOptions, ...options };

  const zip = new JSZip();

  // Add CSS file
  if (mergedOptions.includeCss) {
    zip.file('css/styles.css', generateBaseCSS());
  }

  // Add JS file
  if (mergedOptions.includeJs) {
    zip.file('js/main.js', generateBaseJS());
  }

  // Generate HTML for each page
  const allPages = siteData.pages.map(p => p.page);

  for (const { page, definition } of siteData.pages) {
    const html = generateHTMLPage(definition, page, allPages, mergedOptions);
    const fileName = page.routePath === '/'
      ? 'index.html'
      : `${page.pageSlug}.html`;
    zip.file(fileName, html);
  }

  // Add a simple README
  const readme = `# ${siteData.siteName}

This site was exported from Visual Site Builder.

## Files
${siteData.pages.map(p => `- ${p.page.routePath === '/' ? 'index.html' : p.page.pageSlug + '.html'} - ${p.page.pageName}`).join('\n')}

## Deployment
Upload all files to your web server or hosting service.

### Quick Deploy Options:
1. **Netlify Drop**: Drag and drop this folder to netlify.com/drop
2. **GitHub Pages**: Push to a GitHub repository and enable Pages
3. **Vercel**: Import to vercel.com
4. **Any Web Server**: Upload via FTP/SFTP

## Generated
Date: ${new Date().toISOString()}
Tool: Visual Site Builder
`;
  zip.file('README.md', readme);

  // Generate the zip file
  return await zip.generateAsync({ type: 'blob' });
}

/**
 * Export site from localStorage (demo mode)
 */
export async function exportDemoSite(siteName: string = 'My Site'): Promise<Blob> {
  const savedPages = JSON.parse(localStorage.getItem('builder_saved_pages') || '{}');

  const pages: SiteExportData['pages'] = Object.entries(savedPages).map(
    ([slug, definition]: [string, any], index) => ({
      page: {
        id: index + 1,
        siteId: 0,
        pageName: definition.pageName || slug,
        pageSlug: slug,
        pageType: slug === 'home' ? 'homepage' : 'standard',
        routePath: slug === 'home' ? '/' : `/${slug}`,
        displayOrder: index,
        isPublished: false,
        createdAt: definition.savedAt || new Date().toISOString(),
        updatedAt: definition.savedAt || new Date().toISOString(),
      } as Page,
      definition: definition as PageDefinition,
    })
  );

  // Ensure we have at least one page
  if (pages.length === 0) {
    throw new Error('No pages to export. Please create and save at least one page.');
  }

  return exportSiteAsZip({ pages, siteName });
}

/**
 * Download a blob as a file
 */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/**
 * Download HTML string as a file
 */
export function downloadHTML(html: string, filename: string): void {
  const blob = new Blob([html], { type: 'text/html' });
  downloadBlob(blob, filename);
}
