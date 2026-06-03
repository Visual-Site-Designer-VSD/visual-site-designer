import React from 'react';
import { RendererRegistry } from '../services/RendererRegistry';

interface ComponentInstance {
  componentId: string;
  instanceId: string;
  props: Record<string, any>;
  styles: Record<string, any>;
  children?: ComponentInstance[];
  events?: any[];
  templateBindings?: Record<string, string>;
}

interface ComponentRendererProps {
  component: ComponentInstance;
  dataSources: Record<string, any>;
}

function resolveTemplate(template: string, dataSources: Record<string, any>): string {
  return template.replace(/\{\{([^}]+)\}\}/g, (_, path) => {
    return getNestedValue(dataSources, path.trim()) ?? '';
  });
}

function getNestedValue(obj: any, path: string): any {
  return path.split('.').reduce((o: any, k: string) => o && o[k], obj);
}

function getLayoutStyles(layoutMode: string, props: Record<string, any>): React.CSSProperties {
  const base: React.CSSProperties = { display: 'flex', boxSizing: 'border-box' };
  const gap = props.gap || '8px';
  switch (layoutMode) {
    case 'flex-row': return { ...base, flexDirection: 'row', gap };
    case 'flex-column': return { ...base, flexDirection: 'column', gap };
    case 'flex-wrap': return { ...base, flexDirection: 'row', flexWrap: 'wrap', gap };
    case 'grid-2col': return { display: 'grid', gridTemplateColumns: '1fr 1fr', gap };
    case 'grid-3col': return { display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap };
    case 'grid-4col': return { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap };
    default: return { ...base, flexDirection: 'column', gap };
  }
}

function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

function renderChildren(children: ComponentInstance[] | undefined, dataSources: Record<string, any>) {
  if (!children || children.length === 0) return null;
  return children.map(child => (
    <ComponentRenderer key={child.instanceId} component={child} dataSources={dataSources} />
  ));
}

export default function ComponentRenderer({ component, dataSources }: ComponentRendererProps) {
  const { componentId, props, styles, children, instanceId, templateBindings, events } = component;
  const style = styles || {};

  // Resolve template bindings
  const resolvedProps = { ...props };
  if (templateBindings && dataSources) {
    for (const [propName, template] of Object.entries(templateBindings)) {
      if (typeof template === 'string' && template.includes('{{')) {
        resolvedProps[propName] = resolveTemplate(template, dataSources);
      }
    }
  }

  // Check for plugin-rendered component
  const pluginRenderer = RendererRegistry.get(componentId);
  if (pluginRenderer) {
    return React.createElement(pluginRenderer, {
      key: instanceId,
      ...resolvedProps,
      style,
      children: renderChildren(children, dataSources),
    });
  }

  // Built-in component rendering
  switch (componentId) {
    case 'Label':
      return renderLabel(instanceId, resolvedProps, style);
    case 'Button':
      return renderButton(instanceId, resolvedProps, style, events);
    case 'Image':
      return renderImage(instanceId, resolvedProps, style);
    case 'Container':
    case 'ScrollableContainer':
      return renderContainer(instanceId, resolvedProps, style, children, dataSources);
    case 'Navbar':
    case 'NavbarDefault':
    case 'NavbarCentered':
    case 'NavbarMinimal':
    case 'NavbarDark':
    case 'NavbarGlass':
    case 'NavbarSticky':
      return renderNavbar(instanceId, resolvedProps, style);
    default:
      return renderGeneric(instanceId, componentId, resolvedProps, style, children, dataSources);
  }
}

function renderLabel(key: string, props: Record<string, any>, style: React.CSSProperties) {
  const tagMap: Record<string, string> = {
    h1: 'h1', h2: 'h2', h3: 'h3', h4: 'h4', h5: 'h5', h6: 'h6',
    paragraph: 'p', span: 'span', label: 'label',
  };
  const Tag = (tagMap[props.variant] || 'span') as keyof JSX.IntrinsicElements;
  return <Tag key={key} style={style} className="component label">{props.text || ''}</Tag>;
}

function renderButton(key: string, props: Record<string, any>, style: React.CSSProperties, events?: any[]) {
  const clickEvent = events?.find(e => e.eventType === 'onClick');
  const onClick = () => {
    if (clickEvent?.action?.type === 'navigate' && clickEvent.action.config?.url) {
      navigateTo(clickEvent.action.config.url);
    }
  };

  if (clickEvent?.action?.type === 'navigate') {
    return (
      <a key={key} style={style} className="component button"
         href={clickEvent.action.config.url}
         onClick={e => { e.preventDefault(); onClick(); }}>
        {props.text || 'Click Me'}
      </a>
    );
  }
  return (
    <button key={key} style={style} className="component button" onClick={onClick}>
      {props.text || 'Click Me'}
    </button>
  );
}

function renderImage(key: string, props: Record<string, any>, style: React.CSSProperties) {
  const src = props.src || props.url || '';
  const alt = props.alt || '';
  const objectFit = props.objectFit || 'cover';
  const objectPosition = props.objectPosition || 'center';
  const containerStyle: React.CSSProperties = { ...style, position: 'relative', overflow: 'hidden' };
  const imgStyle: React.CSSProperties = { width: '100%', height: '100%', objectFit, objectPosition };

  return (
    <div key={key} style={containerStyle} className="component image-container">
      <img src={src} alt={alt} style={imgStyle} loading="lazy" />
    </div>
  );
}

function renderContainer(
  key: string, props: Record<string, any>, style: React.CSSProperties,
  children: ComponentInstance[] | undefined, dataSources: Record<string, any>,
) {
  const layoutMode = props.layoutMode || 'flex-column';
  const layoutStyles = getLayoutStyles(layoutMode, props);
  const mergedStyle = { ...layoutStyles, ...style };

  return (
    <div key={key} style={mergedStyle} className="component container">
      {renderChildren(children, dataSources)}
    </div>
  );
}

function renderNavbar(key: string, props: Record<string, any>, style: React.CSSProperties) {
  const brandName = props.brandName || props.brand || 'Site';
  const items = props.items || props.navItems || [];
  const navStyle: React.CSSProperties = {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '12px 24px', ...style,
  };

  return (
    <nav key={key} style={navStyle} className="component navbar">
      <a href="/" className="navbar-brand"
         style={{ fontWeight: 'bold', fontSize: '1.25rem', textDecoration: 'none', color: 'inherit' }}
         onClick={e => { e.preventDefault(); navigateTo('/'); }}>
        {brandName}
      </a>
      <div style={{ display: 'flex', gap: '16px' }}>
        {items.map((item: any, i: number) => (
          <a key={i} href={item.url || item.href || '#'}
             style={{ textDecoration: 'none', color: 'inherit' }}
             onClick={e => {
               if (item.url || item.href) { e.preventDefault(); navigateTo(item.url || item.href); }
             }}>
            {item.label || item.text || ''}
          </a>
        ))}
      </div>
    </nav>
  );
}

function renderGeneric(
  key: string, componentId: string, props: Record<string, any>,
  style: React.CSSProperties, children: ComponentInstance[] | undefined,
  dataSources: Record<string, any>,
) {
  return (
    <div key={key} style={style} className={`component ${componentId.toLowerCase()}`}>
      {renderChildren(children, dataSources)}
    </div>
  );
}
