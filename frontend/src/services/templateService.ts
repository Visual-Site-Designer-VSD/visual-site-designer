/**
 * Template Service - Resolves {{variable}} syntax in component props
 * Part of Phase 1: Data Binding Infrastructure
 */

/**
 * Template node types
 */
export type TemplateNodeType = 'text' | 'variable';

/**
 * Parsed template node
 */
export interface TemplateNode {
  type: TemplateNodeType;
  value: string;
  /** For variables: the parsed path segments */
  path?: string[];
  /** For variables: optional filters/transforms */
  filters?: string[];
}

/**
 * Data context for resolving variables
 */
export interface DataContext {
  /** Page-level data sources */
  dataSources?: Record<string, any>;
  /** Current item in iterator context */
  item?: any;
  /** Current index in iterator context */
  index?: number;
  /** Shared data across components */
  sharedData?: Record<string, any>;
  /** User context (from authentication) */
  user?: any;
}

/**
 * Variable pattern: {{variable.path}} or {{variable.path | filter}}
 */
const VARIABLE_PATTERN = /\{\{([^}]+)\}\}/g;

/**
 * Parse a template string into nodes
 * @param template The template string containing {{variable}} syntax
 * @returns Array of parsed template nodes
 */
export function parseTemplate(template: string): TemplateNode[] {
  if (typeof template !== 'string') {
    return [{ type: 'text', value: String(template) }];
  }

  const nodes: TemplateNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  // Reset regex state
  VARIABLE_PATTERN.lastIndex = 0;

  while ((match = VARIABLE_PATTERN.exec(template)) !== null) {
    // Add text before the variable
    if (match.index > lastIndex) {
      nodes.push({
        type: 'text',
        value: template.slice(lastIndex, match.index),
      });
    }

    // Parse the variable expression
    const expression = match[1].trim();
    const { path, filters } = parseVariableExpression(expression);

    nodes.push({
      type: 'variable',
      value: expression,
      path,
      filters,
    });

    lastIndex = match.index + match[0].length;
  }

  // Add remaining text
  if (lastIndex < template.length) {
    nodes.push({
      type: 'text',
      value: template.slice(lastIndex),
    });
  }

  return nodes;
}

/**
 * Parse a variable expression (e.g., "user.name | uppercase")
 */
function parseVariableExpression(expression: string): { path: string[]; filters: string[] } {
  const parts = expression.split('|').map((p) => p.trim());
  const pathPart = parts[0];
  const filters = parts.slice(1);

  // Parse the path (handle dots and brackets)
  const path = parseVariablePath(pathPart);

  return { path, filters };
}

/**
 * Parse a variable path into segments
 * Handles: "user.name", "items[0].title", "data['key']"
 */
function parseVariablePath(pathStr: string): string[] {
  const segments: string[] = [];
  let current = '';
  let i = 0;

  while (i < pathStr.length) {
    const char = pathStr[i];

    if (char === '.') {
      if (current) {
        segments.push(current);
        current = '';
      }
      i++;
    } else if (char === '[') {
      if (current) {
        segments.push(current);
        current = '';
      }
      // Find closing bracket
      const closeBracket = pathStr.indexOf(']', i);
      if (closeBracket === -1) {
        throw new Error(`Unclosed bracket in path: ${pathStr}`);
      }
      let indexPart = pathStr.slice(i + 1, closeBracket);
      // Remove quotes if present
      if ((indexPart.startsWith("'") && indexPart.endsWith("'")) ||
          (indexPart.startsWith('"') && indexPart.endsWith('"'))) {
        indexPart = indexPart.slice(1, -1);
      }
      segments.push(indexPart);
      i = closeBracket + 1;
    } else {
      current += char;
      i++;
    }
  }

  if (current) {
    segments.push(current);
  }

  return segments;
}

/**
 * Extract variable names from a template string
 * @param template The template string
 * @returns Array of variable paths (e.g., ["user.name", "items[0].title"])
 */
export function extractVariables(template: string): string[] {
  const nodes = parseTemplate(template);
  return nodes
    .filter((node) => node.type === 'variable')
    .map((node) => node.value.split('|')[0].trim());
}

/**
 * Get a value from an object by path
 */
