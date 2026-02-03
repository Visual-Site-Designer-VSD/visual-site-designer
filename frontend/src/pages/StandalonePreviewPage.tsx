import React, { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useMultiPagePreviewStore } from '../stores/multiPagePreviewStore';
import { PreviewNavigationInterceptor } from '../components/builder/PreviewNavigationInterceptor';
import { BuilderCanvas } from '../components/builder/BuilderCanvas';
import { Page } from '../types/site';
import { PageDefinition, ComponentInstance } from '../types/builder';
import { pageService } from '../services/pageService';
import { loadPlugin } from '../services/pluginLoaderService';
import {
  previewBroadcast,
  PageUpdatePayload,
  NavigatePayload,
  PagesListPayload,
} from '../services/previewBroadcastService';
import '../components/builder/MultiPagePreview.css';

/**
 * Recursively collect all unique plugin IDs from a component tree
 */
function collectPluginIds(components: ComponentInstance[]): Set<string> {
  const pluginIds = new Set<string>();

  function traverse(component: ComponentInstance) {
    if (component.pluginId) {
      pluginIds.add(component.pluginId);
    }
    if (component.children && component.children.length > 0) {
      component.children.forEach(traverse);
    }
  }

  components.forEach(traverse);
  return pluginIds;
}

/**
 * Preload all plugins used by a page definition
 */
async function preloadPluginsForPage(page: PageDefinition): Promise<void> {
  const pluginIds = collectPluginIds(page.components || []);
  console.log('[StandalonePreview] Preloading plugins:', Array.from(pluginIds));

  await Promise.all(
    Array.from(pluginIds).map(pluginId =>
      loadPlugin(pluginId).catch(err => {
        console.warn(`[StandalonePreview] Failed to preload plugin ${pluginId}:`, err);
      })
    )
  );

  console.log('[StandalonePreview] All plugins preloaded');
}

/**
 * StandalonePreviewPage - Opens in a separate window and receives live updates from the builder
 */
