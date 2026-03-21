import React, { useEffect, useState } from 'react';
import { useContextStore } from '../../stores/contextStore';
import { contextService } from '../../services/contextService';
import { ContextDescriptor } from '../../types/builder';
import './ContextPanel.css';

/**
 * ContextPanel - Sidebar panel for discovering active context providers
 * in the current site. Shows context IDs, dependencies, API endpoints,
 * and availability status.
 */
export const ContextPanel: React.FC = () => {
  const { contexts, isLoading, error, initialized, setContexts, setLoading, setError } =
    useContextStore();
  const [expandedContexts, setExpandedContexts] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (initialized) return;

    const loadContexts = async () => {
      setLoading(true);
      try {
        const descriptors = await contextService.getActiveContexts();
        setContexts(descriptors);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load contexts');
      } finally {
        setLoading(false);
      }
    };

    loadContexts();
  }, [initialized, setContexts, setLoading, setError]);

  const toggleExpand = (contextId: string) => {
    setExpandedContexts((prev) => {
      const next = new Set(prev);
      if (next.has(contextId)) {
        next.delete(contextId);
      } else {
        next.add(contextId);
      }
      return next;
    });
  };

  const isContextAvailable = (contextId: string): boolean => {
    return contexts.some((c) => c.contextId === contextId);
  };

  if (isLoading) {
    return (
      <div className="context-panel">
        <div className="context-panel-loading">Loading contexts...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="context-panel">
        <div className="context-panel-error">{error}</div>
      </div>
    );
  }

  return (
    <div className="context-panel">
      <div className="context-panel-header">
        <h3>Context Providers</h3>
        <p>
          {contexts.length} active context{contexts.length !== 1 ? 's' : ''} available
        </p>
      </div>

      {contexts.length === 0 ? (
        <div className="context-panel-empty">
          <span className="empty-icon">🔗</span>
          <p>
            No context providers registered.
            <br />
            Install a context provider plugin to share state between components.
          </p>
        </div>
      ) : (
        <ul className="context-list">
          {contexts.map((ctx) => (
            <ContextItem
              key={ctx.contextId}
              context={ctx}
              isExpanded={expandedContexts.has(ctx.contextId)}
              onToggle={() => toggleExpand(ctx.contextId)}
              isContextAvailable={isContextAvailable}
            />
          ))}
        </ul>
      )}
    </div>
  );
};

interface ContextItemProps {
  context: ContextDescriptor;
  isExpanded: boolean;
  onToggle: () => void;
  isContextAvailable: (contextId: string) => boolean;
}

const ContextItem: React.FC<ContextItemProps> = ({
  context,
  isExpanded,
  onToggle,
  isContextAvailable,
}) => {
  return (
    <li className="context-item">
      <button className="context-item-header" onClick={onToggle}>
        <span className={`context-item-expand ${isExpanded ? 'expanded' : ''}`}>▶</span>
        <span className="context-item-icon">🔗</span>
        <div className="context-item-info">
          <div className="context-item-name">{context.contextId}</div>
          <div className="context-item-plugin">{context.pluginId}</div>
        </div>
        <span className="context-item-badge">Active</span>
      </button>

      {isExpanded && (
        <div className="context-item-details">
          {/* Dependencies */}
          {context.requiredContexts && context.requiredContexts.length > 0 && (
            <div className="context-detail-section">
              <h4>Dependencies</h4>
              <ul className="context-deps-list">
                {context.requiredContexts.map((depId) => (
                  <li key={depId} className={isContextAvailable(depId) ? '' : 'dep-missing'}>
                    {depId}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* API Endpoints */}
          {context.apiEndpoints && context.apiEndpoints.length > 0 && (
            <div className="context-detail-section">
              <h4>API Endpoints</h4>
              <ul className="context-detail-list">
                {context.apiEndpoints.map((ep, idx) => (
                  <li key={idx}>
                    <span
                      className={`endpoint-method method-${ep.method.toLowerCase()}`}
                    >
                      {ep.method}
                    </span>
                    <span className="endpoint-path">{ep.path}</span>
                    {ep.description && (
                      <span className="endpoint-desc">— {ep.description}</span>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Provider component path */}
          <div className="context-detail-section">
            <h4>Provider Bundle</h4>
            <ul className="context-detail-list">
              <li>
                <span className="endpoint-path">{context.providerComponentPath}</span>
              </li>
            </ul>
          </div>
        </div>
      )}
    </li>
  );
};

export default ContextPanel;
