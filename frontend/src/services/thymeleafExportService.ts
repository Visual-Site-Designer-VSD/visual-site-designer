import { PageDefinition, ComponentInstance, DataSourceConfig } from '../types/builder';
import JSZip from 'jszip';

/**
 * ThymeleafExportService - Exports site pages as Spring Boot/Thymeleaf project
 * Generates Thymeleaf templates with th:* attributes for dynamic data binding
 */

interface ThymeleafExportOptions {
  projectName: string;
  groupId: string;
  artifactId: string;
  version: string;
  springBootVersion: string;
  javaVersion: string;
}

interface PageExportData {
  pageName: string;
  path: string;
  definition: PageDefinition;
}

/**
 * Convert camelCase to kebab-case for CSS properties
 */
function camelToKebab(str: string): string {
  return str.replace(/([A-Z])/g, '-$1').toLowerCase();
}

/**
 * Generate inline style string from styles object
 */
function generateInlineStyle(styles: Record<string, any>): string {
  if (!styles || Object.keys(styles).length === 0) return '';

  const cssProperties: string[] = [];
  for (const [key, value] of Object.entries(styles)) {
    if (value !== undefined && value !== null && value !== '') {
      const cssKey = camelToKebab(key);
      cssProperties.push(`${cssKey}: ${value}`);
    }
  }

  return cssProperties.join('; ');
}

/**
 * Escape HTML special characters
 */
function escapeHtml(text: string): string {
  if (typeof text !== 'string') return String(text || '');
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
 * Convert template variables from {{variable}} to Thymeleaf ${variable} syntax
 */
function convertTemplateVariables(text: string): string {
  if (!text) return text;
  // Replace {{variable.path}} with ${variable.path}
  return text.replace(/\{\{([^}]+)\}\}/g, '${$1}');
}

/**
 * Generate Thymeleaf HTML for a component
 */
function generateThymeleafComponent(component: ComponentInstance, depth: number = 0): string {
  const { componentId, props, styles, instanceId, children } = component;
  const indent = '    '.repeat(depth);
  const id = `component-${instanceId}`;

  switch (componentId) {
    case 'Label':
      return generateThymeleafLabel(component, id, indent);
    case 'Button':
      return generateThymeleafButton(component, id, indent);
    case 'Image':
      return generateThymeleafImage(component, id, indent);
    case 'Container':
    case 'ScrollableContainer':
      return generateThymeleafContainer(component, id, indent, depth);
    case 'Repeater':
      return generateThymeleafRepeater(component, id, indent, depth);
    case 'DataList':
      return generateThymeleafDataList(component, id, indent, depth);
    case 'Navbar':
    case 'NavbarDefault':
      return generateThymeleafNavbar(component, id, indent);
    default:
      return generateThymeleafGeneric(component, id, indent, depth);
  }
}

/**
 * Generate Thymeleaf Label
 */
