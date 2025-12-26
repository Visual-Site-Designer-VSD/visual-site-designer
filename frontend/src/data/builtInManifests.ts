/**
 * Built-in Component Manifests
 *
 * These manifests define the configurable properties for built-in frontend components
 * that don't come from backend plugins. They are used by the PropertiesPanel to show
 * property editors for components like Label, Button, Container, etc.
 */

import { ComponentManifest, PropType } from '../types/builder';

/**
 * Label component manifest
 */
const labelManifest: ComponentManifest = {
  componentId: 'Label',
  displayName: 'Label',
  category: 'ui',
  icon: '📝',
  description: 'Text label supporting various HTML elements (h1-h6, p, span)',
  defaultProps: {
    text: 'Label Text',
    htmlTag: 'p',
    textAlign: 'left',
    truncate: false,
    maxLines: 0,
  },
  defaultStyles: {
    color: '#333333',
  },
  reactComponentPath: 'renderers/LabelRenderer',
  configurableProps: [
    {
      name: 'text',
      type: PropType.TEXTAREA,
      label: 'Text Content',
      defaultValue: 'Label Text',
      helpText: 'The text to display',
    },
    {
      name: 'htmlTag',
      type: PropType.SELECT,
      label: 'HTML Element',
      defaultValue: 'p',
      options: ['h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'span', 'caption'],
      helpText: 'Choose the HTML element type',
    },
    {
      name: 'textAlign',
      type: PropType.SELECT,
      label: 'Text Align',
      defaultValue: 'left',
      options: ['left', 'center', 'right', 'justify'],
      helpText: 'Text alignment within the element',
    },
    {
      name: 'truncate',
      type: PropType.BOOLEAN,
      label: 'Truncate Text',
      defaultValue: false,
      helpText: 'Truncate text with ellipsis if it overflows',
    },
    {
      name: 'maxLines',
      type: PropType.NUMBER,
      label: 'Max Lines',
      defaultValue: 0,
      min: 0,
      max: 10,
      helpText: 'Maximum number of lines to display (0 = unlimited)',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 50,
    minHeight: 20,
    maxWidth: 2000,
    maxHeight: 1000,
  },
  pluginId: 'core-ui',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Button component manifest
 */
const buttonManifest: ComponentManifest = {
  componentId: 'Button',
  displayName: 'Button',
  category: 'ui',
  icon: '🔘',
  description: 'Interactive button with various styles and sizes',
  defaultProps: {
    text: 'Click Me',
    variant: 'primary',
    size: 'medium',
    disabled: false,
    fullWidth: false,
  },
  defaultStyles: {},
  reactComponentPath: 'renderers/ButtonRenderer',
  configurableProps: [
    {
      name: 'text',
      type: PropType.STRING,
      label: 'Button Text',
      defaultValue: 'Click Me',
      helpText: 'Text displayed on the button',
    },
    {
      name: 'variant',
      type: PropType.SELECT,
      label: 'Variant',
      defaultValue: 'primary',
      options: ['primary', 'secondary', 'success', 'danger', 'warning', 'outline', 'outline-light', 'link'],
      helpText: 'Button style variant',
    },
    {
      name: 'size',
      type: PropType.SELECT,
      label: 'Size',
      defaultValue: 'medium',
      options: ['small', 'medium', 'large'],
      helpText: 'Button size',
    },
    {
      name: 'disabled',
      type: PropType.BOOLEAN,
      label: 'Disabled',
      defaultValue: false,
      helpText: 'Disable button interactions',
    },
    {
      name: 'fullWidth',
      type: PropType.BOOLEAN,
      label: 'Full Width',
      defaultValue: false,
      helpText: 'Make button span full container width',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 60,
    minHeight: 30,
    maxWidth: 500,
    maxHeight: 100,
  },
  pluginId: 'core-ui',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Container component manifest
 */
const containerManifest: ComponentManifest = {
  componentId: 'Container',
  displayName: 'Container',
  category: 'layout',
  icon: '📦',
  description: 'Layout container for organizing child components',
  defaultProps: {
    layoutType: 'flex-column',
    layoutMode: 'flex-column',
    gap: '16px',
    padding: '20px',
    maxWidth: '1200px',
    centerContent: true,
    alignItems: 'stretch',
    justifyContent: 'flex-start',
  },
  defaultStyles: {
    backgroundColor: '#ffffff',
    borderRadius: '8px',
  },
  reactComponentPath: 'renderers/ContainerRenderer',
  configurableProps: [
    {
      name: 'layoutMode',
      type: PropType.SELECT,
      label: 'Layout Mode',
      defaultValue: 'flex-column',
      options: ['flex-column', 'flex-row', 'flex-wrap', 'grid-2col', 'grid-3col', 'grid-4col', 'grid-auto'],
      helpText: 'How child components are arranged',
    },
    {
      name: 'gap',
      type: PropType.STRING,
      label: 'Gap',
      defaultValue: '16px',
      helpText: 'Space between child components (e.g., 16px, 1rem)',
    },
    {
      name: 'padding',
      type: PropType.STRING,
      label: 'Padding',
      defaultValue: '20px',
      helpText: 'Inner padding of the container',
    },
    {
      name: 'maxWidth',
      type: PropType.STRING,
      label: 'Max Width',
      defaultValue: '1200px',
      helpText: 'Maximum width of the container (use "none" for no limit)',
    },
    {
      name: 'centerContent',
      type: PropType.BOOLEAN,
      label: 'Center Content',
      defaultValue: true,
      helpText: 'Center the container horizontally on the page',
    },
    {
      name: 'alignItems',
      type: PropType.SELECT,
      label: 'Align Items',
      defaultValue: 'stretch',
      options: ['flex-start', 'center', 'flex-end', 'stretch', 'baseline'],
      helpText: 'Cross-axis alignment of children',
    },
    {
      name: 'justifyContent',
      type: PropType.SELECT,
      label: 'Justify Content',
      defaultValue: 'flex-start',
      options: ['flex-start', 'center', 'flex-end', 'space-between', 'space-around', 'space-evenly'],
      helpText: 'Main-axis alignment of children',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 100,
    minHeight: 50,
    maxWidth: 3000,
    maxHeight: 5000,
  },
  pluginId: 'core-ui',
  pluginVersion: '1.0.0',
  canHaveChildren: true,
};

/**
 * Textbox component manifest
 */
const textboxManifest: ComponentManifest = {
  componentId: 'Textbox',
  displayName: 'Text Input',
  category: 'form',
  icon: '✏️',
  description: 'Text input field with support for single or multi-line input',
  defaultProps: {
    placeholder: 'Enter text...',
    type: 'text',
    disabled: false,
    readOnly: false,
    required: false,
    multiline: false,
    rows: 3,
    maxLength: 0,
    label: '',
    showLabel: false,
  },
  defaultStyles: {},
  reactComponentPath: 'renderers/TextboxRenderer',
  configurableProps: [
    {
      name: 'placeholder',
      type: PropType.STRING,
      label: 'Placeholder',
      defaultValue: 'Enter text...',
      helpText: 'Placeholder text shown when empty',
    },
    {
      name: 'label',
      type: PropType.STRING,
      label: 'Label',
      defaultValue: '',
      helpText: 'Label text for the input field',
    },
    {
      name: 'showLabel',
      type: PropType.BOOLEAN,
      label: 'Show Label',
      defaultValue: false,
      helpText: 'Display the label above the input',
    },
    {
      name: 'type',
      type: PropType.SELECT,
      label: 'Input Type',
      defaultValue: 'text',
      options: ['text', 'email', 'password', 'tel', 'url', 'number'],
      helpText: 'Type of input field',
    },
    {
      name: 'multiline',
      type: PropType.BOOLEAN,
      label: 'Multiline',
      defaultValue: false,
      helpText: 'Use a textarea for multi-line input',
    },
    {
      name: 'rows',
      type: PropType.NUMBER,
      label: 'Rows',
      defaultValue: 3,
      min: 1,
      max: 20,
      helpText: 'Number of visible rows (for multiline)',
    },
    {
      name: 'maxLength',
      type: PropType.NUMBER,
      label: 'Max Length',
      defaultValue: 0,
      min: 0,
      helpText: 'Maximum character length (0 = unlimited)',
    },
    {
      name: 'required',
      type: PropType.BOOLEAN,
      label: 'Required',
      defaultValue: false,
      helpText: 'Mark field as required',
    },
    {
      name: 'disabled',
      type: PropType.BOOLEAN,
      label: 'Disabled',
      defaultValue: false,
      helpText: 'Disable the input field',
    },
    {
      name: 'readOnly',
      type: PropType.BOOLEAN,
      label: 'Read Only',
      defaultValue: false,
      helpText: 'Make the field read-only',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 100,
    minHeight: 30,
    maxWidth: 1000,
    maxHeight: 500,
  },
  pluginId: 'core-ui',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Navbar Default component manifest
 */
const navbarDefaultManifest: ComponentManifest = {
  componentId: 'NavbarDefault',
  displayName: 'Navbar (Default)',
  category: 'navigation',
  icon: '🧭',
  description: 'Default navbar with brand on left and navigation links on right',
  defaultProps: {
    brandText: 'My Site',
    navItems: [
      { label: 'Home', href: '/', active: true },
      { label: 'About', href: '/about', active: false },
      { label: 'Services', href: '/services', active: false },
      { label: 'Contact', href: '/contact', active: false },
    ],
    layout: 'default',
  },
  defaultStyles: {
    backgroundColor: '#ffffff',
    textColor: '#333333',
    accentColor: '#007bff',
  },
  reactComponentPath: 'renderers/NavbarDefaultRenderer',
  configurableProps: [
    {
      name: 'brandText',
      type: PropType.STRING,
      label: 'Brand Text',
      defaultValue: 'My Site',
      helpText: 'Text displayed as the brand/logo',
    },
    {
      name: 'navItems',
      type: PropType.JSON,
      label: 'Navigation Items',
      defaultValue: [],
      helpText: 'Array of navigation items with label, href, and active properties',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 300,
    minHeight: 50,
    maxWidth: 3000,
    maxHeight: 150,
  },
  pluginId: 'core-navbar',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Navbar Centered component manifest
 */
const navbarCenteredManifest: ComponentManifest = {
  componentId: 'NavbarCentered',
  displayName: 'Navbar (Centered)',
  category: 'navigation',
  icon: '🧭',
  description: 'Centered navbar with brand in the middle',
  defaultProps: {
    brandText: 'My Site',
    navItems: [
      { label: 'Home', href: '/', active: true },
      { label: 'About', href: '/about', active: false },
      { label: 'Services', href: '/services', active: false },
      { label: 'Contact', href: '/contact', active: false },
    ],
    layout: 'centered',
  },
  defaultStyles: {
    backgroundColor: '#ffffff',
    textColor: '#333333',
    accentColor: '#007bff',
  },
  reactComponentPath: 'renderers/NavbarCenteredRenderer',
  configurableProps: [
    {
      name: 'brandText',
      type: PropType.STRING,
      label: 'Brand Text',
      defaultValue: 'My Site',
      helpText: 'Text displayed as the brand/logo',
    },
    {
      name: 'navItems',
      type: PropType.JSON,
      label: 'Navigation Items',
      defaultValue: [],
      helpText: 'Array of navigation items with label, href, and active properties',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 300,
    minHeight: 50,
    maxWidth: 3000,
    maxHeight: 150,
  },
  pluginId: 'core-navbar',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Navbar Dark component manifest
 */
const navbarDarkManifest: ComponentManifest = {
  componentId: 'NavbarDark',
  displayName: 'Navbar (Dark)',
  category: 'navigation',
  icon: '🧭',
  description: 'Dark themed navbar',
  defaultProps: {
    brandText: 'My Site',
    navItems: [
      { label: 'Home', href: '/', active: true },
      { label: 'About', href: '/about', active: false },
      { label: 'Services', href: '/services', active: false },
      { label: 'Contact', href: '/contact', active: false },
    ],
    layout: 'default',
  },
  defaultStyles: {
    backgroundColor: '#1a1a2e',
    textColor: '#ffffff',
    accentColor: '#4dabf7',
  },
  reactComponentPath: 'renderers/NavbarDarkRenderer',
  configurableProps: [
    {
      name: 'brandText',
      type: PropType.STRING,
      label: 'Brand Text',
      defaultValue: 'My Site',
      helpText: 'Text displayed as the brand/logo',
    },
    {
      name: 'navItems',
      type: PropType.JSON,
      label: 'Navigation Items',
      defaultValue: [],
      helpText: 'Array of navigation items with label, href, and active properties',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 300,
    minHeight: 50,
    maxWidth: 3000,
    maxHeight: 150,
  },
  pluginId: 'core-navbar',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Navbar Minimal component manifest
 */
const navbarMinimalManifest: ComponentManifest = {
  componentId: 'NavbarMinimal',
  displayName: 'Navbar (Minimal)',
  category: 'navigation',
  icon: '🧭',
  description: 'Minimalist navbar with clean styling',
  defaultProps: {
    brandText: 'My Site',
    navItems: [
      { label: 'Home', href: '/', active: true },
      { label: 'About', href: '/about', active: false },
      { label: 'Services', href: '/services', active: false },
      { label: 'Contact', href: '/contact', active: false },
    ],
    layout: 'default',
  },
  defaultStyles: {
    backgroundColor: 'transparent',
    textColor: '#333333',
    accentColor: '#007bff',
  },
  reactComponentPath: 'renderers/NavbarMinimalRenderer',
  configurableProps: [
    {
      name: 'brandText',
      type: PropType.STRING,
      label: 'Brand Text',
      defaultValue: 'My Site',
      helpText: 'Text displayed as the brand/logo',
    },
    {
      name: 'navItems',
      type: PropType.JSON,
      label: 'Navigation Items',
      defaultValue: [],
      helpText: 'Array of navigation items with label, href, and active properties',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 300,
    minHeight: 50,
    maxWidth: 3000,
    maxHeight: 150,
  },
  pluginId: 'core-navbar',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Navbar Glass component manifest
 */
const navbarGlassManifest: ComponentManifest = {
  componentId: 'NavbarGlass',
  displayName: 'Navbar (Glass)',
  category: 'navigation',
  icon: '🧭',
  description: 'Glassmorphism styled navbar with blur effect',
  defaultProps: {
    brandText: 'My Site',
    navItems: [
      { label: 'Home', href: '/', active: true },
      { label: 'About', href: '/about', active: false },
      { label: 'Services', href: '/services', active: false },
      { label: 'Contact', href: '/contact', active: false },
    ],
    layout: 'default',
  },
  defaultStyles: {
    backgroundColor: 'rgba(255, 255, 255, 0.7)',
    textColor: '#333333',
    accentColor: '#007bff',
  },
  reactComponentPath: 'renderers/NavbarGlassRenderer',
  configurableProps: [
    {
      name: 'brandText',
      type: PropType.STRING,
      label: 'Brand Text',
      defaultValue: 'My Site',
      helpText: 'Text displayed as the brand/logo',
    },
    {
      name: 'navItems',
      type: PropType.JSON,
      label: 'Navigation Items',
      defaultValue: [],
      helpText: 'Array of navigation items with label, href, and active properties',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 300,
    minHeight: 50,
    maxWidth: 3000,
    maxHeight: 150,
  },
  pluginId: 'core-navbar',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Navbar Sticky component manifest
 */
const navbarStickyManifest: ComponentManifest = {
  componentId: 'NavbarSticky',
  displayName: 'Navbar (Sticky)',
  category: 'navigation',
  icon: '🧭',
  description: 'Sticky navbar that stays at the top when scrolling',
  defaultProps: {
    brandText: 'My Site',
    navItems: [
      { label: 'Home', href: '/', active: true },
      { label: 'About', href: '/about', active: false },
      { label: 'Services', href: '/services', active: false },
      { label: 'Contact', href: '/contact', active: false },
    ],
    layout: 'default',
  },
  defaultStyles: {
    backgroundColor: '#ffffff',
    textColor: '#333333',
    accentColor: '#007bff',
  },
  reactComponentPath: 'renderers/NavbarStickyRenderer',
  configurableProps: [
    {
      name: 'brandText',
      type: PropType.STRING,
      label: 'Brand Text',
      defaultValue: 'My Site',
      helpText: 'Text displayed as the brand/logo',
    },
    {
      name: 'navItems',
      type: PropType.JSON,
      label: 'Navigation Items',
      defaultValue: [],
      helpText: 'Array of navigation items with label, href, and active properties',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 300,
    minHeight: 50,
    maxWidth: 3000,
    maxHeight: 150,
  },
  pluginId: 'core-navbar',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * Image component manifest
 */
const imageManifest: ComponentManifest = {
  componentId: 'Image',
  displayName: 'Image',
  category: 'media',
  icon: '🖼️',
  description: 'Image component with placeholder support and various display options',
  defaultProps: {
    src: '',
    alt: 'Image',
    objectFit: 'cover',
    objectPosition: 'center',
    aspectRatio: 'auto',
    borderRadius: '0px',
    placeholder: 'icon',
    placeholderColor: '#e9ecef',
    caption: '',
    showCaption: false,
    lazyLoad: true,
  },
  defaultStyles: {},
  reactComponentPath: 'renderers/ImageRenderer',
  configurableProps: [
    {
      name: 'src',
      type: PropType.URL,
      label: 'Image URL',
      defaultValue: '',
      helpText: 'URL of the image to display',
    },
    {
      name: 'alt',
      type: PropType.STRING,
      label: 'Alt Text',
      defaultValue: 'Image',
      helpText: 'Alternative text for accessibility',
    },
    {
      name: 'aspectRatio',
      type: PropType.SELECT,
      label: 'Aspect Ratio',
      defaultValue: 'auto',
      options: ['auto', '1:1', '4:3', '16:9', '3:2', '2:3', '3:4', '9:16', 'circle'],
      helpText: 'Fixed aspect ratio for the image',
    },
    {
      name: 'objectFit',
      type: PropType.SELECT,
      label: 'Object Fit',
      defaultValue: 'cover',
      options: ['cover', 'contain', 'fill', 'none', 'scale-down'],
      helpText: 'How the image should fit within its container',
    },
    {
      name: 'objectPosition',
      type: PropType.SELECT,
      label: 'Object Position',
      defaultValue: 'center',
      options: ['center', 'top', 'bottom', 'left', 'right', 'top left', 'top right', 'bottom left', 'bottom right'],
      helpText: 'Position of the image within its container',
    },
    {
      name: 'borderRadius',
      type: PropType.STRING,
      label: 'Border Radius',
      defaultValue: '0px',
      helpText: 'Rounded corners (e.g., 8px, 50%)',
    },
    {
      name: 'placeholder',
      type: PropType.SELECT,
      label: 'Placeholder Type',
      defaultValue: 'icon',
      options: ['icon', 'color'],
      helpText: 'What to show when image is loading or missing',
    },
    {
      name: 'placeholderColor',
      type: PropType.COLOR,
      label: 'Placeholder Color',
      defaultValue: '#e9ecef',
      helpText: 'Background color for placeholder',
    },
    {
      name: 'caption',
      type: PropType.STRING,
      label: 'Caption',
      defaultValue: '',
      helpText: 'Caption text below the image',
    },
    {
      name: 'showCaption',
      type: PropType.BOOLEAN,
      label: 'Show Caption',
      defaultValue: false,
      helpText: 'Display the caption below the image',
    },
    {
      name: 'lazyLoad',
      type: PropType.BOOLEAN,
      label: 'Lazy Load',
      defaultValue: true,
      helpText: 'Load image only when visible in viewport',
    },
  ],
  configurableStyles: [],
  sizeConstraints: {
    minWidth: 50,
    minHeight: 50,
    maxWidth: 2000,
    maxHeight: 2000,
  },
  pluginId: 'core-ui',
  pluginVersion: '1.0.0',
  canHaveChildren: false,
};

/**
 * All built-in manifests indexed by key (pluginId:componentId)
 */
export const builtInManifests: Record<string, ComponentManifest> = {
  // Core UI components
  'core-ui:Label': labelManifest,
  'core-ui:Button': buttonManifest,
  'core-ui:Container': containerManifest,
  'core-ui:Textbox': textboxManifest,
  'core-ui:Image': imageManifest,

  // Core Navbar components
  'core-navbar:NavbarDefault': navbarDefaultManifest,
  'core-navbar:NavbarCentered': navbarCenteredManifest,
  'core-navbar:NavbarDark': navbarDarkManifest,
  'core-navbar:NavbarMinimal': navbarMinimalManifest,
  'core-navbar:NavbarGlass': navbarGlassManifest,
  'core-navbar:NavbarSticky': navbarStickyManifest,
};

/**
 * Get a built-in manifest by pluginId and componentId
 */
export const getBuiltInManifest = (pluginId: string, componentId: string): ComponentManifest | null => {
  const key = `${pluginId}:${componentId}`;
  return builtInManifests[key] || null;
};

/**
 * Check if a component has a built-in manifest
 */
export const hasBuiltInManifest = (pluginId: string, componentId: string): boolean => {
  const key = `${pluginId}:${componentId}`;
  return key in builtInManifests;
};

/**
 * Get all built-in manifest keys
 */
export const getBuiltInManifestKeys = (): string[] => {
  return Object.keys(builtInManifests);
};