function getValueByPath(obj: any, path: string[]): any {
  let current = obj;

  for (const segment of path) {
    if (current === null || current === undefined) {
      return undefined;
    }

    // Handle array index
    if (Array.isArray(current) && /^\d+$/.test(segment)) {
      current = current[parseInt(segment, 10)];
    } else if (typeof current === 'object') {
      current = current[segment];
    } else {
      return undefined;
    }
  }

  return current;
}

/**
 * Apply a filter/transform to a value
 */
function applyFilter(value: any, filter: string): any {
  if (value === null || value === undefined) {
    return value;
  }

  const filterLower = filter.toLowerCase();

  switch (filterLower) {
    case 'uppercase':
      return String(value).toUpperCase();
    case 'lowercase':
      return String(value).toLowerCase();
    case 'capitalize':
      return String(value).charAt(0).toUpperCase() + String(value).slice(1);
    case 'trim':
      return String(value).trim();
    case 'number':
      return Number(value);
    case 'integer':
      return parseInt(String(value), 10);
    case 'float':
      return parseFloat(String(value));
    case 'boolean':
      return Boolean(value);
    case 'string':
      return String(value);
    case 'json':
      return JSON.stringify(value);
    case 'date':
      return new Date(value).toLocaleDateString();
    case 'datetime':
      return new Date(value).toLocaleString();
    case 'time':
      return new Date(value).toLocaleTimeString();
    case 'currency':
      return typeof value === 'number'
        ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
        : value;
    case 'percent':
      return typeof value === 'number'
        ? new Intl.NumberFormat('en-US', { style: 'percent', minimumFractionDigits: 0 }).format(value)
        : value;
    case 'length':
      return Array.isArray(value) ? value.length : String(value).length;
    case 'first':
      return Array.isArray(value) ? value[0] : value;
    case 'last':
      return Array.isArray(value) ? value[value.length - 1] : value;
    case 'reverse':
      return Array.isArray(value) ? [...value].reverse() : String(value).split('').reverse().join('');
    case 'sort':
      return Array.isArray(value) ? [...value].sort() : value;
    case 'unique':
      return Array.isArray(value) ? [...new Set(value)] : value;
    case 'keys':
      return typeof value === 'object' && value !== null ? Object.keys(value) : [];
    case 'values':
      return typeof value === 'object' && value !== null ? Object.values(value) : [];
    case 'default':
      // Default filter should be handled with a parameter, but for now return as-is
      return value;
    default:
      // Check for parameterized filters like "default:fallback" or "truncate:100"
      if (filter.includes(':')) {
        const [filterName, param] = filter.split(':');
        return applyParameterizedFilter(value, filterName.trim(), param.trim());
      }
      console.warn(`Unknown filter: ${filter}`);
      return value;
  }
}

/**
 * Apply a parameterized filter
 */
function applyParameterizedFilter(value: any, filterName: string, param: string): any {
  switch (filterName.toLowerCase()) {
    case 'default':
      return value === null || value === undefined || value === '' ? param : value;
    case 'truncate':
      const maxLength = parseInt(param, 10);
      if (isNaN(maxLength)) return value;
      const str = String(value);
      return str.length > maxLength ? str.slice(0, maxLength) + '...' : str;
    case 'round':
      const decimals = parseInt(param, 10) || 0;
      return typeof value === 'number' ? Number(value.toFixed(decimals)) : value;
    case 'pad':
      const padLength = parseInt(param, 10);
      return String(value).padStart(padLength, '0');
    case 'slice':
      const [start, end] = param.split(',').map(n => parseInt(n.trim(), 10));
      if (Array.isArray(value)) {
        return value.slice(start, end);
      }
      return String(value).slice(start, end);
    case 'replace':
      const [search, replacement] = param.split(',').map(s => s.trim());
      return String(value).replace(new RegExp(search, 'g'), replacement || '');
    case 'split':
      return String(value).split(param);
    case 'join':
      return Array.isArray(value) ? value.join(param) : value;
    case 'format':
      // Simple format string replacement
      if (typeof value === 'number') {
        return new Intl.NumberFormat('en-US', { maximumFractionDigits: parseInt(param, 10) || 2 }).format(value);
      }
      return value;
    default:
      console.warn(`Unknown parameterized filter: ${filterName}`);
      return value;
  }
}