function generateThymeleafLabel(component: ComponentInstance, id: string, indent: string): string {
  const { props, styles, templateBindings } = component;
  const text = props.text || '';
  const variant = props.variant || 'span';

  const tagMap: Record<string, string> = {
    h1: 'h1', h2: 'h2', h3: 'h3', h4: 'h4', h5: 'h5', h6: 'h6',
    paragraph: 'p', span: 'span', label: 'label'
  };
  const tag = tagMap[variant] || 'span';

  const inlineStyle = generateInlineStyle(styles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  // Check if text contains template variables
  if (text.includes('{{')) {
    const thymeleafExpr = convertTemplateVariables(text);
    return `${indent}<${tag} id="${id}" class="component label"${styleAttr} th:text="${thymeleafExpr}">${escapeHtml(text)}</${tag}>`;
  }

  // Check for template bindings
  if (templateBindings?.text) {
    const thymeleafExpr = convertTemplateVariables(templateBindings.text);
    return `${indent}<${tag} id="${id}" class="component label"${styleAttr} th:text="${thymeleafExpr}">${escapeHtml(text)}</${tag}>`;
  }

  return `${indent}<${tag} id="${id}" class="component label"${styleAttr}>${escapeHtml(text)}</${tag}>`;
}

/**
 * Generate Thymeleaf Button
 */
function generateThymeleafButton(component: ComponentInstance, id: string, indent: string): string {
  const { props, styles, events } = component;
  const text = props.text || 'Click Me';

  const inlineStyle = generateInlineStyle(styles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  // Handle navigation events
  let thymeleafHref = '';
  const clickEvent = events?.find((e: any) => e.eventType === 'onClick');
  if (clickEvent?.action?.type === 'navigate' && clickEvent?.action?.config?.url) {
    const url = clickEvent.action.config.url;
    thymeleafHref = ` th:href="@{${url}}"`;
  }

  if (thymeleafHref) {
    return `${indent}<a id="${id}" class="component button"${styleAttr}${thymeleafHref}>${escapeHtml(text)}</a>`;
  }

  return `${indent}<button id="${id}" class="component button"${styleAttr}>${escapeHtml(text)}</button>`;
}

/**
 * Generate Thymeleaf Image
 */
function generateThymeleafImage(component: ComponentInstance, id: string, indent: string): string {
  const { props, styles, templateBindings } = component;
  const src = props.src || props.url || '';
  const alt = props.alt || '';

  const inlineStyle = generateInlineStyle(styles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  // Check for dynamic src
  if (src.includes('{{') || templateBindings?.src) {
    const srcExpr = convertTemplateVariables(templateBindings?.src || src);
    return `${indent}<img id="${id}" class="component image" th:src="${srcExpr}" th:alt="${convertTemplateVariables(alt)}"${styleAttr} />`;
  }

  // Static src with Thymeleaf static resource handling
  if (src.startsWith('/')) {
    return `${indent}<img id="${id}" class="component image" th:src="@{${src}}" alt="${escapeHtml(alt)}"${styleAttr} />`;
  }

  return `${indent}<img id="${id}" class="component image" src="${src}" alt="${escapeHtml(alt)}"${styleAttr} />`;
}

/**
 * Generate Thymeleaf Container
 */
function generateThymeleafContainer(component: ComponentInstance, id: string, indent: string, depth: number): string {
  const { props, styles, children } = component;

  const inlineStyle = generateInlineStyle(styles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  const childrenHtml = (children || [])
    .map(child => generateThymeleafComponent(child, depth + 1))
    .join('\n');

  if (childrenHtml) {
    return `${indent}<div id="${id}" class="component container"${styleAttr}>\n${childrenHtml}\n${indent}</div>`;
  }
  return `${indent}<div id="${id}" class="component container"${styleAttr}></div>`;
}

/**
 * Generate Thymeleaf Repeater (th:each)
 */
function generateThymeleafRepeater(component: ComponentInstance, id: string, indent: string, depth: number): string {
  const { props, styles, children, iteratorConfig, dataSourceRef } = component;

  const inlineStyle = generateInlineStyle(styles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  const itemAlias = iteratorConfig?.itemAlias || props.itemAlias || 'item';
  const indexAlias = iteratorConfig?.indexAlias || props.indexAlias || 'index';
  const dataPath = iteratorConfig?.dataPath || props.dataPath || '';

  // Build the th:each expression
  // e.g., th:each="item, iterStat : ${dataSources['products'].items}"
  let dataSourceExpr = '';
  if (dataSourceRef) {
    dataSourceExpr = dataPath
      ? `\${dataSources['${dataSourceRef}'].${dataPath}}`
      : `\${dataSources['${dataSourceRef}']}`;
  } else if (dataPath) {
    dataSourceExpr = `\${${dataPath}}`;
  } else {
    dataSourceExpr = '${items}';
  }

  const thEach = `${itemAlias}, ${indexAlias}Stat : ${dataSourceExpr}`;

  // Generate children with item context
  const childrenHtml = (children || [])
    .map(child => {
      // Convert template bindings for children
      const modifiedChild = { ...child };
      if (modifiedChild.templateBindings) {
        // Template bindings will be converted in child generation
      }
      return generateThymeleafComponent(modifiedChild, depth + 2);
    })
    .join('\n');

  const emptyMessage = props.emptyMessage || 'No items to display';

  return `${indent}<div id="${id}" class="component repeater"${styleAttr}>
${indent}    <div th:each="${thEach}">
${childrenHtml || `${indent}        <div th:text="\${${itemAlias}}">Item</div>`}
${indent}    </div>
${indent}    <div th:if="${dataSourceExpr} == null or #lists.isEmpty(${dataSourceExpr})">
${indent}        ${emptyMessage}
${indent}    </div>
${indent}</div>`;
}

/**
 * Generate Thymeleaf DataList (pre-styled table/cards)
 */
function generateThymeleafDataList(component: ComponentInstance, id: string, indent: string, depth: number): string {
  const { props, styles, dataSourceRef, iteratorConfig } = component;

  const inlineStyle = generateInlineStyle(styles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  const listStyle = props.listStyle || 'cards';
  const itemAlias = iteratorConfig?.itemAlias || 'item';
  const dataPath = iteratorConfig?.dataPath || '';

  let dataSourceExpr = '';
  if (dataSourceRef) {
    dataSourceExpr = dataPath
      ? `\${dataSources['${dataSourceRef}'].${dataPath}}`
      : `\${dataSources['${dataSourceRef}']}`;
  } else {
    dataSourceExpr = '${items}';
  }

  if (listStyle === 'table') {
    const columns = props.columns || [];
    const headerRow = columns.map((col: any) =>
      `${indent}            <th>${escapeHtml(col.header || col.field)}</th>`
    ).join('\n');
    const dataRow = columns.map((col: any) =>
      `${indent}            <td th:text="\${${itemAlias}.${col.field}}"></td>`
    ).join('\n');

    return `${indent}<div id="${id}" class="component datalist table-style"${styleAttr}>
${indent}    <table class="data-table">
${indent}        <thead>
${indent}            <tr>
${headerRow}
${indent}            </tr>
${indent}        </thead>
${indent}        <tbody>
${indent}            <tr th:each="${itemAlias} : ${dataSourceExpr}">
${dataRow}
${indent}            </tr>
${indent}        </tbody>
${indent}    </table>
${indent}</div>`;
  }

  // Default: cards style
  const imageField = props.imageField || 'image';
  const titleField = props.titleField || 'title';
  const descField = props.descriptionField || 'description';

  return `${indent}<div id="${id}" class="component datalist cards-style"${styleAttr}>
${indent}    <div class="datalist-grid" th:each="${itemAlias} : ${dataSourceExpr}">
${indent}        <div class="card">
${indent}            <img th:if="\${${itemAlias}.${imageField}}" th:src="\${${itemAlias}.${imageField}}" class="card-image" />
${indent}            <h3 th:text="\${${itemAlias}.${titleField}}" class="card-title"></h3>
${indent}            <p th:text="\${${itemAlias}.${descField}}" class="card-description"></p>
${indent}        </div>
${indent}    </div>
${indent}</div>`;
}

/**
 * Generate Thymeleaf Navbar
 */
function generateThymeleafNavbar(component: ComponentInstance, id: string, indent: string): string {
  const { props, styles } = component;

  const brandText = props.brandText || props.brandName || 'Brand';
  const brandLink = props.brandLink || '/';
  const navItems = props.navItems || [];

  const inlineStyle = generateInlineStyle(styles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  // Parse navItems if string
  let parsedNavItems = navItems;
  if (typeof navItems === 'string') {
    try {
      parsedNavItems = JSON.parse(navItems);
    } catch (e) {
      parsedNavItems = [];
    }
  }

  const navItemsHtml = parsedNavItems.map((item: any) => {
    const href = item.href || '#';
    const isActive = item.active;
    const activeClass = isActive ? ' class="active"' : '';
    return `${indent}            <li${activeClass}><a th:href="@{${href}}">${escapeHtml(item.label || '')}</a></li>`;
  }).join('\n');

  return `${indent}<nav id="${id}" class="component navbar"${styleAttr}>
${indent}    <a th:href="@{${brandLink}}" class="navbar-brand">${escapeHtml(brandText)}</a>
${indent}    <ul class="navbar-nav">
${navItemsHtml}
${indent}    </ul>
${indent}</nav>`;
}

/**
 * Generate generic Thymeleaf component
 */
function generateThymeleafGeneric(component: ComponentInstance, id: string, indent: string, depth: number): string {
  const { props, styles, children, componentId } = component;

  const inlineStyle = generateInlineStyle(styles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  if (children && children.length > 0) {
    const childrenHtml = children.map(child => generateThymeleafComponent(child, depth + 1)).join('\n');
    return `${indent}<div id="${id}" class="component ${componentId.toLowerCase()}"${styleAttr}>\n${childrenHtml}\n${indent}</div>`;
  }

  const text = props.text || props.content || '';
  if (text.includes('{{')) {
    const thymeleafExpr = convertTemplateVariables(text);
    return `${indent}<div id="${id}" class="component ${componentId.toLowerCase()}"${styleAttr} th:text="${thymeleafExpr}">${escapeHtml(text)}</div>`;
  }

  return `${indent}<div id="${id}" class="component ${componentId.toLowerCase()}"${styleAttr}>${escapeHtml(text)}</div>`;
}

/**
 * Generate Thymeleaf template for a page
 */
function generateThymeleafTemplate(pageData: PageExportData): string {
  const { pageName, definition } = pageData;
  const { components, globalStyles, dataContext } = definition;

  const componentsHtml = components
    .map(comp => generateThymeleafComponent(comp, 2))
    .join('\n\n');

  // Custom CSS from global styles
  let customCss = '';
  if (globalStyles?.customCSS) {
    customCss = `\n    <style th:inline="css">\n${globalStyles.customCSS}\n    </style>`;
  }

  return `<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="\${page.title}">${escapeHtml(pageName)}</title>
    <meta th:if="\${page.description}" name="description" th:content="\${page.description}">
    <link rel="stylesheet" th:href="@{/css/styles.css}">${customCss}
</head>
<body>
    <main class="page-content">
${componentsHtml}
    </main>
    <script th:src="@{/js/main.js}" defer></script>
</body>
</html>`;
}

/**
 * Generate PageController.java
 */
function generatePageController(pages: PageExportData[], options: ThymeleafExportOptions): string {
  const controllerMethods = pages.map(page => {
    const methodName = page.pageName.toLowerCase().replace(/[^a-z0-9]/g, '');
    const path = page.path === '/' ? '' : page.path;
    const templateName = page.pageName.toLowerCase().replace(/[^a-z0-9]/g, '-');

    return `
    @GetMapping("${path || '/'}")
    public String ${methodName || 'home'}(Model model) {
        PageDataService.PageData data = pageDataService.loadPageData("${templateName}");
        model.addAttribute("page", data.getPageMeta());
        model.addAttribute("dataSources", data.getData());
        return "${templateName}";
    }`;
  }).join('\n');

  return `package ${options.groupId}.${options.artifactId}.controller;

import ${options.groupId}.${options.artifactId}.service.PageDataService;
import dev.mainul35.siteruntime.service.PageDataService.PageData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final PageDataService pageDataService;

    public PageController(PageDataService pageDataService) {
        this.pageDataService = pageDataService;
    }
${controllerMethods}
}
`;
}

/**
 * Generate pom.xml for the exported project
 */
function generatePomXml(options: ThymeleafExportOptions): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>${options.springBootVersion}</version>
        <relativePath/>
    </parent>

    <groupId>${options.groupId}</groupId>
    <artifactId>${options.artifactId}</artifactId>
    <version>${options.version}</version>
    <packaging>jar</packaging>
    <name>${options.projectName}</name>
    <description>Generated by Visual Site Builder</description>

    <properties>
        <java.version>${options.javaVersion}</java.version>
    </properties>

    <dependencies>
        <!-- Site Runtime (contains all data fetching logic) -->
        <dependency>
            <groupId>dev.mainul35</groupId>
            <artifactId>site-runtime</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Thymeleaf for SSR -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <!-- Optional: Uncomment to add database support -->
        <!--
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
`;
}

/**
 * Generate application.properties template
 */
function generateApplicationProperties(options: ThymeleafExportOptions): string {
  return `# ============================================================
# ${options.projectName} Configuration
# Generated by Visual Site Builder
# ============================================================

server.port=8080

# ============================================================
# API GATEWAY (for data fetching from external APIs)
# ============================================================
site.runtime.api.gateway-url=https://api.yourcompany.com
site.runtime.api.timeout-ms=30000
site.runtime.api.max-retries=3

# ============================================================
# DATABASE (optional - uncomment and configure as needed)
# ============================================================
# Options: none, jpa, mongodb
site.runtime.database.type=none

# For JPA (MySQL, PostgreSQL, H2, etc.)
#site.runtime.database.type=jpa
#spring.datasource.url=jdbc:mysql://localhost:3306/mysite
#spring.datasource.username=root
#spring.datasource.password=secret
#spring.jpa.hibernate.ddl-auto=validate

# ============================================================
# CACHING
# ============================================================
site.runtime.cache.type=memory
site.runtime.cache.default-ttl-ms=60000

# ============================================================
# AUTHENTICATION (optional)
# ============================================================
# Options: none, social, sso
site.runtime.auth.type=none

# For social login (Google, GitHub, Facebook):
#site.runtime.auth.type=social
#site.runtime.auth.social.google.enabled=true
#site.runtime.auth.social.google.client-id=your-google-client-id
#site.runtime.auth.social.google.client-secret=your-google-client-secret

# ============================================================
# THYMELEAF
# ============================================================
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# ============================================================
# LOGGING
# ============================================================
logging.level.dev.mainul35.siteruntime=INFO
logging.level.${options.groupId}=DEBUG
`;
}

/**
 * Generate Application.java main class
 */
function generateApplicationJava(options: ThymeleafExportOptions): string {
  return `package ${options.groupId}.${options.artifactId};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
`;
}

/**
 * Generate base CSS for Thymeleaf templates
 */
function generateBaseCSS(): string {
  return `/* Base Styles - Generated by Visual Site Builder */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html, body {
  width: 100%;
  min-height: 100%;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  line-height: 1.6;
  color: #333;
}

.page-content {
  width: 100%;
  min-height: 100vh;
}

.component {
  position: relative;
}

/* Navbar Styles */
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  min-height: 60px;
  background-color: #ffffff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.navbar-brand {
  font-size: 1.25rem;
  font-weight: 600;
  text-decoration: none;
  color: inherit;
}

.navbar-nav {
  display: flex;
  list-style: none;
  gap: 1rem;
}

.navbar-nav a {
  text-decoration: none;
  color: inherit;
  padding: 0.5rem 1rem;
  transition: color 0.2s;
}

.navbar-nav a:hover {
  color: #007bff;
}

.navbar-nav .active a {
  color: #007bff;
  font-weight: 600;
}

/* Container defaults */
.container {
  width: 100%;
}

/* DataList Styles */
.datalist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.card-title {
  padding: 1rem;
  margin: 0;
}

.card-description {
  padding: 0 1rem 1rem;
  color: #666;
}

/* Table Styles */
.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.data-table th {
  background-color: #f5f5f5;
  font-weight: 600;
}

/* Button Styles */
.button {
  display: inline-block;
  padding: 8px 16px;
  font-weight: 500;
  text-align: center;
  text-decoration: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

/* Responsive */
@media (max-width: 768px) {
  .navbar {
    flex-wrap: wrap;
  }

  .navbar-nav {
    flex-direction: column;
    width: 100%;
    display: none;
  }

  .navbar-nav.active {
    display: flex;
  }

  .datalist-grid {
    grid-template-columns: 1fr;
  }
}
`;
}

/**
 * Generate base JavaScript
 */
function generateBaseJS(): string {
  return `// Site JavaScript - Generated by Visual Site Builder

document.addEventListener('DOMContentLoaded', function() {
  // Mobile navbar toggle
  const navbarToggle = document.querySelector('.navbar-toggle');
  if (navbarToggle) {
    navbarToggle.addEventListener('click', function() {
      const navList = document.querySelector('.navbar-nav');
      if (navList) {
        navList.classList.toggle('active');
      }
    });
  }

  console.log('Site loaded successfully!');
});
`;
}

/**
 * Generate README for the exported project
 */
function generateReadme(options: ThymeleafExportOptions, pages: PageExportData[]): string {
  const pagesList = pages.map(p => `- ${p.pageName} (${p.path})`).join('\n');

  return `# ${options.projectName}

This Spring Boot project was generated by Visual Site Builder.

## Pages
${pagesList}

## Getting Started

### Prerequisites
- Java ${options.javaVersion}+
- Maven 3.6+

### Running Locally
\`\`\`bash
mvn spring-boot:run
\`\`\`

Then open http://localhost:8080 in your browser.

### Building for Production
\`\`\`bash
mvn clean package
java -jar target/${options.artifactId}-${options.version}.jar
\`\`\`

## Configuration

Edit \`src/main/resources/application.properties\` to configure:
- API Gateway URL
- Database connection
- Authentication providers
- Caching settings

## Project Structure
\`\`\`
src/
├── main/
│   ├── java/
│   │   └── ${options.groupId.replace(/\./g, '/')}/${options.artifactId}/
│   │       ├── Application.java
│   │       └── controller/
│   │           └── PageController.java
│   └── resources/
│       ├── application.properties
│       ├── pages/                    # Page data definitions
│       ├── templates/                # Thymeleaf templates
│       └── static/
│           ├── css/
│           └── js/
\`\`\`

## Generated
- Date: ${new Date().toISOString()}
- Tool: Visual Site Builder
`;
}

/**
 * Export site as Spring Boot/Thymeleaf project ZIP
 */
export async function exportAsThymeleafProject(
  pages: PageExportData[],
  options: Partial<ThymeleafExportOptions> = {}
): Promise<Blob> {
  const defaultOptions: ThymeleafExportOptions = {
    projectName: 'my-site',
    groupId: 'com.example',
    artifactId: 'my-site',
    version: '1.0.0',
    springBootVersion: '3.2.0',
    javaVersion: '21',
  };
  const mergedOptions = { ...defaultOptions, ...options };

  const zip = new JSZip();

  // Generate pom.xml
  zip.file('pom.xml', generatePomXml(mergedOptions));

  // Generate Application.java
  const javaBasePath = `src/main/java/${mergedOptions.groupId.replace(/\./g, '/')}/${mergedOptions.artifactId}`;
  zip.file(`${javaBasePath}/Application.java`, generateApplicationJava(mergedOptions));

  // Generate PageController.java
  zip.file(`${javaBasePath}/controller/PageController.java`, generatePageController(pages, mergedOptions));

  // Generate application.properties
  zip.file('src/main/resources/application.properties', generateApplicationProperties(mergedOptions));

  // Generate Thymeleaf templates
  for (const page of pages) {
    const templateName = page.pageName.toLowerCase().replace(/[^a-z0-9]/g, '-');
    zip.file(`src/main/resources/templates/${templateName}.html`, generateThymeleafTemplate(page));

    // Generate page definition JSON for runtime
    zip.file(`src/main/resources/pages/${templateName}.json`, JSON.stringify({
      pageName: page.pageName,
      title: page.definition.pageName,
      description: '',
      path: page.path,
      dataSources: page.definition.dataContext?.dataSources || {},
    }, null, 2));
  }

  // Generate static assets
  zip.file('src/main/resources/static/css/styles.css', generateBaseCSS());
  zip.file('src/main/resources/static/js/main.js', generateBaseJS());

  // Generate README
  zip.file('README.md', generateReadme(mergedOptions, pages));

  // Generate Dockerfile
  zip.file('Dockerfile', `FROM eclipse-temurin:${mergedOptions.javaVersion}-jre
WORKDIR /app
COPY target/${mergedOptions.artifactId}-${mergedOptions.version}.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
`);

  return await zip.generateAsync({ type: 'blob' });
}

/**
 * Download the exported project
 */
export function downloadThymeleafProject(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
