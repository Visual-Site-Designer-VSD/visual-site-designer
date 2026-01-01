import { PageDefinition, ComponentInstance, DataSourceConfig } from '../types/builder';
import JSZip from 'jszip';

/**
 * ThymeleafExportService - Exports site pages as Spring Boot/Thymeleaf project
 * Generates Thymeleaf templates with th:* attributes for dynamic data binding
 */

// Map to track original URLs to their new static paths
let imageUrlMap: Map<string, string> = new Map();

/**
 * Represents an API endpoint configuration detected from Repeater components
 */
interface ApiEndpointConfig {
  endpoint: string;      // Full endpoint path (e.g., "/api/sample/products")
  dataPath: string;      // Path to data in response (e.g., "items")
  controllerName: string; // Generated controller class name
  methodName: string;    // Generated method name
  routePath: string;     // Spring MVC route path
}

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
 * Sanitize a string to be a valid Java package name segment
 * - Removes hyphens and converts to lowercase
 * - Removes any non-alphanumeric characters
 */
function toJavaPackageName(str: string): string {
  // Replace hyphens with nothing (testsite instead of test-site)
  // Remove any other invalid characters
  return str.toLowerCase().replace(/[^a-z0-9]/g, '');
}

/**
 * Get the Java package path from options (handles hyphenated artifact IDs)
 */
function getJavaPackage(options: ThymeleafExportOptions): string {
  return `${options.groupId}.${toJavaPackageName(options.artifactId)}`;
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
 * This is for simple replacement only - use convertToThymeleafExpression for th:text
 */
function convertTemplateVariables(text: string): string {
  if (!text) return text;
  // Replace {{variable.path}} with ${variable.path}
  return text.replace(/\{\{([^}]+)\}\}/g, '${$1}');
}

/**
 * Convert text with template variables to a proper Thymeleaf expression for th:text
 * Handles mixed literal text and variables: "Name: {{item.name}}" -> "'Name: ' + ${item.name}"
 * Pure variables: "{{item.name}}" -> "${item.name}"
 * Multiple variables: "{{item.name}} - {{item.price}}" -> "${item.name} + ' - ' + ${item.price}"
 */
function convertToThymeleafExpression(text: string): string {
  if (!text) return "''";

  // If no template variables, return as literal string
  if (!text.includes('{{')) {
    return `'${text.replace(/'/g, "\\'")}'`;
  }

  // Split text by template variables, keeping the delimiters
  const parts: string[] = [];
  let lastIndex = 0;
  const regex = /\{\{([^}]+)\}\}/g;
  let match;

  while ((match = regex.exec(text)) !== null) {
    // Add literal text before this variable (if any)
    if (match.index > lastIndex) {
      const literal = text.substring(lastIndex, match.index);
      if (literal) {
        parts.push(`'${literal.replace(/'/g, "\\'")}'`);
      }
    }
    // Add the variable expression
    parts.push(`\${${match[1]}}`);
    lastIndex = match.index + match[0].length;
  }

  // Add any remaining literal text after the last variable
  if (lastIndex < text.length) {
    const literal = text.substring(lastIndex);
    if (literal) {
      parts.push(`'${literal.replace(/'/g, "\\'")}'`);
    }
  }

  // Join with ' + ' for concatenation
  return parts.join(' + ');
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
    case 'NavbarCentered':
    case 'NavbarMinimal':
    case 'NavbarDark':
    case 'NavbarGlass':
    case 'NavbarSticky':
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
    const thymeleafExpr = convertToThymeleafExpression(text);
    return `${indent}<${tag} id="${id}" class="component label"${styleAttr} th:text="${thymeleafExpr}">${escapeHtml(text)}</${tag}>`;
  }

  // Check for template bindings
  if (templateBindings?.text) {
    const thymeleafExpr = convertToThymeleafExpression(templateBindings.text);
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
 * Replicates ImageRenderer.tsx structure with container, wrapper, and img
 */
function generateThymeleafImage(component: ComponentInstance, id: string, indent: string): string {
  const { props, styles, templateBindings } = component;
  let src = props.src || props.url || '';
  const alt = props.alt || '';

  // Get props matching ImageRenderer.tsx (lines 12-25)
  const objectFit = (props.objectFit as string) || 'cover';
  const objectPosition = (props.objectPosition as string) || 'center';
  const aspectRatio = (props.aspectRatio as string) || 'auto';
  const borderRadius = (props.borderRadius as string) || '0px';
  const placeholderColor = (props.placeholderColor as string) || '#e0e0e0';

  // Get width/height from props (matching lines 24-25)
  // Handle both string values ("150px") and numeric values (150)
  const rawWidth = props.width;
  const rawHeight = props.height;
  const propsWidth = rawWidth ? (typeof rawWidth === 'number' ? `${rawWidth}px` : String(rawWidth)) : '';
  const propsHeight = rawHeight ? (typeof rawHeight === 'number' ? `${rawHeight}px` : String(rawHeight)) : '';

  // Get stored dimensions from component.size (matching lines 28-29)
  const storedWidth = component.size?.width;
  const storedHeight = component.size?.height;

  // Check if component has a parent (matching line 32)
  const hasParent = !!component.parentId;

  // Effective dimensions (matching lines 38-39)
  // Priority: explicit props > stored size > parent-aware default
  const effectiveWidth = propsWidth || storedWidth || (hasParent ? '100%' : 'auto');
  const effectiveHeight = propsHeight || storedHeight || 'auto';

  console.log(`[Image Export] id=${id}, propsWidth=${propsWidth}, propsHeight=${propsHeight}, effectiveWidth=${effectiveWidth}, effectiveHeight=${effectiveHeight}`);

  // Container styles (matching lines 41-49)
  // When explicit dimensions are set, the container should NOT stretch in flex layouts
  const hasExplicitWidth = !!propsWidth || !!storedWidth;
  const containerStyles: Record<string, any> = {
    width: effectiveWidth,
    height: effectiveHeight,
    // Only apply maxWidth: 100% when the image should fill parent
    // When explicit width is set, don't constrain with maxWidth to allow exact sizing
    ...(hasExplicitWidth ? {} : { maxWidth: '100%' }),
    position: 'relative',
    overflow: 'hidden',
    boxSizing: 'border-box',
    // Prevent flex stretch when explicit dimensions are set
    ...(hasExplicitWidth ? { flexShrink: '0', flexGrow: '0' } : {}),
    ...styles,
  };

  // Determine if using aspect ratio (matching lines 53-54)
  const hasExplicitHeight = (propsHeight && propsHeight !== 'auto') || (storedHeight && storedHeight !== 'auto');
  const useAspectRatio = !hasExplicitHeight;

  // Image wrapper styles (matching lines 56-68)
  const wrapperStyles: Record<string, any> = {
    width: '100%',
    height: hasExplicitHeight ? '100%' : 'auto',
    ...(useAspectRatio && aspectRatio !== 'auto' ? { aspectRatio } : {}),
    backgroundColor: placeholderColor,
    borderRadius: borderRadius,
    overflow: 'hidden',
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  };

  // Image styles (matching lines 70-77)
  const imageStyles: Record<string, any> = {
    width: '100%',
    height: '100%',
    objectFit: objectFit,
    objectPosition: objectPosition,
  };

  const containerStyleStr = generateInlineStyle(containerStyles);
  const wrapperStyleStr = generateInlineStyle(wrapperStyles);
  const imageStyleStr = generateInlineStyle(imageStyles);

  // Check if this image URL has been mapped to a local static path
  if (imageUrlMap.has(src)) {
    src = imageUrlMap.get(src)!;
  }

  // Generate the src attribute
  let srcAttr: string;
  if (src.includes('{{') || templateBindings?.src) {
    const srcExpr = convertTemplateVariables(templateBindings?.src || src);
    srcAttr = `th:src="${srcExpr}"`;
  } else if (src.startsWith('/')) {
    srcAttr = `th:src="@{${src}}"`;
  } else {
    srcAttr = `src="${src}"`;
  }

  // Generate alt attribute
  const altAttr = alt.includes('{{')
    ? `th:alt="${convertToThymeleafExpression(alt)}"`
    : `alt="${escapeHtml(alt)}"`;

  return `${indent}<div id="${id}" class="component image-container" style="${containerStyleStr}">
${indent}    <div class="image-wrapper" style="${wrapperStyleStr}">
${indent}        <img ${srcAttr} ${altAttr} style="${imageStyleStr}" loading="lazy" />
${indent}    </div>
${indent}</div>`;
}

/**
 * Get layout CSS based on layoutMode prop
 */
function getLayoutStyles(layoutMode: string): Record<string, string> {
  switch (layoutMode) {
    case 'flex-row':
      return {
        display: 'flex',
        flexDirection: 'row',
        flexWrap: 'nowrap',
      };
    case 'flex-wrap':
      return {
        display: 'flex',
        flexDirection: 'row',
        flexWrap: 'wrap',
      };
    case 'grid-2col':
      return {
        display: 'grid',
        gridTemplateColumns: 'repeat(2, 1fr)',
      };
    case 'grid-3col':
      return {
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)',
      };
    case 'grid-4col':
      return {
        display: 'grid',
        gridTemplateColumns: 'repeat(4, 1fr)',
      };
    case 'grid-auto':
      return {
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
      };
    case 'flex-column':
    default:
      return {
        display: 'flex',
        flexDirection: 'column',
      };
  }
}