/**
 * Resolve a single variable from context
 */
function resolveVariable(path: string[], filters: string[], context: DataContext): any {
  if (path.length === 0) {
    return undefined;
  }

  const rootKey = path[0];
  let value: any;

  // Resolve from different context sources
  switch (rootKey) {
    case 'item':
      // Current item in iterator context
      value = context.item;
      if (path.length > 1) {
        value = getValueByPath(value, path.slice(1));
      }
      break;
    case 'index':
      // Current index in iterator context
      value = context.index;
      break;
    case 'user':
      // User context
      value = context.user;
      if (path.length > 1) {
        value = getValueByPath(value, path.slice(1));
      }
      break;
    case 'shared':
      // Shared data
      value = context.sharedData;
      if (path.length > 1) {
        value = getValueByPath(value, path.slice(1));
      }
      break;
    default:
      // Try to resolve from dataSources first
      if (context.dataSources && rootKey in context.dataSources) {
        value = context.dataSources[rootKey];
        if (path.length > 1) {
          value = getValueByPath(value, path.slice(1));
        }
      } else if (context.item && typeof context.item === 'object') {
        // If in iterator context, try to resolve from item
        value = getValueByPath(context.item, path);
      } else {
        // Try to resolve from all context sources
        value = getValueByPath(context.dataSources, path) ??
                getValueByPath(context.sharedData, path) ??
                getValueByPath(context.item, path);
      }
  }

  // Apply filters
  for (const filter of filters) {
    value = applyFilter(value, filter);
  }

  return value;
}

/**
 * Resolve a template string with context
 * @param template The template string containing {{variable}} syntax
 * @param context The data context for variable resolution
 * @returns The resolved string
 */
export function resolveTemplate(template: string, context: DataContext): string {
  if (typeof template !== 'string') {
    return String(template);
  }

  const nodes = parseTemplate(template);

  return nodes
    .map((node) => {
      if (node.type === 'text') {
        return node.value;
      }

      const value = resolveVariable(node.path || [], node.filters || [], context);

      // Handle undefined/null
      if (value === undefined || value === null) {
        return '';
      }

      // Convert objects to JSON string for display
      if (typeof value === 'object') {
        return JSON.stringify(value);
      }

      return String(value);
    })
    .join('');
}

/**
 * Resolve template variables in component props
 * @param props The component props (may contain {{variable}} syntax)
 * @param context The data context for variable resolution
 * @returns Resolved props with template variables replaced
 */
export function resolveTemplateVariables(
  props: Record<string, any>,
  context: DataContext
): Record<string, any> {
  const resolved: Record<string, any> = {};

  for (const [key, value] of Object.entries(props)) {
    if (typeof value === 'string' && value.includes('{{')) {
      resolved[key] = resolveTemplate(value, context);
    } else if (Array.isArray(value)) {
      resolved[key] = value.map((item) => {
        if (typeof item === 'string' && item.includes('{{')) {
          return resolveTemplate(item, context);
        }
        if (typeof item === 'object' && item !== null) {
          return resolveTemplateVariables(item, context);
        }
        return item;
      });
    } else if (typeof value === 'object' && value !== null) {
      resolved[key] = resolveTemplateVariables(value, context);
    } else {
      resolved[key] = value;
    }
  }

  return resolved;
}

/**
 * Check if a string contains template variables
 */
export function hasTemplateVariables(str: string): boolean {
  if (typeof str !== 'string') return false;
  return VARIABLE_PATTERN.test(str);
}

/**
 * Get all unique variable paths from an object (recursively)
 */
export function getAllVariablePaths(obj: any): string[] {
  const variables = new Set<string>();

  function traverse(value: any) {
    if (typeof value === 'string') {
      const extracted = extractVariables(value);
      extracted.forEach((v) => variables.add(v));
    } else if (Array.isArray(value)) {
      value.forEach(traverse);
    } else if (typeof value === 'object' && value !== null) {
      Object.values(value).forEach(traverse);
    }
  }

  traverse(obj);
  return Array.from(variables);
}
