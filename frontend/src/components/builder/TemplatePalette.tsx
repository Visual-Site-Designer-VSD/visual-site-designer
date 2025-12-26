import React, { useState, useMemo } from 'react';
import { UITemplate, TemplateComponentDef } from '../../types/templates';
import { ComponentInstance } from '../../types/builder';
import { templateGroups, searchTemplates } from '../../data/uiTemplates';
import './TemplatePalette.css';

interface TemplatePaletteProps {
  onTemplateDrop?: (components: ComponentInstance[]) => void;
}

/**
 * Generate a unique instance ID
 */
const generateInstanceId = (componentId: string): string => {
  return `${componentId}-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
};

/**
 * Determine component category based on componentId and pluginId
 */
const determineComponentCategory = (componentId: string, pluginId: string): 'layout' | 'ui' | 'navigation' => {
  // Layout components
  if (componentId === 'Container' || componentId === 'scrollable-container') {
    return 'layout';
  }
  // Navbar components (from core-navbar plugin)
  if (pluginId === 'core-navbar' || componentId.toLowerCase().includes('navbar')) {
    return 'navigation';
  }
  // Default to UI
  return 'ui';
};

/**
 * Convert TemplateComponentDef to ComponentInstance recursively
 */
const templateDefToInstance = (
  def: TemplateComponentDef,
  basePosition: { row: number; column: number } = { row: 1, column: 1 }
): ComponentInstance => {
  const position = def.relativePosition || { row: 1, column: 1, rowSpan: 1, columnSpan: 12 };
  const componentCategory = determineComponentCategory(def.componentId, def.pluginId);

  const instance: ComponentInstance = {
    instanceId: generateInstanceId(def.componentId),
    pluginId: def.pluginId,
    componentId: def.componentId,
    componentCategory: componentCategory,
    position: {
      row: basePosition.row + position.row - 1,
      column: basePosition.column + position.column - 1,
      rowSpan: position.rowSpan,
      columnSpan: position.columnSpan,
    },
    size: {
      width: '100%',
      height: 'auto',
    },
    props: { ...def.props },
    styles: { ...def.styles },
    isVisible: true,
  };

  if (def.children && def.children.length > 0) {
    instance.children = def.children.map(child => templateDefToInstance(child));
  }

  return instance;
};

/**
 * Template Palette - Displays pre-built UI templates
 */
export const TemplatePalette: React.FC<TemplatePaletteProps> = ({ onTemplateDrop }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(
    new Set(templateGroups.map(g => g.category))
  );
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  // Filter templates based on search and category
  const filteredGroups = useMemo(() => {
    if (searchQuery.trim()) {
      const results = searchTemplates(searchQuery);
      // Group search results by category
      const groupedResults = new Map<string, UITemplate[]>();
      results.forEach(template => {
        if (!groupedResults.has(template.category)) {
          groupedResults.set(template.category, []);
        }
        groupedResults.get(template.category)!.push(template);
      });

      return templateGroups
        .filter(g => groupedResults.has(g.category))
        .map(g => ({
          ...g,
          templates: groupedResults.get(g.category) || [],
        }));
    }

    if (selectedCategory) {
      return templateGroups.filter(g => g.category === selectedCategory);
    }

    return templateGroups;
  }, [searchQuery, selectedCategory]);

  const toggleCategoryExpansion = (category: string) => {
    const newExpanded = new Set(expandedCategories);
    if (newExpanded.has(category)) {
      newExpanded.delete(category);
    } else {
      newExpanded.add(category);
    }
    setExpandedCategories(newExpanded);
  };

  const handleCategoryClick = (category: string) => {
    if (selectedCategory === category) {
      setSelectedCategory(null);
    } else {
      setSelectedCategory(category);
    }
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
  };

  const clearFilters = () => {
    setSelectedCategory(null);
    setSearchQuery('');
  };

  const handleDragStart = (e: React.DragEvent, template: UITemplate) => {
    // Convert template components to ComponentInstances
    const instances = template.components.map(comp => templateDefToInstance(comp));

    // Set drag data
    e.dataTransfer.effectAllowed = 'copy';
    e.dataTransfer.setData('application/template', JSON.stringify({
      templateId: template.id,
      templateName: template.name,
      components: instances,
      suggestedSize: template.suggestedSize,
    }));
  };

  const hasActiveFilters = selectedCategory !== null || searchQuery.trim() !== '';
  const totalTemplates = templateGroups.reduce((sum, g) => sum + g.templates.length, 0);
  const categories = templateGroups.map(g => g.category);

  return (
    <div className="template-palette">
      <div className="palette-header">
        <h3>Templates</h3>
      </div>

      {/* Search Bar */}
      <div className="palette-search">
        <input
          type="text"
          placeholder="Search templates..."
          value={searchQuery}
          onChange={handleSearchChange}
          className="search-input"
        />
        {hasActiveFilters && (
          <button onClick={clearFilters} className="clear-filters" title="Clear filters">
            ✕
          </button>
        )}
      </div>

      {/* Category Filter Chips */}
      <div className="category-chips">
        <button
          className={`category-chip ${selectedCategory === null ? 'active' : ''}`}
          onClick={() => setSelectedCategory(null)}
        >
          All
        </button>
        {categories.map(category => {
          const group = templateGroups.find(g => g.category === category);
          return (
            <button
              key={category}
              className={`category-chip ${selectedCategory === category ? 'active' : ''}`}
              onClick={() => handleCategoryClick(category)}
              title={group?.displayName}
            >
              {group?.icon} {group?.displayName}
            </button>
          );
        })}
      </div>

      {/* Template List */}
      <div className="template-list">
        {filteredGroups.length === 0 ? (
          <div className="empty-state">
            <p>No templates found</p>
            {hasActiveFilters && (
              <button onClick={clearFilters} className="clear-filters-button">
                Clear filters
              </button>
            )}
          </div>
        ) : (
          filteredGroups.map(group => (
            <div key={group.category} className="template-category">
              <div
                className="category-header"
                onClick={() => toggleCategoryExpansion(group.category)}
              >
                <span className="category-toggle">
                  {expandedCategories.has(group.category) ? '▼' : '▶'}
                </span>
                <span className="category-icon">{group.icon}</span>
                <h4>{group.displayName}</h4>
                <span className="template-count">{group.templates.length}</span>
              </div>

              {expandedCategories.has(group.category) && (
                <div className="category-templates">
                  {group.templates.map(template => (
                    <div
                      key={template.id}
                      className="template-item"
                      draggable
                      onDragStart={(e) => handleDragStart(e, template)}
                      title={template.description}
                    >
                      <div className="template-icon">{template.icon}</div>
                      <div className="template-info">
                        <div className="template-name">{template.name}</div>
                        <div className="template-description">{template.description}</div>
                      </div>
                      <div className="drag-handle">⋮⋮</div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* Footer Info */}
      <div className="palette-footer">
        <small>
          {totalTemplates} template{totalTemplates !== 1 ? 's' : ''} available
        </small>
      </div>
    </div>
  );
};

export default TemplatePalette;