/**
 * Generate Thymeleaf Container
 * Replicates EXACTLY the same style computation as BuilderCanvas PREVIEW mode
 * (NOT ContainerRenderer which has defaults - preview mode is what user actually sees)
 *
 * Key insight from BuilderCanvas (lines 498-519):
 * - Line 500: EXCLUDES width and maxWidth from component.styles
 * - Line 514: Only applies maxWidth from PROPS (not styles) if set and not 'none'
 * - Line 506: Uses stylesWithoutWidth (styles minus width/maxWidth)
 */
function generateThymeleafContainer(component: ComponentInstance, id: string, indent: string, depth: number): string {
  const { props, styles, children } = component;

  // Get layout mode from props
  const layoutMode = props.layoutMode;
  const layoutType = props.layoutType;
  const effectiveLayout = layoutMode || layoutType || 'flex-column';
  const layoutStyles = getLayoutStyles(effectiveLayout);

  // CRITICAL: Match BuilderCanvas line 500 - exclude width and maxWidth from styles
  // These should be controlled by props, not stored style values
  // Also exclude default values that shouldn't override parent backgrounds
  const {
    width: _ignoredWidth,
    maxWidth: _ignoredMaxWidth,
    ...stylesWithoutWidth
  } = styles || {};

  // For NESTED containers (depth > 0), remove default visual styling
  // that makes containers appear as visible boxes when they should be transparent.
  // This matches preview behavior where nested containers appear transparent overlays.
  if (depth > 0) {
    // Check if this container has an INTENTIONAL visible background (gradient, image)
    const bg = stylesWithoutWidth.background;
    const bgColor = stylesWithoutWidth.backgroundColor;

    const hasGradientOrImage = bg && typeof bg === 'string' &&
      (bg.includes('gradient') || bg.includes('url('));

    // Check if backgroundColor is a default/white/transparent value that should be removed
    // This includes: undefined, white variants, transparent, and rgba with full transparency
    const isDefaultOrTransparentBg = !bgColor ||
      bgColor === '#ffffff' || bgColor === '#fff' || bgColor === 'white' ||
      bgColor === 'rgb(255, 255, 255)' || bgColor === 'rgb(255,255,255)' ||
      bgColor === 'transparent' || bgColor === 'rgba(255, 255, 255, 0)' ||
      bgColor === 'rgba(0, 0, 0, 0)' || bgColor === 'initial' || bgColor === 'inherit';

    // If no intentional background (gradient/image) and backgroundColor is default/transparent,
    // this container should be transparent - remove background styling
    if (!hasGradientOrImage && isDefaultOrTransparentBg) {
      delete stylesWithoutWidth.backgroundColor;
      delete stylesWithoutWidth.background;
    }

    // Also remove default "card-like" styling (borderRadius, boxShadow, border)
    // for nested containers unless they have an intentional gradient/image background
    if (!hasGradientOrImage) {
      // Remove borderRadius - nested containers typically shouldn't have rounded corners
      // unless they're intentionally styled cards with backgrounds
      const br = stylesWithoutWidth.borderRadius;
      if (!br || br === '8px' || br === '0' || br === '0px' || br === '4px' || br === '12px' || br === '16px') {
        delete stylesWithoutWidth.borderRadius;
      }

      // Remove boxShadow - nested containers typically shouldn't have shadows
      const bs = stylesWithoutWidth.boxShadow;
      if (!bs || bs === 'none' || bs.includes('rgba(0,0,0,0.1)') || bs.includes('rgba(0, 0, 0, 0.1)') ||
          bs.includes('rgba(0,0,0,0.05)') || bs.includes('rgba(0, 0, 0, 0.05)')) {
        delete stylesWithoutWidth.boxShadow;
      }

      // Remove border if not explicitly styled with visible border
      const border = stylesWithoutWidth.border;
      if (!border || border === 'none' || border === '0' || border === '0px' ||
          border === '1px solid transparent' || border === '0 none') {
        delete stylesWithoutWidth.border;
      }
    }
  }

  // Get props - NO DEFAULTS, only use what's explicitly set
  // This matches BuilderCanvas preview mode behavior (lines 480-518)
  const maxWidth = props.maxWidth;  // From PROPS, not styles
  const centerContent = props.centerContent;
  const gap = props.gap || styles?.gap;
  const padding = styles?.padding || props.padding;

  // Get flex alignment props from component props (matching lines 494-496)
  const alignItems = props.alignItems;
  const justifyContent = props.justifyContent;
  const flexWrap = styles?.flexWrap;

  // Build container styles - matching BuilderCanvas preview mode (lines 505-519)
  // Start with stylesWithoutWidth (EXCLUDING width/maxWidth from stored styles)
  const containerStyles: Record<string, any> = {
    ...stylesWithoutWidth,  // Line 506: Use styles WITHOUT width/maxWidth
    ...layoutStyles,
    // Override with explicit flex props from template (matching lines 509-511)
    alignItems: alignItems || layoutStyles.alignItems,
    justifyContent: justifyContent || layoutStyles.justifyContent,
    flexWrap: flexWrap || layoutStyles.flexWrap,
    // Only apply maxWidth from PROPS if explicitly set AND not 'none' (matching line 514)
    ...(maxWidth && maxWidth !== 'none' ? { maxWidth } : {}),
    // Only apply centering if centerContent is true (matching lines 515-516)
    ...(centerContent ? { marginLeft: 'auto', marginRight: 'auto' } : {}),
    // Only apply gap if set (matching line 517)
    ...(gap ? { gap } : {}),
    // Only apply padding if set (matching line 518)
    ...(padding ? { padding } : {}),
  };

  // Clean up undefined values
  Object.keys(containerStyles).forEach(key => {
    if (containerStyles[key] === undefined) {
      delete containerStyles[key];
    }
  });

  const inlineStyle = generateInlineStyle(containerStyles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  // Check if this is a flex-row layout (for child wrapper logic)
  const isFlexRow = effectiveLayout === 'flex-row' || effectiveLayout === 'flex-wrap';

  // Generate children - matching BuilderCanvas lines 521-552
  // For layout children in non-flex-row layouts, they get width: 100%
  const childrenHtml = (children || [])
    .map(child => {
      const childIsLayout = child.componentCategory?.toLowerCase() === 'layout';

      if (childIsLayout && !isFlexRow) {
        // Wrap layout children with width: 100% for non-flex-row layouts (line 544)
        const childHtml = generateThymeleafComponent(child, depth + 2);
        return `${indent}    <div style="width: 100%">\n${childHtml}\n${indent}    </div>`;
      } else if (childIsLayout && isFlexRow) {
        // For flex-row, layout children get flex: 1 (line 546)
        const childHtml = generateThymeleafComponent(child, depth + 2);
        return `${indent}    <div style="flex: 1">\n${childHtml}\n${indent}    </div>`;
      }

      // Non-layout children rendered directly (lines 526-531)
      return generateThymeleafComponent(child, depth + 1);
    })
    .join('\n');

  if (childrenHtml) {
    return `${indent}<div id="${id}" class="component container"${styleAttr}>\n${childrenHtml}\n${indent}</div>`;
  }
  return `${indent}<div id="${id}" class="component container"${styleAttr}></div>`;
}

/**
 * Collect all component-level data sources (static data) from the component tree
 * Returns a map of dataSourceKey -> data for inclusion in page JSON
 */
function collectComponentDataSources(components: ComponentInstance[]): Record<string, any> {
  const dataSources: Record<string, any> = {};

  function traverse(comp: ComponentInstance) {
    // Check if this component has static data
    if (comp.dataSource?.type === 'static' && comp.dataSource?.staticData) {
      const dataSourceKey = `repeater_${comp.instanceId}`;
      dataSources[dataSourceKey] = comp.dataSource.staticData;
    }

    // Recursively check children
    if (comp.children && comp.children.length > 0) {
      comp.children.forEach(child => traverse(child));
    }
  }

  components.forEach(comp => traverse(comp));
  return dataSources;
}

/**
 * Generate Thymeleaf Repeater (th:each)
 * Handles both dataSourceRef (page-level) and component-level dataSource.staticData
 * Replicates RepeaterRenderer.tsx layout behavior (lines 159-205, 397-405)
 */
function generateThymeleafRepeater(component: ComponentInstance, id: string, indent: string, depth: number): string {
  const { props, styles, children, iteratorConfig, dataSourceRef, dataSource, instanceId } = component;

  // Get layout props matching RepeaterRenderer (lines 48-49)
  const layoutType = (props.layoutType as string) || 'flex-column';
  const gap = (props.gap as string) || '16px';

  // Get layout styles matching RepeaterRenderer (lines 159-205)
  const getRepeaterLayoutStyles = (): Record<string, string> => {
    const baseStyles: Record<string, string> = {
      width: '100%',
      gap: gap,
    };

    switch (layoutType) {
      case 'flex-row':
        return {
          ...baseStyles,
          display: 'flex',
          flexDirection: 'row',
          flexWrap: 'wrap',
        };
      case 'grid-2col':
        return {
          ...baseStyles,
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
        };
      case 'grid-3col':
        return {
          ...baseStyles,
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
        };
      case 'grid-4col':
        return {
          ...baseStyles,
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
        };
      case 'grid-auto':
        return {
          ...baseStyles,
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))',
        };
      case 'flex-column':
      default:
        return {
          ...baseStyles,
          display: 'flex',
          flexDirection: 'column',
        };
    }
  };

  // Container styles matching RepeaterRenderer (lines 397-405)
  const layoutStyles = getRepeaterLayoutStyles();
  const containerStyles: Record<string, any> = {
    ...layoutStyles,
    backgroundColor: styles?.backgroundColor || 'transparent',
    padding: styles?.padding || '0',
    borderRadius: styles?.borderRadius || '0',
    border: styles?.border || 'none',
    boxSizing: 'border-box',
  };

  const inlineStyle = generateInlineStyle(containerStyles);
  const styleAttr = inlineStyle ? ` style="${inlineStyle}"` : '';

  const itemAlias = iteratorConfig?.itemAlias || props.itemAlias || 'item';
  const indexAlias = iteratorConfig?.indexAlias || props.indexAlias || 'index';
  const dataPath = iteratorConfig?.dataPath || props.dataPath || '';

  // Build the th:each expression - use ${...} syntax
  // e.g., th:each="item, iterStat : ${dataSources['products'].items}"
  // For th:if, we need the expression WITHOUT ${} wrapper since th:if expects a raw expression
  let dataSourceExpr = '';
  let dataSourceExprRaw = ''; // For use in th:if

  if (dataSourceRef) {
    // Page-level named data source
    dataSourceExprRaw = dataPath
      ? `dataSources['${dataSourceRef}'].${dataPath}`
      : `dataSources['${dataSourceRef}']`;
    dataSourceExpr = `\${${dataSourceExprRaw}}`;
  } else if (dataSource?.type === 'static' && dataSource?.staticData) {
    // Component-level static data - use instanceId as the data source key
    const dataSourceKey = `repeater_${instanceId}`;
    dataSourceExprRaw = `dataSources['${dataSourceKey}']`;
    dataSourceExpr = `\${${dataSourceExprRaw}}`;
  } else if (dataPath) {
    dataSourceExprRaw = dataPath;
    dataSourceExpr = `\${${dataPath}}`;
  } else {
    dataSourceExprRaw = 'items';
    dataSourceExpr = '${items}';
  }

  const thEach = `${itemAlias}, ${indexAlias}Stat : ${dataSourceExpr}`;

  // Generate children with item context
  // Each child in the repeater becomes a grid/flex item
  // We render children at depth 0 to preserve their full styling (borders, backgrounds, etc.)
  // since they are the "root" of each repeated item
  const childrenHtml = (children || [])
    .map(child => {
      // Convert template bindings for children
      const modifiedChild = { ...child };
      if (modifiedChild.templateBindings) {
        // Template bindings will be converted in child generation
      }
      // Use depth 0 for repeater children so they keep their card-like styling
      return generateThymeleafComponent(modifiedChild, 0);
    })
    .join('\n');

  const emptyMessage = props.emptyMessage || 'No items to display';

  // For grid/flex layouts, we need the th:each directly on the item wrapper
  // so each iteration creates a proper grid cell
  // Structure: outer container has grid, each th:each item is a grid cell
  // Add proper constraints matching RepeaterRenderer (lines 301-314)
  const itemWrapperStyle = 'max-width: 100%; box-sizing: border-box; overflow: hidden;';

  return `${indent}<div id="${id}" class="component repeater"${styleAttr}>
${indent}    <div th:each="${thEach}" class="repeater-item" style="${itemWrapperStyle}">
${childrenHtml || `${indent}        <div th:text="\${${itemAlias}}">Item</div>`}
${indent}    </div>
${indent}    <div th:if="\${${dataSourceExprRaw} == null or #lists.isEmpty(${dataSourceExprRaw})}" style="grid-column: 1 / -1; text-align: center; padding: 20px; color: #999;">
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
 * Matches exactly the styling computed by NavbarRenderer.tsx (lines 110-160)
 */
function generateThymeleafNavbar(component: ComponentInstance, id: string, indent: string): string {
  const { props, styles } = component;

  const brandText = (props.brandText as string) || (props.brandName as string) || 'Brand';
  const brandLink = (props.brandLink as string) || '/';
  const navItems = props.navItems || [];
  const layout = (props.layout as string) || 'default';
  const sticky = Boolean(props.sticky);

  // Extract style props from component.styles - matching NavbarRenderer lines 61-74
  const backgroundColor = (styles.backgroundColor as string) || '#ffffff';
  const textColor = (styles.textColor as string) || '#333333';
  const accentColor = (styles.accentColor as string) || '#007bff';
  const padding = (styles.padding as string) || '0 20px';
  const boxShadow = (styles.boxShadow as string) || '0 2px 4px rgba(0,0,0,0.1)';
  const borderBottom = (styles.borderBottom as string) || '1px solid #e0e0e0';
  const fontFamily = (styles.fontFamily as string) || 'inherit';
  const fontSize = (styles.fontSize as string) || '16px';

  // Get justify-content based on layout - matching NavbarRenderer lines 76-89
  const getJustifyContent = (): string => {
    switch (layout) {
      case 'centered': return 'center';
      case 'split': return 'space-between';
      case 'minimal': return 'flex-start';
      default: return 'space-between';
    }
  };

  // Container styles - matching NavbarRenderer lines 110-130
  const containerStyles: Record<string, any> = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: getJustifyContent(),
    width: '100%',
    minHeight: '40px',
    backgroundColor,
    color: textColor,
    padding,
    boxShadow,
    borderBottom,
    fontFamily,
    fontSize,
    boxSizing: 'border-box',
    position: sticky ? 'sticky' : 'relative',
    top: sticky ? '0' : 'auto',
    zIndex: sticky ? '1000' : 'auto',
  };

  // Brand styles - matching NavbarRenderer lines 133-142
  const brandStyles: Record<string, any> = {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    textDecoration: 'none',
    color: textColor,
    fontWeight: '600',
    fontSize: '1.25em',
  };

  // Nav list styles (desktop mode) - matching NavbarRenderer lines 145-160
  const navListStyles: Record<string, any> = {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    gap: '8px',
    listStyle: 'none',
    margin: '0',
    padding: '0',
  };

  // Nav link styles - matching NavbarRenderer lines 163-177
  const getLinkStyles = (isActive: boolean): Record<string, any> => ({
    display: 'flex',
    alignItems: 'center',
    padding: '8px 12px',
    textDecoration: 'none',
    color: isActive ? accentColor : textColor,
    fontWeight: isActive ? '600' : '400',
    borderBottom: isActive ? `2px solid ${accentColor}` : '2px solid transparent',
    whiteSpace: 'nowrap',
  });

  const containerStyleStr = generateInlineStyle(containerStyles);
  const brandStyleStr = generateInlineStyle(brandStyles);
  const navListStyleStr = generateInlineStyle(navListStyles);

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
    const isActive = Boolean(item.active);
    const linkStyle = generateInlineStyle(getLinkStyles(isActive));
    return `${indent}            <li><a th:href="@{${href}}" style="${linkStyle}">${escapeHtml(item.label || '')}</a></li>`;
  }).join('\n');

  // Add spacer for split layout
  const spacer = layout === 'split' ? `\n${indent}    <div style="flex: 1"></div>` : '';

  return `${indent}<nav id="${id}" class="component navbar" style="${containerStyleStr}">
