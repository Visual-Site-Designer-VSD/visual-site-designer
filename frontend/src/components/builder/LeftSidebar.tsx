import React, { useState } from 'react';
import { ComponentPalette } from './ComponentPalette';
import { TemplatePalette } from './TemplatePalette';
import { ComponentRegistryEntry, ComponentInstance } from '../../types/builder';
import './LeftSidebar.css';

type SidebarTab = 'components' | 'templates';

interface LeftSidebarProps {
  onComponentDragStart?: (component: ComponentRegistryEntry) => void;
  onTemplateDrop?: (components: ComponentInstance[]) => void;
}

/**
 * Left Sidebar - Tabbed container for Components and Templates palettes
 */
export const LeftSidebar: React.FC<LeftSidebarProps> = ({
  onComponentDragStart,
  onTemplateDrop,
}) => {
  const [activeTab, setActiveTab] = useState<SidebarTab>('components');

  return (
    <div className="left-sidebar">
      {/* Tab Navigation */}
      <div className="sidebar-tabs">
        <button
          className={`sidebar-tab ${activeTab === 'components' ? 'active' : ''}`}
          onClick={() => setActiveTab('components')}
        >
          <span className="tab-icon">🧩</span>
          <span className="tab-label">Components</span>
        </button>
        <button
          className={`sidebar-tab ${activeTab === 'templates' ? 'active' : ''}`}
          onClick={() => setActiveTab('templates')}
        >
          <span className="tab-icon">📐</span>
          <span className="tab-label">Templates</span>
        </button>
      </div>

      {/* Tab Content */}
      <div className="sidebar-content">
        {activeTab === 'components' && (
          <ComponentPalette onComponentDragStart={onComponentDragStart} />
        )}
        {activeTab === 'templates' && (
          <TemplatePalette onTemplateDrop={onTemplateDrop} />
        )}
      </div>
    </div>
  );
};

export default LeftSidebar;
