import React, { useState } from 'react';
import { ComponentPalette } from './ComponentPalette';
import { TemplatePalette } from './TemplatePalette';
import { PageManager } from './PageManager';
import { ContextPanel } from './ContextPanel';
import { ComponentRegistryEntry, ComponentInstance } from '../../types/builder';
import { Page } from '../../types/site';
import './LeftSidebar.css';

type SidebarTab = 'pages' | 'components' | 'templates' | 'contexts';

interface LeftSidebarProps {
  siteId?: number | null;
  currentPageId?: number | null;
  onPageSelect?: (page: Page) => void;
  onPageCreate?: (page: Page) => void;
  onPageDelete?: (pageId: number) => void;
  onComponentDragStart?: (component: ComponentRegistryEntry) => void;
  onTemplateDrop?: (components: ComponentInstance[]) => void;
}

/**
 * Left Sidebar - Tabbed container for Pages, Components, and Templates palettes
 */
export const LeftSidebar: React.FC<LeftSidebarProps> = ({
  siteId,
  currentPageId,
  onPageSelect,
  onPageCreate,
  onPageDelete,
  onComponentDragStart,
  onTemplateDrop,
}) => {
  const [activeTab, setActiveTab] = useState<SidebarTab>('components');

  return (
    <div className="left-sidebar">
      {/* Tab Navigation */}
      <div className="sidebar-tabs">
        <button
          className={`sidebar-tab ${activeTab === 'pages' ? 'active' : ''}`}
          onClick={() => setActiveTab('pages')}
          title="Manage pages"
        >
          <span className="tab-icon">📑</span>
          <span className="tab-label">Pages</span>
        </button>
        <button
          className={`sidebar-tab ${activeTab === 'components' ? 'active' : ''}`}
          onClick={() => setActiveTab('components')}
          title="Drag components to canvas"
        >
          <span className="tab-icon">🧩</span>
          <span className="tab-label">Components</span>
        </button>
        <button
          className={`sidebar-tab ${activeTab === 'templates' ? 'active' : ''}`}
          onClick={() => setActiveTab('templates')}
          title="Apply page templates"
        >
          <span className="tab-icon">📐</span>
          <span className="tab-label">Templates</span>
        </button>
        <button
          className={`sidebar-tab ${activeTab === 'contexts' ? 'active' : ''}`}
          onClick={() => setActiveTab('contexts')}
          title="View active context providers"
        >
          <span className="tab-icon">🔗</span>
          <span className="tab-label">Contexts</span>
        </button>
      </div>

      {/* Tab Content */}
      <div className="sidebar-content">
        {activeTab === 'pages' && (
          <PageManager
            siteId={siteId ?? null}
            currentPageId={currentPageId ?? null}
            onPageSelect={onPageSelect || (() => {})}
            onPageCreate={onPageCreate}
            onPageDelete={onPageDelete}
          />
        )}
        {activeTab === 'components' && (
          <ComponentPalette onComponentDragStart={onComponentDragStart} />
        )}
        {activeTab === 'templates' && (
          <TemplatePalette onTemplateDrop={onTemplateDrop} />
        )}
        {activeTab === 'contexts' && <ContextPanel />}
      </div>
    </div>
  );
};

export default LeftSidebar;