${indent}    <a th:href="@{${brandLink}}" class="navbar-brand" style="${brandStyleStr}">${escapeHtml(brandText)}</a>${spacer}
${indent}    <ul class="navbar-nav" style="${navListStyleStr}">
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
    const thymeleafExpr = convertToThymeleafExpression(text);
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
  const javaPackage = getJavaPackage(options);
  const controllerMethods = pages.map(page => {
    const methodName = page.pageName.toLowerCase().replace(/[^a-z0-9]/g, '');
    const templateName = page.pageName.toLowerCase().replace(/[^a-z0-9]/g, '-');
    const isHomePage = page.path === '/' || page.path === '/home';

    // For home page, map both "/" and "/home"
    if (isHomePage) {
      return `
    @GetMapping({"/", "/home"})
    public String ${methodName || 'home'}(Model model) {
        PageData data = pageDataService.loadPageData("${templateName}");
        model.addAttribute("page", data.getPageMeta());
        model.addAttribute("dataSources", data.getData());
        return "${templateName}";
    }`;
    }

    return `
    @GetMapping("${page.path}")
    public String ${methodName}(Model model) {
        PageData data = pageDataService.loadPageData("${templateName}");
        model.addAttribute("page", data.getPageMeta());
        model.addAttribute("dataSources", data.getData());
        return "${templateName}";
    }`;
  }).join('\n');

  return `package ${javaPackage}.controller;

import ${javaPackage}.service.PageDataService;
import ${javaPackage}.service.PageDataService.PageData;
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
 * Generate PageDataService.java - Self-contained service for loading page data
 */
function generatePageDataService(options: ThymeleafExportOptions): string {
  const javaPackage = getJavaPackage(options);
  return `package ${javaPackage}.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * PageDataService - Loads page metadata and data sources from JSON files
 */
