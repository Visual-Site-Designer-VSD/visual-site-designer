/**
 * TypeScript types for UI Templates
 */

import { ComponentInstance } from './builder';

/**
 * Template category types
 */
export type TemplateCategory =
  | 'hero'
  | 'features'
  | 'pricing'
  | 'testimonials'
  | 'contact'
  | 'footer'
  | 'header'
  | 'cta'
  | 'gallery'
  | 'team'
  | 'faq'
  | 'stats'
  | 'content';

/**
 * Template definition
 */
export interface UITemplate {
  /** Unique identifier for the template */
  id: string;
  /** Display name shown in the palette */
  name: string;
  /** Template category for grouping */
  category: TemplateCategory;
  /** Icon emoji for the template */
  icon: string;
  /** Brief description of the template */
  description: string;
  /** Preview image URL (optional) */
  previewImage?: string;
  /** Tags for search/filtering */
  tags: string[];
  /** The component structure that makes up this template */
  components: TemplateComponentDef[];
  /** Suggested grid span for the template container */
  suggestedSize: {
    columnSpan: number;
    rowSpan: number;
  };
}

/**
 * Template component definition (simplified ComponentInstance for templates)
 * These get converted to full ComponentInstance when dropped on canvas
 */
export interface TemplateComponentDef {
  /** Reference to the component type */
  pluginId: string;
  componentId: string;
  /** Component props */
  props: Record<string, any>;
  /** Component styles */
  styles: Record<string, string>;
  /** Relative position within the template (percentages or grid units) */
  relativePosition?: {
    row: number;
    column: number;
    rowSpan: number;
    columnSpan: number;
  };
  /** Child components (for containers) */
  children?: TemplateComponentDef[];
}

/**
 * Template group for displaying in palette
 */
export interface TemplateGroup {
  category: TemplateCategory;
  displayName: string;
  icon: string;
  templates: UITemplate[];
}