export const StandalonePreviewPage: React.FC = () => {
  const { siteId: urlSiteId, pageId: urlPageId } = useParams<{ siteId?: string; pageId?: string }>();
  const [isLoadingPage, setIsLoadingPage] = useState(true);
  const [isLoadingPlugins, setIsLoadingPlugins] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);

  const siteId = urlSiteId ? parseInt(urlSiteId, 10) : null;

  const {
    isActive,
    pages,
    currentPreviewPath,
    previewPage,
    setActive,
    setPages,
    navigateToPage,
    loadPageDefinition,
    getPageDefinition,
    setPreviewPage,
    goBack,
    goForward,
    canGoBack,
    canGoForward,
  } = useMultiPagePreviewStore();

  // Handle incoming page updates from builder
  const handlePageUpdate = useCallback(async (payload: unknown) => {
    const { page, pageMeta } = payload as PageUpdatePayload;
    console.log('[StandalonePreview] Received page update:', pageMeta.pageName);

    setLastUpdate(new Date());

    // Preload plugins
    setIsLoadingPlugins(true);
    try {
      await preloadPluginsForPage(page);
    } catch (err) {
      console.error('[StandalonePreview] Plugin preload error:', err);
    } finally {
      setIsLoadingPlugins(false);
    }

    // Get the page path
    const slug = pageMeta.pageSlug?.toLowerCase() || 'home';
    const hasValidRoutePath = pageMeta.routePath && pageMeta.routePath.trim() !== '';
    const pagePath = hasValidRoutePath ? pageMeta.routePath : (slug === 'home' ? '/' : `/${slug}`);

    // Update the cache and preview
    loadPageDefinition(pagePath, page);

    // If this is the current page, update the preview
    if (pagePath === currentPreviewPath) {
      setPreviewPage(page);
    }
  }, [currentPreviewPath, loadPageDefinition, setPreviewPage]);

  // Handle navigation requests from builder
  const handleNavigate = useCallback((payload: unknown) => {
    const { path } = payload as NavigatePayload;
    console.log('[StandalonePreview] Received navigate request:', path);
    navigateToPage(path);
  }, [navigateToPage]);

  // Handle pages list updates from builder
  const handlePagesList = useCallback((payload: unknown) => {
    const { pages: newPages } = payload as PagesListPayload;
    console.log('[StandalonePreview] Received pages list:', newPages.length, 'pages');
    setPages(newPages);
  }, [setPages]);

  // Set up broadcast listeners
  useEffect(() => {
    // Mark this as the preview window
    previewBroadcast.setAsPreviewWindow();
    setIsConnected(true);

    // Listen for messages from builder
    previewBroadcast.on('PAGE_UPDATE', handlePageUpdate);
    previewBroadcast.on('PAGE_NAVIGATE', handleNavigate);
    previewBroadcast.on('PAGES_LIST', handlePagesList);

    // Update window title
    document.title = 'Preview - Visual Site Designer';

    return () => {
      previewBroadcast.off('PAGE_UPDATE', handlePageUpdate);
      previewBroadcast.off('PAGE_NAVIGATE', handleNavigate);
      previewBroadcast.off('PAGES_LIST', handlePagesList);
    };
  }, [handlePageUpdate, handleNavigate, handlePagesList]);

  // Initialize preview mode
  useEffect(() => {
    setActive(true);

    // Try to load initial data from localStorage or API
    const loadInitialData = async () => {
      setIsLoadingPage(true);
      setError(null);

      try {
        // Try to get pages from localStorage first (for demo mode)
        // Note: builder_saved_pages is stored as an OBJECT keyed by page slug, not an array
        const savedPagesJson = localStorage.getItem('builder_saved_pages');
        if (savedPagesJson) {
          const parsed = JSON.parse(savedPagesJson);

          // Handle both object format (demo mode) and array format (if ever used)
          let savedPages: Page[];
          if (Array.isArray(parsed)) {
            savedPages = parsed;
          } else if (typeof parsed === 'object' && parsed !== null) {
            // Convert object format to Page[] array (matching BuilderPage.loadPagesForPreview)
            savedPages = Object.entries(parsed).map(([key, val]: [string, any], index) => ({
              id: index + 1,
              siteId: 0,
              pageName: val.pageName || key,
              pageSlug: key,
              pageType: val.pageType || 'standard',
              routePath: key === 'home' ? '/' : `/${key}`,
              displayOrder: index,
              isPublished: false,
              createdAt: val.savedAt || new Date().toISOString(),
              updatedAt: val.savedAt || new Date().toISOString(),
            }));

            // Also pre-cache the page definitions from localStorage
            Object.entries(parsed).forEach(([key, val]: [string, any]) => {
              const pagePath = key === 'home' ? '/' : `/${key}`;
              // The val contains the PageDefinition data
              loadPageDefinition(pagePath, val as PageDefinition);
            });
          } else {
            savedPages = [];
          }

          if (savedPages.length > 0) {
            setPages(savedPages);

            // Navigate to home page by default
            const homePage = savedPages.find(p =>
              p.pageSlug?.toLowerCase() === 'home' ||
              p.routePath === '/'
            );

            if (homePage) {
              const path = homePage.routePath || '/';
              navigateToPage(path);
            }
          }
        }

        // If we have a siteId, try to load from API
        if (siteId) {
          const apiPages = await pageService.getAllPages(siteId);
          if (apiPages.length > 0) {
            setPages(apiPages);

            // Navigate to the specified page or home
            if (urlPageId) {
              const targetPage = apiPages.find(p => p.id === parseInt(urlPageId, 10));
              if (targetPage) {
                const slug = targetPage.pageSlug?.toLowerCase() || 'home';
                const hasValidRoutePath = targetPage.routePath && targetPage.routePath.trim() !== '';
                const path = hasValidRoutePath ? targetPage.routePath : (slug === 'home' ? '/' : `/${slug}`);
                navigateToPage(path);
              }
            } else {
              navigateToPage('/');
            }
          }
        }
      } catch (err) {
        console.error('[StandalonePreview] Failed to load initial data:', err);
        // Don't show error - wait for builder to send data
      } finally {
        setIsLoadingPage(false);
      }
    };

    loadInitialData();

    return () => {
      setActive(false);
    };
  }, [siteId, urlPageId, setActive, setPages, navigateToPage]);

  // Load page when path changes
  useEffect(() => {
    if (!isActive || !currentPreviewPath) return;

    const loadPage = async () => {
      setIsLoadingPage(true);
      setError(null);

      try {
        // Check cache first
        const cachedPage = getPageDefinition(currentPreviewPath);
        if (cachedPage) {
          setIsLoadingPlugins(true);
          await preloadPluginsForPage(cachedPage);
          setIsLoadingPlugins(false);
          setPreviewPage(cachedPage);
          setIsLoadingPage(false);
          return;
        }

        // Find page metadata
        const pagesArray = Array.isArray(pages) ? pages : [];
        const page = pagesArray.find(p => {
          const slug = p.pageSlug?.toLowerCase() || 'home';
          const hasValidRoutePath = p.routePath && p.routePath.trim() !== '';
          const pagePath = hasValidRoutePath ? p.routePath : (slug === 'home' ? '/' : `/${slug}`);
          return pagePath === currentPreviewPath;
        });

        if (!page) {
          setError(`Page not found: ${currentPreviewPath}`);
          setIsLoadingPage(false);
          return;
        }

        // Try to load from localStorage
        // Note: Page definitions are stored in builder_saved_pages object keyed by page slug
        const savedPagesJson = localStorage.getItem('builder_saved_pages');
        let pageData: PageDefinition | null = null;

        if (savedPagesJson) {
          try {
            const savedPagesObj = JSON.parse(savedPagesJson);
            // Look up by page slug
            const pageSlug = page.pageSlug || 'home';
            if (savedPagesObj[pageSlug]) {
              pageData = savedPagesObj[pageSlug] as PageDefinition;
            }
          } catch (e) {
            console.warn('[StandalonePreview] Failed to parse localStorage:', e);
          }
        }

        if (pageData) {
          setIsLoadingPlugins(true);
          await preloadPluginsForPage(pageData);
          setIsLoadingPlugins(false);
          loadPageDefinition(currentPreviewPath, pageData);
          setPreviewPage(pageData);
        } else if (siteId) {
          // Try API
          const pageData = await pageService.getPageDefinition(siteId, page.id);
          if (pageData) {
            setIsLoadingPlugins(true);
            await preloadPluginsForPage(pageData);
            setIsLoadingPlugins(false);
            loadPageDefinition(currentPreviewPath, pageData);
            setPreviewPage(pageData);
          } else {
            setError(`No content found for: ${page.pageName}`);
          }
        } else {
          setError(`Page data not available: ${page.pageName}`);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load page');
      } finally {
        setIsLoadingPage(false);
      }
    };

    loadPage();
  }, [currentPreviewPath, isActive, pages, siteId, getPageDefinition, loadPageDefinition, setPreviewPage]);

  // Get current page name
  const getCurrentPageName = (): string => {
    const pagesArray = Array.isArray(pages) ? pages : [];
    const page = pagesArray.find(p => {
      const slug = p.pageSlug?.toLowerCase() || 'home';
      const hasValidRoutePath = p.routePath && p.routePath.trim() !== '';
      const pagePath = hasValidRoutePath ? p.routePath : (slug === 'home' ? '/' : `/${slug}`);
      return pagePath === currentPreviewPath;
    });
    return page?.pageName || previewPage?.pageName || 'Preview';
  };

  // Handle page selection
  const handlePageSelect = (path: string) => {
    navigateToPage(path);
  };

  // Close preview window
  const handleClose = () => {
    window.close();
  };

  return (
    <div className="multi-page-preview standalone-preview">
      {/* Preview Toolbar */}
      <div className="preview-toolbar">
        <div className="preview-toolbar-left">
          {/* Connection indicator */}
          <div
            className="connection-indicator"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 10px',
              borderRadius: '4px',
              backgroundColor: isConnected ? 'rgba(39, 174, 96, 0.2)' : 'rgba(231, 76, 60, 0.2)',
              fontSize: '12px',
            }}
          >
            <span
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                backgroundColor: isConnected ? '#27ae60' : '#e74c3c',
              }}
            />
            <span style={{ color: isConnected ? '#27ae60' : '#e74c3c' }}>
              {isConnected ? 'Live' : 'Disconnected'}
            </span>
            {lastUpdate && (
              <span style={{ color: '#7f8c8d', marginLeft: '4px' }}>
                {lastUpdate.toLocaleTimeString()}
              </span>
            )}
          </div>

          {/* Navigation Controls */}
          <div className="nav-controls">
            <button
              className="nav-btn"
              onClick={goBack}
              disabled={!canGoBack()}
              title="Go Back"
            >
              ←
            </button>
            <button
              className="nav-btn"
              onClick={goForward}
              disabled={!canGoForward()}
              title="Go Forward"
            >
              →
            </button>
          </div>

          {/* Address Bar */}
          <div className="address-bar">
            <span className="protocol">preview://</span>
            <span className="path">{currentPreviewPath}</span>
          </div>
        </div>

        <div className="preview-toolbar-center">
          {/* Page Selector */}
          <div className="page-selector-dropdown">
            <select
              value={currentPreviewPath}
              onChange={(e) => handlePageSelect(e.target.value)}
            >
              {Array.isArray(pages) && pages.map(page => {
                const slug = page.pageSlug?.toLowerCase() || 'home';
                const hasValidRoutePath = page.routePath && page.routePath.trim() !== '';
                const path = hasValidRoutePath ? page.routePath : (slug === 'home' ? '/' : `/${slug}`);
                return (
                  <option key={page.id} value={path}>
                    {page.pageName} ({path})
                  </option>
                );
              })}
            </select>
          </div>
        </div>

        <div className="preview-toolbar-right">
          <button
            className="exit-preview-btn"
            onClick={handleClose}
            title="Close Preview Window"
          >
            ✕ Close
          </button>
        </div>
      </div>

      {/* Preview Content */}
      <div className="preview-content">
        {isLoadingPage || isLoadingPlugins ? (
          <div className="preview-loading">
            <div className="loading-spinner" />
            <p>{isLoadingPlugins ? 'Loading plugins...' : 'Loading page...'}</p>
            {(!Array.isArray(pages) || !pages.length) && (
              <p style={{ fontSize: '12px', color: '#7f8c8d', marginTop: '8px' }}>
                Waiting for builder connection...
              </p>
            )}
          </div>
        ) : error ? (
          <div className="preview-error">
            <div className="error-icon">⚠️</div>
            <h3>Page Not Found</h3>
            <p>{error}</p>
            <button onClick={() => navigateToPage('/')}>Go to Home</button>
          </div>
        ) : (
          <PreviewNavigationInterceptor enabled={isActive}>
            <BuilderCanvas
              key={`standalone-preview-${currentPreviewPath}-${previewPage?.pageName || 'empty'}-${lastUpdate?.getTime() || 0}`}
              pageOverride={previewPage}
              forcePreviewMode={true}
            />
          </PreviewNavigationInterceptor>
        )}
      </div>

      {/* Page Indicator */}
      <div className="preview-page-indicator">
        <span className="current-page-name">{getCurrentPageName()}</span>
        <span className="page-count">
          {Array.isArray(pages) && pages.length > 0 && (() => {
            const idx = pages.findIndex(p => {
              const slug = p.pageSlug?.toLowerCase() || 'home';
              const hasValidRoutePath = p.routePath && p.routePath.trim() !== '';
              const pagePath = hasValidRoutePath ? p.routePath : (slug === 'home' ? '/' : `/${slug}`);
              return pagePath === currentPreviewPath;
            });
            return `(${idx + 1} of ${pages.length} pages)`;
          })()}
        </span>
      </div>
    </div>
  );
};

export default StandalonePreviewPage;