@Service
public class PageDataService {

    private final ObjectMapper objectMapper;

    public PageDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Load page data from the pages/ resource directory
     */
    public PageData loadPageData(String pageName) {
        try {
            ClassPathResource resource = new ClassPathResource("pages/" + pageName + ".json");
            if (!resource.exists()) {
                return new PageData(
                    new PageMeta(pageName, pageName, ""),
                    new HashMap<>()
                );
            }

            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);

                PageMeta pageMeta = new PageMeta(
                    root.has("pageName") ? root.get("pageName").asText() : pageName,
                    root.has("title") ? root.get("title").asText() : pageName,
                    root.has("description") ? root.get("description").asText() : ""
                );

                Map<String, Object> dataSources = new HashMap<>();
                if (root.has("dataSources")) {
                    JsonNode ds = root.get("dataSources");
                    ds.fields().forEachRemaining(entry -> {
                        dataSources.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class));
                    });
                }

                return new PageData(pageMeta, dataSources);
            }
        } catch (IOException e) {
            return new PageData(
                new PageMeta(pageName, pageName, ""),
                new HashMap<>()
            );
        }
    }

    /**
     * Page metadata
     */
    public static class PageMeta {
        private final String name;
        private final String title;
        private final String description;

        public PageMeta(String name, String title, String description) {
            this.name = name;
            this.title = title;
            this.description = description;
        }

        public String getName() { return name; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }

    /**
     * Container for page data
     */
    public static class PageData {
        private final PageMeta pageMeta;
        private final Map<String, Object> data;

        public PageData(PageMeta pageMeta, Map<String, Object> data) {
            this.pageMeta = pageMeta;
            this.data = data;
        }

        public PageMeta getPageMeta() { return pageMeta; }
        public Map<String, Object> getData() { return data; }
    }
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

        <!-- Jackson for JSON processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
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
 * Generate pom.xml with Lombok support for API controllers
 * Adds Lombok dependency when API endpoints are detected
 */
function generatePomXmlWithLombok(options: ThymeleafExportOptions, hasApiEndpoints: boolean): string {
  const lombokDependency = hasApiEndpoints ? `
        <!-- Lombok for reducing boilerplate -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
` : '';

  const lombokAnnotationProcessor = hasApiEndpoints ? `
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>` : '';

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

        <!-- Jackson for JSON processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
${lombokDependency}
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
                <artifactId>spring-boot-maven-plugin</artifactId>${lombokAnnotationProcessor}
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
# THYMELEAF
# ============================================================
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# ============================================================
# DATABASE (optional - uncomment and configure as needed)
# ============================================================
# For JPA (MySQL, PostgreSQL, H2, etc.)
#spring.datasource.url=jdbc:mysql://localhost:3306/mysite
#spring.datasource.username=root
#spring.datasource.password=secret
#spring.jpa.hibernate.ddl-auto=validate

# ============================================================
# LOGGING
# ============================================================
logging.level.${options.groupId}=DEBUG
`;
}

/**
 * Collect all image URLs from a component tree
 * Handles both absolute URLs (http://...) and relative paths (/uploads/...)
 */
function collectImageUrls(components: ComponentInstance[]): Set<string> {
  const urls = new Set<string>();

  const processComponent = (component: ComponentInstance) => {
    // Check for image src in props
    const src = component.props?.src || component.props?.url;
    if (src && typeof src === 'string' && src.trim() !== '') {
      // Include both absolute URLs and relative paths (like /uploads/...)
      if (src.startsWith('http') || src.startsWith('/')) {
        urls.add(src);
      }
    }

    // Also check for background images in styles
    const bgImage = component.styles?.backgroundImage;
    if (bgImage && typeof bgImage === 'string') {
      const urlMatch = bgImage.match(/url\(['"]?([^'")\s]+)['"]?\)/);
      if (urlMatch && urlMatch[1]) {
        const bgUrl = urlMatch[1];
        if (bgUrl.startsWith('http') || bgUrl.startsWith('/')) {
          urls.add(bgUrl);
        }
      }
    }

    // Process children recursively
    if (component.children) {
      component.children.forEach(processComponent);
    }
  };

  components.forEach(processComponent);
  return urls;
}

/**
 * Generate a safe filename from a URL
 */
function urlToFilename(url: string): string {
  try {
    // Handle both absolute URLs and relative paths
    let pathname: string;
    if (url.startsWith('http')) {
      const urlObj = new URL(url);
      pathname = urlObj.pathname;
    } else {
      pathname = url;
    }

    // Get the last part of the path and sanitize it
    let filename = pathname.split('/').pop() || 'image';

    // If no extension, try to add one
    if (!filename.includes('.')) {
      filename += '.png';
    }

    // Sanitize the filename
    filename = filename.replace(/[^a-zA-Z0-9._-]/g, '_');

    return filename;
  } catch {
    return `image_${Date.now()}.png`;
  }
}

/**
 * Convert a relative URL to absolute URL using current origin
 */
function toAbsoluteUrl(url: string): string {
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  if (url.startsWith('/')) {
    return `${window.location.origin}${url}`;
  }
  return new URL(url, window.location.href).href;
}

/**
 * Fetch an image and return it as a blob
 */
async function fetchImageAsBlob(url: string): Promise<Blob | null> {
  try {
    const absoluteUrl = toAbsoluteUrl(url);
    const response = await fetch(absoluteUrl, {
      mode: 'cors',
      credentials: 'include',
    });
    if (!response.ok) {
      console.warn(`Failed to fetch image: ${absoluteUrl} (${response.status})`);
      return null;
    }
    return await response.blob();
  } catch (err) {
    console.warn(`Failed to fetch image: ${url}`, err);
    return null;
  }
}

/**
 * Collect images from all pages and add them to the ZIP
 * Returns a map of original URL to new static path
 */
async function collectAndAddImages(
  pages: PageExportData[],
  zip: JSZip
): Promise<Map<string, string>> {
  const urlMap = new Map<string, string>();
  const allUrls = new Set<string>();

  // Collect all image URLs from all pages
  for (const page of pages) {
    const pageUrls = collectImageUrls(page.definition.components);
    pageUrls.forEach(url => allUrls.add(url));
  }

  // Fetch and add each image to the ZIP
  for (const url of allUrls) {
    try {
      const blob = await fetchImageAsBlob(url);
      if (blob) {
        const filename = urlToFilename(url);
        const staticPath = `/images/${filename}`;

        // Add to ZIP
        zip.file(`src/main/resources/static/images/${filename}`, blob);

        // Map original URL to new path
        urlMap.set(url, staticPath);
      }
    } catch (err) {
      console.warn(`Failed to process image: ${url}`, err);
    }
  }

  return urlMap;
}

/**
 * Generate Application.java main class
 */
function generateApplicationJava(options: ThymeleafExportOptions): string {
  const javaPackage = getJavaPackage(options);
  return `package ${javaPackage};

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
 * MINIMAL reset only - component styles come from inline styles to preserve exact design
 */
function generateBaseCSS(): string {
  return `/* Base Styles - Generated by Visual Site Builder */
/* Minimal reset - all component styles are inline to preserve exact design */
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
}

.page-content {
  width: 100%;
  min-height: 100vh;
}

/* Navbar navigation list reset */
.navbar-nav {
  list-style: none;
}

.navbar-nav a {
  text-decoration: none;
  color: inherit;
}

/* DataList grid layout helper */
.datalist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

/* Table reset */
.data-table {
  width: 100%;
  border-collapse: collapse;
}

/* Button base */
.button {
  cursor: pointer;
  border: none;
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
│           ├── js/
│           └── images/               # Exported images
\`\`\`

## Generated
- Date: ${new Date().toISOString()}
- Tool: Visual Site Builder
`;
}

/**
 * Collect all API endpoints used by Repeater components across all pages
 * Analyzes dataSource configurations to identify API-based data sources
 */
function collectApiEndpoints(pages: PageExportData[]): ApiEndpointConfig[] {
  const endpointsMap = new Map<string, ApiEndpointConfig>();

  const processComponent = (component: ComponentInstance) => {
    // Check if this is a Repeater with API data source
    if (component.componentId === 'Repeater' && component.dataSource?.type === 'api') {
      const apiConfig = component.dataSource;
      const endpoint = apiConfig.apiEndpoint || apiConfig.endpoint || '';

      if (endpoint && !endpointsMap.has(endpoint)) {
        // Parse the endpoint to generate controller/method names
        const parsed = parseApiEndpoint(endpoint);
        endpointsMap.set(endpoint, {
          endpoint,
          dataPath: apiConfig.dataPath || component.iteratorConfig?.dataPath || '',
          controllerName: parsed.controllerName,
          methodName: parsed.methodName,
          routePath: parsed.routePath,
        });
      }
    }

    // Recursively process children
    if (component.children) {
      component.children.forEach(processComponent);
    }
  };

  for (const page of pages) {
    page.definition.components.forEach(processComponent);
  }

  return Array.from(endpointsMap.values());
}

/**
 * Parse an API endpoint URL to extract controller and method names
 * e.g., "/api/sample/products" -> { controllerName: "SampleDataController", methodName: "getProducts", routePath: "/api/sample/products" }
 */
function parseApiEndpoint(endpoint: string): { controllerName: string; methodName: string; routePath: string } {
  // Remove leading slash and split into parts
  const parts = endpoint.replace(/^\//, '').split('/');

  // Try to identify a controller name from the path
  // Common pattern: /api/{controller}/{action}
  let controllerName = 'DataController';
  let methodName = 'getData';
  let routePath = endpoint;

  if (parts.length >= 2) {
    // Use the second part as controller base (after 'api')
    const controllerBase = parts[1] || 'data';
    controllerName = toPascalCase(controllerBase) + 'Controller';

    // Use the last part as method name
    const actionPart = parts[parts.length - 1];
    methodName = 'get' + toPascalCase(actionPart);
  }

  return { controllerName, methodName, routePath };
}

/**
 * Convert string to PascalCase
 */
function toPascalCase(str: string): string {
  return str
    .split(/[-_]/)
    .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join('');
}

/**
 * Convert string to camelCase
 */
function toCamelCase(str: string): string {
  const pascal = toPascalCase(str);
  return pascal.charAt(0).toLowerCase() + pascal.slice(1);
}

/**
 * Generate API Data Controller for exported site
 * Creates a controller that serves the same data structure as the CMS sample endpoints
 */
function generateApiDataController(endpoints: ApiEndpointConfig[], options: ThymeleafExportOptions): string {
  const javaPackage = getJavaPackage(options);

  // Group endpoints by controller name
  const controllerGroups = new Map<string, ApiEndpointConfig[]>();
  for (const ep of endpoints) {
    const existing = controllerGroups.get(ep.controllerName) || [];
    existing.push(ep);
    controllerGroups.set(ep.controllerName, existing);
  }

  // For simplicity, we'll create a single SampleDataController that handles all endpoints
  // In production, you'd want to split these into separate controller files

  const methodsCode = endpoints.map(ep => {
    const dataPathKey = ep.dataPath || 'items';
    return `
    /**
     * API endpoint for: ${ep.endpoint}
     * Data path: ${ep.dataPath || '(root array)'}
     */
    @GetMapping("${ep.routePath}")
    public ResponseEntity<Map<String, Object>> ${ep.methodName}(
            @RequestParam(required = false) Map<String, String> params) {

        log.debug("Fetching data from ${ep.endpoint} with params: {}", params);

        // TODO: Replace with your actual data fetching logic
        // This is sample data - modify to fetch from database or external service
        List<Map<String, Object>> items = getSampleData("${ep.methodName}");

        Map<String, Object> response = new HashMap<>();
        response.put("${dataPathKey}", items);
        response.put("total", items.size());

        return ResponseEntity.ok(response);
    }`;
  }).join('\n');

  return `package ${javaPackage}.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * API Data Controller - Provides data endpoints for dynamic components.
 *
 * This controller was generated based on API data sources configured in the
 * visual builder. Modify the data fetching logic to connect to your actual
 * data sources (database, external APIs, etc.).
 *
 * Generated by Visual Site Builder
 */
@RestController
@Slf4j
@CrossOrigin(origins = "*")
public class ApiDataController {
${methodsCode}

    /**
     * Get sample data for an endpoint
     * TODO: Replace this with your actual data source logic
     */
    private List<Map<String, Object>> getSampleData(String endpointMethod) {
        List<Map<String, Object>> items = new ArrayList<>();

        switch (endpointMethod) {
            case "getProducts":
                items.add(createItem(1L, "Sample Product 1", "Description for product 1", 29.99, "https://via.placeholder.com/300x200"));
                items.add(createItem(2L, "Sample Product 2", "Description for product 2", 49.99, "https://via.placeholder.com/300x200"));
                items.add(createItem(3L, "Sample Product 3", "Description for product 3", 79.99, "https://via.placeholder.com/300x200"));
                break;
            case "getTeam":
                items.add(createTeamMember(1L, "John Doe", "CEO", "https://via.placeholder.com/100x100", "john@example.com"));
                items.add(createTeamMember(2L, "Jane Smith", "CTO", "https://via.placeholder.com/100x100", "jane@example.com"));
                break;
            case "getPosts":
                items.add(createPost(1L, "Sample Blog Post", "This is a sample blog post excerpt", "Technology", "2024-01-15"));
                items.add(createPost(2L, "Another Post", "Another sample post excerpt", "Design", "2024-01-10"));
                break;
            case "getTestimonials":
                items.add(createTestimonial(1L, "Great product!", "John Smith", "CEO, TechCorp", 5));
                items.add(createTestimonial(2L, "Highly recommended!", "Jane Doe", "Designer", 5));
                break;
            case "getServices":
                items.add(createService(1L, "Web Development", "Custom web applications", "🌐"));
                items.add(createService(2L, "Mobile Apps", "iOS and Android development", "📱"));
                break;
            default:
                items.add(createItem(1L, "Sample Item 1", "Sample description", 0.0, ""));
                items.add(createItem(2L, "Sample Item 2", "Sample description", 0.0, ""));
        }

        return items;
    }

    private Map<String, Object> createItem(Long id, String name, String description, double price, String image) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("name", name);
        item.put("description", description);
        item.put("price", price);
        item.put("image", image);
        return item;
    }

    private Map<String, Object> createTeamMember(Long id, String name, String role, String avatar, String email) {
        Map<String, Object> member = new LinkedHashMap<>();
        member.put("id", id);
        member.put("name", name);
        member.put("role", role);
        member.put("avatar", avatar);
        member.put("email", email);
        return member;
    }

    private Map<String, Object> createPost(Long id, String title, String excerpt, String category, String date) {
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("id", id);
        post.put("title", title);
        post.put("excerpt", excerpt);
        post.put("category", category);
        post.put("date", date);
        return post;
    }

    private Map<String, Object> createTestimonial(Long id, String quote, String name, String title, int rating) {
        Map<String, Object> testimonial = new LinkedHashMap<>();
        testimonial.put("id", id);
        testimonial.put("quote", quote);
        testimonial.put("name", name);
        testimonial.put("title", title);
        testimonial.put("rating", rating);
        return testimonial;
    }

    private Map<String, Object> createService(Long id, String name, String description, String icon) {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("id", id);
        service.put("name", name);
        service.put("description", description);
        service.put("icon", icon);
        return service;
    }
}
`;
}

/**
 * Generate DataService for fetching data from external APIs or database
 * This provides a more structured approach for production use
 */
function generateDataService(endpoints: ApiEndpointConfig[], options: ThymeleafExportOptions): string {
  const javaPackage = getJavaPackage(options);

  return `package ${javaPackage}.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Data Service - Handles data fetching for API endpoints.
 *
 * Modify this service to:
 * - Connect to your database using JPA repositories
 * - Fetch data from external APIs
 * - Apply business logic and transformations
 *
 * Generated by Visual Site Builder
 */
@Service
@Slf4j
public class DataService {

    private final RestTemplate restTemplate;

    public DataService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetch data from an external API endpoint
     * @param apiUrl The full URL of the external API
     * @return List of items from the API response
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchFromExternalApi(String apiUrl, String dataPath) {
        try {
            log.debug("Fetching data from external API: {}", apiUrl);
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);

            if (response == null) {
                return Collections.emptyList();
            }

            // Navigate to the data path if specified
            if (dataPath != null && !dataPath.isEmpty()) {
                Object data = response.get(dataPath);
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
            }

            // If no data path, check if response is directly a list wrapper
            for (Object value : response.values()) {
                if (value instanceof List) {
                    return (List<Map<String, Object>>) value;
                }
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch data from external API: {}", apiUrl, e);
            return Collections.emptyList();
        }
    }

    /**
     * Example: Fetch products from your database
     * TODO: Implement with your JPA repository
     */
    public List<Map<String, Object>> getProductsFromDatabase() {
        // Example implementation - replace with actual repository call:
        // return productRepository.findAll().stream()
        //     .map(this::productToMap)
        //     .collect(Collectors.toList());

        log.debug("getProductsFromDatabase called - implement with your repository");
        return Collections.emptyList();
    }

    /**
     * Example: Fetch team members from your database
     * TODO: Implement with your JPA repository
     */
    public List<Map<String, Object>> getTeamMembersFromDatabase() {
        log.debug("getTeamMembersFromDatabase called - implement with your repository");
        return Collections.emptyList();
    }
}
`;
}

/**
 * Update README with API controller documentation
 */
function generateReadmeWithApiDocs(options: ThymeleafExportOptions, pages: PageExportData[], endpoints: ApiEndpointConfig[]): string {
  const pagesList = pages.map(p => `- ${p.pageName} (${p.path})`).join('\n');
  const endpointsList = endpoints.length > 0
    ? endpoints.map(ep => `- ${ep.endpoint} (Data path: ${ep.dataPath || 'root'})`).join('\n')
    : '- No API endpoints detected';

  return `# ${options.projectName}

This Spring Boot project was generated by Visual Site Builder.

## Pages
${pagesList}

## API Endpoints
The following API endpoints were detected in your page designs:
${endpointsList}

These endpoints are handled by \`ApiDataController.java\`. The controller includes
sample data that matches the structure expected by your Repeater components.

**To connect to your actual data source:**
1. Modify \`ApiDataController.java\` to call your service layer
2. Implement data fetching in \`DataService.java\`
3. Add JPA repositories for database access (see pom.xml for dependencies)

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
- Server port
- Database connection (uncomment and configure)
- External API URLs
- Logging settings

## Project Structure
\`\`\`
src/
├── main/
│   ├── java/
│   │   └── ${options.groupId.replace(/\./g, '/')}/${options.artifactId}/
│   │       ├── Application.java
│   │       ├── controller/
│   │       │   ├── PageController.java     # Thymeleaf page routes
│   │       │   └── ApiDataController.java  # API data endpoints
│   │       └── service/
│   │           ├── PageDataService.java    # Page metadata loading
│   │           └── DataService.java        # Data fetching utilities
│   └── resources/
│       ├── application.properties
│       ├── pages/                    # Page data definitions
│       ├── templates/                # Thymeleaf templates
│       └── static/
│           ├── css/
│           ├── js/
│           └── images/               # Exported images
\`\`\`

## Customizing API Endpoints

The \`ApiDataController\` provides sample data out of the box. To connect to real data:

### Option 1: Database (JPA)
1. Uncomment JPA dependencies in \`pom.xml\`
2. Configure database in \`application.properties\`
3. Create Entity classes and Repositories
4. Inject repositories into ApiDataController

### Option 2: External API
1. Use the \`DataService.fetchFromExternalApi()\` method
2. Configure the external API URL in application.properties
3. Call the service from your controller methods

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

  // IMPORTANT: Collect and add images FIRST, before generating templates
  // This populates the imageUrlMap which is used during template generation
  console.log('[Thymeleaf Export] Collecting images from pages...');
  imageUrlMap = await collectAndAddImages(pages, zip);
  console.log(`[Thymeleaf Export] Collected ${imageUrlMap.size} images`);

  // Collect API endpoints used by Repeater components
  console.log('[Thymeleaf Export] Analyzing API data sources...');
  const apiEndpoints = collectApiEndpoints(pages);
  console.log(`[Thymeleaf Export] Found ${apiEndpoints.length} API endpoints`);

  // Generate pom.xml (with Lombok dependency for API controllers)
  zip.file('pom.xml', generatePomXmlWithLombok(mergedOptions, apiEndpoints.length > 0));

  // Generate Application.java
  // Use sanitized package name for directory path (hyphens are invalid in Java packages)
  const sanitizedArtifactId = toJavaPackageName(mergedOptions.artifactId);
  const javaBasePath = `src/main/java/${mergedOptions.groupId.replace(/\./g, '/')}/${sanitizedArtifactId}`;
  zip.file(`${javaBasePath}/Application.java`, generateApplicationJava(mergedOptions));

  // Generate PageController.java
  zip.file(`${javaBasePath}/controller/PageController.java`, generatePageController(pages, mergedOptions));

  // Generate PageDataService.java
  zip.file(`${javaBasePath}/service/PageDataService.java`, generatePageDataService(mergedOptions));

  // Generate API controllers if API endpoints are detected
  if (apiEndpoints.length > 0) {
    console.log('[Thymeleaf Export] Generating API controllers...');

    // Generate ApiDataController.java
    zip.file(
      `${javaBasePath}/controller/ApiDataController.java`,
      generateApiDataController(apiEndpoints, mergedOptions)
    );

    // Generate DataService.java for external API/database access
    zip.file(
      `${javaBasePath}/service/DataService.java`,
      generateDataService(apiEndpoints, mergedOptions)
    );

    console.log('[Thymeleaf Export] API controllers generated successfully');
  }

  // Generate application.properties
  zip.file('src/main/resources/application.properties', generateApplicationProperties(mergedOptions));

  // Generate Thymeleaf templates
  for (const page of pages) {
    const templateName = page.pageName.toLowerCase().replace(/[^a-z0-9]/g, '-');
    zip.file(`src/main/resources/templates/${templateName}.html`, generateThymeleafTemplate(page));

    // Collect component-level data sources (e.g., Repeater static data)
    const componentDataSources = collectComponentDataSources(page.definition.components);

    // Merge page-level and component-level data sources
    const allDataSources = {
      ...(page.definition.dataContext?.dataSources || {}),
      ...componentDataSources,
    };

    // Generate page definition JSON for runtime
    zip.file(`src/main/resources/pages/${templateName}.json`, JSON.stringify({
      pageName: page.pageName,
      title: page.definition.pageName,
      description: '',
      path: page.path,
      dataSources: allDataSources,
    }, null, 2));
  }

  // Generate static assets
  zip.file('src/main/resources/static/css/styles.css', generateBaseCSS());
  zip.file('src/main/resources/static/js/main.js', generateBaseJS());

  // Generate README (with API documentation if endpoints exist)
  if (apiEndpoints.length > 0) {
    zip.file('README.md', generateReadmeWithApiDocs(mergedOptions, pages, apiEndpoints));
  } else {
    zip.file('README.md', generateReadme(mergedOptions, pages));
  }

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
