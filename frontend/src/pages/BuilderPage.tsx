import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useBuilderStore } from '../stores/builderStore';
import { useUIPreferencesStore } from '../stores/uiPreferencesStore';
import { useSiteManagerStore } from '../stores/siteManagerStore';
import { LeftSidebar } from '../components/builder/LeftSidebar';
import { BuilderCanvas } from '../components/builder/BuilderCanvas';
import { PropertiesPanel } from '../components/builder/PropertiesPanel';
import { BuilderMenubar } from '../components/builder/BuilderMenubar';
import { CSSEditor } from '../components/editor/CSSEditor';
import { CanvasRuler } from '../components/builder/CanvasRuler';
import { ImageRepositoryModal } from '../components/builder/ImageRepositoryModal';
import { MultiPagePreview } from '../components/builder/MultiPagePreview';
import { ExportModal } from '../components/builder/ExportModal';
import { ClipboardProvider } from '../components/clipboard';
import { pageService } from '../services/pageService';
import { PageDefinition } from '../types/builder';
import { Page } from '../types/site';
import './BuilderPage.css';

interface BuilderPageParams {
  siteId: string;
  pageId: string;
}

/**
 * BuilderPage - Main visual site builder page
 * Integrates all builder components into a cohesive interface
 */
export const BuilderPage: React.FC = () => {
  const { siteId, pageId } = useParams<Record<string, string>>();
  const navigate = useNavigate();

  const [showCSSEditor, setShowCSSEditor] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'saved' | 'unsaved' | 'saving' | 'error'>('saved');
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [showImageRepository, setShowImageRepository] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);
  const [currentPageMeta, setCurrentPageMeta] = useState<Page | null>(null);
  const [sitePages, setSitePages] = useState<Page[]>([]);
  const [isMultiPagePreview, setIsMultiPagePreview] = useState(false);

  // UI Preferences from store
  const uiPreferences = useUIPreferencesStore();
  const { leftPanelCollapsed, rightPanelCollapsed, showGrid, showRulers } = uiPreferences;

  // Site manager store
  const siteManager = useSiteManagerStore();

  const {
    currentPage,
    selectedComponentId,
    viewMode,
    isLoading,
    error,
    setCurrentPage,
    setViewMode,
    setLoading,
    setError,
    undo,
    redo,
    canUndo,
    canRedo,
    saveSnapshot
  } = useBuilderStore();

  // Load page data on mount
  useEffect(() => {
    if (siteId && pageId) {
      loadPage(parseInt(siteId), parseInt(pageId));
    } else {
      // Create new page
      initializeNewPage();
    }
  }, [siteId, pageId]);

  // Auto-save on changes
  useEffect(() => {
    if (currentPage && saveStatus === 'saved') {
      setSaveStatus('unsaved');
    }
  }, [currentPage]);

  // handleSave must be defined before keyboard shortcuts useEffect
  const handleSave = useCallback(async () => {
    if (!currentPage) {
      alert('Cannot save: No page data available');
      return;
    }

    setIsSaving(true);
    setSaveStatus('saving');

    try {
      // If we have siteId and pageId, save to backend
      if (siteId && pageId) {
        console.log(`[Save] Saving page to backend: siteId=${siteId}, pageId=${pageId}, pageName="${currentPage.pageName}", componentsCount=${currentPage.components?.length}`);
        const savedVersion = await pageService.savePageVersion(
          parseInt(siteId),
          parseInt(pageId),
          currentPage,
          'Manual save'
        );
        console.log(`[Save] Successfully saved version:`, savedVersion);
      } else {
        // Save to localStorage for demo/new pages
        const savedPages = JSON.parse(localStorage.getItem('builder_saved_pages') || '{}');

        // IMPORTANT: Always use currentPageMeta.pageSlug as the key to ensure
        // each page is saved under its correct slug, not derived from pageName
        // which can change when templates are dropped or page name is edited
        let pageKey: string;
        if (currentPageMeta?.pageSlug) {
          pageKey = currentPageMeta.pageSlug;
        } else {
          // Fallback: derive from page name (only for brand new pages)
          pageKey = currentPage.pageName.replace(/\s+/g, '-').toLowerCase() || 'untitled';
          console.warn('No currentPageMeta.pageSlug, using derived key:', pageKey);
        }

        // Save the page with its correct pageName from metadata
        savedPages[pageKey] = {
          ...currentPage,
          // Use metadata pageName if available (keeps original name regardless of template)
          pageName: currentPageMeta?.pageName || currentPage.pageName,
          savedAt: new Date().toISOString()
        };
        localStorage.setItem('builder_saved_pages', JSON.stringify(savedPages));
        console.log('Page saved to localStorage:', pageKey, 'pageName:', savedPages[pageKey].pageName);

        // Update currentPageMeta if not already set
        if (!currentPageMeta) {
          const pageKeys = Object.keys(savedPages);
          const pageIndex = pageKeys.indexOf(pageKey);
          setCurrentPageMeta({
            id: Date.now(),
            siteId: 0,
            pageName: currentPage.pageName,
            pageSlug: pageKey,
            pageType: 'standard',
            routePath: `/${pageKey}`,
            displayOrder: pageIndex,
            isPublished: false,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          });
        }
      }

      setSaveStatus('saved');
      setLastSaved(new Date());
      saveSnapshot('Manual save');
    } catch (err) {
      setSaveStatus('error');
      setError(err instanceof Error ? err.message : 'Failed to save page');
      alert('Failed to save page. Please try again.');
    } finally {
      setIsSaving(false);
    }
  }, [currentPage, siteId, pageId, currentPageMeta, saveSnapshot]);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ctrl+S / Cmd+S - Save
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        handleSave();
      }

      // Ctrl+Z / Cmd+Z - Undo
      if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
        e.preventDefault();
        if (canUndo()) {
          undo();
        }
      }

      // Ctrl+Shift+Z / Cmd+Shift+Z - Redo
      if ((e.ctrlKey || e.metaKey) && e.key === 'z' && e.shiftKey) {
        e.preventDefault();
        if (canRedo()) {
          redo();
        }
      }

      // Ctrl+E / Cmd+E - Toggle CSS Editor
      if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
        e.preventDefault();
        setShowCSSEditor(!showCSSEditor);
      }

      // Escape - Close panels
      if (e.key === 'Escape') {
        setShowCSSEditor(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [canUndo, canRedo, showCSSEditor, handleSave, undo, redo]);


  const loadPage = async (siteId: number, pageId: number) => {
    setLoading(true);
    setError(null);

    try {
      // Load page metadata first (this should always succeed if page exists)
      const pageMeta = await pageService.getPageById(siteId, pageId);
      setCurrentPageMeta(pageMeta);

      // Try to load page definition (may not exist yet for new pages)
      try {
        const pageDefinition = await pageService.getPageDefinition(siteId, pageId);
        setCurrentPage(pageDefinition);
        setSaveStatus('saved');
        setLastSaved(new Date());
      } catch (defErr: any) {
        // If 404, page exists but has no saved content - create empty definition
        if (defErr?.response?.status === 404) {
          const emptyPage: PageDefinition = {
            version: '1.0',
            pageId: pageMeta.id,
            pageName: pageMeta.pageName,
            grid: {
              columns: 12,
              rows: 'auto',
              gap: '20px',
              minRowHeight: '50px'
            },
            components: [],
            globalStyles: {
              cssVariables: {},
              customCSS: ''
            }
          };
          setCurrentPage(emptyPage);
          setSaveStatus('unsaved');
        } else {
          throw defErr;
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load page');
    } finally {
      setLoading(false);
    }
  };

  const initializeNewPage = () => {
    // Try to load from localStorage first
    const savedPages = JSON.parse(localStorage.getItem('builder_saved_pages') || '{}');
    const lastSavedKey = Object.keys(savedPages).pop();

    if (lastSavedKey && savedPages[lastSavedKey]) {
      const savedPage = savedPages[lastSavedKey];
      console.log('Loading saved page from localStorage:', lastSavedKey);
      setCurrentPage(savedPage);
      setSaveStatus('saved');
      setLastSaved(savedPage.savedAt ? new Date(savedPage.savedAt) : null);

      // Set page metadata for multi-page preview
      const pageKeys = Object.keys(savedPages);
      const pageIndex = pageKeys.indexOf(lastSavedKey);
      setCurrentPageMeta({
        id: pageIndex + 1,
        siteId: 0,
        pageName: savedPage.pageName || lastSavedKey,
        pageSlug: lastSavedKey,
        pageType: savedPage.pageType || 'standard',
        routePath: `/${lastSavedKey}`,
        displayOrder: pageIndex,
        isPublished: false,
        createdAt: savedPage.savedAt || new Date().toISOString(),
        updatedAt: savedPage.savedAt || new Date().toISOString(),
      });
      return;
    }

    // Create new page if nothing saved
    const newPage: PageDefinition = {
      version: '1.0',
      pageName: 'New Page',
      grid: {
        columns: 12,
        rows: 'auto',
        gap: '20px',
        minRowHeight: '50px'
      },
      components: [],
      globalStyles: {
        cssVariables: {},
        customCSS: ''
      }
    };

    setCurrentPage(newPage);
    setSaveStatus('unsaved');
    setCurrentPageMeta(null);
  };

  const handlePublish = async () => {
    if (!siteId || !pageId) return;

    if (!confirm('Are you sure you want to publish this page? It will be visible to users.')) {
      return;
    }

    try {
      // Save first
      await handleSave();

      // Then publish
      await pageService.publishPage(parseInt(siteId), parseInt(pageId));
      alert('Page published successfully!');
    } catch (err) {
      alert('Failed to publish page: ' + (err instanceof Error ? err.message : 'Unknown error'));
    }
  };

  const handlePreview = () => {
    if (viewMode === 'edit') {
      // Enter multi-page preview mode
      setViewMode('preview');
      setIsMultiPagePreview(true);
      // Load pages for preview navigation
      loadPagesForPreview();
    } else {
      // Exit preview mode
      setViewMode('edit');
      setIsMultiPagePreview(false);
    }
  };

  const loadPagesForPreview = async () => {
    // Use URL siteId or fall back to store's currentSiteId
    const effectiveSiteId = siteId ? Number.parseInt(siteId) : siteManager.currentSiteId;

    if (effectiveSiteId) {
      try {
        const pages = await pageService.getAllPages(effectiveSiteId);
        setSitePages(pages);
      } catch (err) {
        console.error('Failed to load pages for preview:', err);
      }
    } else {
      // Demo mode - load from localStorage
      const savedPages = JSON.parse(localStorage.getItem('builder_saved_pages') || '{}');
      const demoPages: Page[] = Object.entries(savedPages).map(([key, val]: [string, any], index) => ({
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
      setSitePages(demoPages);
    }
  };

  const handleExitMultiPagePreview = () => {
    // Import the preview store to get the original editing page
    import('../stores/multiPagePreviewStore').then(({ useMultiPagePreviewStore }) => {
      const { getOriginalEditingPage, reset } = useMultiPagePreviewStore.getState();
      const { page: originalPage, meta: originalMeta } = getOriginalEditingPage();

      // Restore the original page that was being edited before preview
      if (originalPage) {
        setCurrentPage(originalPage);
      }
      if (originalMeta) {
        setCurrentPageMeta(originalMeta);
      }

      // Reset the preview store
      reset();
    });

    setViewMode('edit');
    setIsMultiPagePreview(false);
  };

  const handleExport = () => {
    if (!currentPage) return;

    const dataStr = JSON.stringify(currentPage, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${currentPage.pageName.replace(/\s+/g, '-').toLowerCase()}-${Date.now()}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  // Handle import from JSON file
  const handleImport = (pageDefinition: PageDefinition) => {
    // Set the imported page as current
    setCurrentPage(pageDefinition);
    setSaveStatus('unsaved');

    // Update page metadata
    const pageSlug = pageDefinition.pageName.replace(/\s+/g, '-').toLowerCase();
    setCurrentPageMeta({
      id: Date.now(),
      siteId: siteId ? Number.parseInt(siteId) : 0,
      pageName: pageDefinition.pageName,
      pageSlug: pageSlug,
      pageType: 'standard',
      routePath: `/${pageSlug}`,
      displayOrder: 0,
      isPublished: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });

    // Show success message
    alert(`Page "${pageDefinition.pageName}" imported successfully! Remember to save your changes.`);
  };

  const handleCSSChange = (css: string) => {
    // Parse CSS and update selected component styles
    if (selectedComponentId && currentPage) {
      // This is a simplified version - in production, use a proper CSS parser
      const component = currentPage.components.find(c => c.instanceId === selectedComponentId);
      if (component) {
        // Update component styles
        useBuilderStore.getState().updateComponentStyles(selectedComponentId, component.styles);
      }
    }
  };

  const getCSSForSelectedComponent = (): string => {
    if (!selectedComponentId || !currentPage) return '';

    const component = currentPage.components.find(c => c.instanceId === selectedComponentId);
    if (!component) return '';

    // Convert styles object to CSS string
    return Object.entries(component.styles)
      .map(([property, value]) => `${property}: ${value};`)
      .join('\n');
  };

  const formatLastSaved = (): string => {
    if (!lastSaved) return 'Never';

    const now = new Date();
    const diff = now.getTime() - lastSaved.getTime();
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (seconds < 60) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return lastSaved.toLocaleDateString();
  };

  // Handle page selection from PageManager
  const handlePageSelect = async (page: Page) => {
    // Save current page first if there are unsaved changes
    if (saveStatus === 'unsaved' && currentPage) {
      const shouldSave = window.confirm('You have unsaved changes. Save before switching pages?');
      if (shouldSave) {
        await handleSave();
      }
    }

    // Update current page metadata
    setCurrentPageMeta(page);

    // Load the selected page
    if (siteId) {
      // Load from backend
      navigate(`/builder/sites/${siteId}/pages/${page.id}`);
    } else {
      // Demo mode - load from localStorage
      const savedPages = JSON.parse(localStorage.getItem('builder_saved_pages') || '{}');
      const pageData = savedPages[page.pageSlug];

      if (pageData) {
        setCurrentPage(pageData);
        setSaveStatus('saved');
        setLastSaved(pageData.savedAt ? new Date(pageData.savedAt) : null);
      } else {
        // Create empty page
        const newPage: PageDefinition = {
          version: '1.0',
          pageName: page.pageName,
          grid: {
            columns: 12,
            rows: 'auto',
            gap: '20px',
            minRowHeight: '50px'
          },
          components: [],
          globalStyles: {
            cssVariables: {},
            customCSS: ''
          }
        };
        setCurrentPage(newPage);
        setSaveStatus('unsaved');
      }
    }
  };

  // Handle page creation
  const handlePageCreate = (page: Page) => {
    console.log('Page created:', page);
    // Optionally navigate to the new page
    handlePageSelect(page);
  };

  // Handle page deletion
  const handlePageDelete = (pageId: number) => {
    // If we deleted the current page, load another page or create a new one
    if (currentPageMeta?.id === pageId) {
      initializeNewPage();
      setCurrentPageMeta(null);
    }
  };

  // Render multi-page preview mode
  if (viewMode === 'preview' && isMultiPagePreview) {
    return (
      <MultiPagePreview
        siteId={siteId ? Number.parseInt(siteId) : null}
        initialPages={sitePages}
        currentEditingPage={currentPageMeta}
        onExitPreview={handleExitMultiPagePreview}
      />
    );
  }

  return (
    <ClipboardProvider>
    <div className={`builder-page ${viewMode === 'preview' ? 'preview-mode' : ''}`}>
      {/* Top Menubar - Show minimal toolbar in preview mode */}
      {viewMode === 'preview' ? (
        <div className="builder-toolbar preview-toolbar">
          <div className="toolbar-center">
            <button
              className="toolbar-button primary-button"
              onClick={handlePreview}
              title="Exit Preview (Press P or Escape)"
            >
              Exit Preview
            </button>
          </div>
        </div>
      ) : (
        <BuilderMenubar
          siteId={siteId ? Number.parseInt(siteId) : null}
          pageId={pageId ? Number.parseInt(pageId) : null}
          currentPageMeta={currentPageMeta}
          saveStatus={saveStatus}
          onSave={handleSave}
          onPublish={handlePublish}
          onPreview={handlePreview}
          onExport={() => setShowExportModal(true)}
          onImport={handleImport}
          onShowCSSEditor={() => setShowCSSEditor(!showCSSEditor)}
          onPageSelect={handlePageSelect}
          onPageCreate={handlePageCreate}
          onShowImageRepository={() => setShowImageRepository(true)}
        />
      )}

      {/* Main Builder Area */}
      <div className={`builder-main ${viewMode === 'preview' ? 'preview-mode' : ''}`}>
        {/* Left Panel - Component Palette (hidden in preview mode) */}
        {viewMode === 'edit' && (
          <div className={`builder-panel left-panel ${leftPanelCollapsed ? 'collapsed' : ''}`}>
            <button
              className="panel-toggle"
              onClick={() => uiPreferences.toggleLeftPanel()}
              title={leftPanelCollapsed ? 'Show Components' : 'Hide Components'}
            >
              {leftPanelCollapsed ? '>' : '<'}
            </button>
            {!leftPanelCollapsed && (
              <LeftSidebar
                siteId={siteId ? Number.parseInt(siteId) : null}
                currentPageId={currentPageMeta?.id ?? (pageId ? Number.parseInt(pageId) : null)}
                onPageSelect={handlePageSelect}
                onPageCreate={handlePageCreate}
                onPageDelete={handlePageDelete}
              />
            )}
          </div>
        )}

        {/* Center - Canvas */}
        <div className="builder-canvas-area">
          {/* Canvas Ruler - only show in edit mode when enabled */}
          {viewMode === 'edit' && showRulers && <CanvasRuler />}

          {isLoading ? (
            <div className="builder-loading">
              <div className="loading-spinner"></div>
              <p>Loading page...</p>
            </div>
          ) : error ? (
            <div className="builder-error">
              <div className="error-icon">⚠️</div>
              <h3>Failed to load page</h3>
              <p>{error}</p>
              <button onClick={() => window.location.reload()} className="retry-button">
                Retry
              </button>
            </div>
          ) : (
            <BuilderCanvas onComponentSelect={(id) => useBuilderStore.getState().selectComponent(id)} />
          )}

          {/* CSS Editor Modal */}
          {showCSSEditor && selectedComponentId && (
            <div className="css-editor-modal">
              <div className="modal-content">
                <div className="modal-header">
                  <h3>CSS Editor</h3>
                  <button
                    className="close-button"
                    onClick={() => setShowCSSEditor(false)}
                    title="Close (Esc)"
                  >
                    ✕
                  </button>
                </div>
                <CSSEditor
                  value={getCSSForSelectedComponent()}
                  onChange={handleCSSChange}
                  componentId={selectedComponentId}
                  height="500px"
                />
              </div>
            </div>
          )}
        </div>

        {/* Right Panel - Properties (hidden in preview mode) */}
        {viewMode === 'edit' && (
          <div className={`builder-panel right-panel ${rightPanelCollapsed ? 'collapsed' : ''}`}>
            <button
              className="panel-toggle"
              onClick={() => uiPreferences.toggleRightPanel()}
              title={rightPanelCollapsed ? 'Show Properties' : 'Hide Properties'}
            >
              {rightPanelCollapsed ? '<' : '>'}
            </button>
            {!rightPanelCollapsed && <PropertiesPanel selectedComponentId={selectedComponentId} />}
          </div>
        )}
      </div>

      {/* Bottom Status Bar (hidden in preview mode) */}
      {viewMode === 'edit' && (
        <div className="builder-status-bar">
          <div className="status-left">
            <span className="status-item">
              Grid: {currentPage?.grid.columns} columns × {currentPage?.grid.gap} gap
            </span>
            <span className="status-item">
              Components: {currentPage?.components.length || 0}
            </span>
          </div>

          <div className="status-right">
            <span className="status-item">
              ✏ Edit Mode
            </span>
            {selectedComponentId && (
              <span className="status-item">
                Selected: {currentPage?.components.find(c => c.instanceId === selectedComponentId)?.componentId}
              </span>
            )}
          </div>
        </div>
      )}

      {/* Keyboard Shortcuts Help (hidden in preview mode) */}
      {viewMode === 'edit' && (
        <div className="keyboard-shortcuts-hint">
          <small>
            Shortcuts: Ctrl+S (Save) | Ctrl+Z (Undo) | Ctrl+Shift+Z (Redo) | Ctrl+E (CSS Editor) | Delete (Remove)
          </small>
        </div>
      )}

      {/* Image Repository Modal */}
      <ImageRepositoryModal
        isOpen={showImageRepository}
        onClose={() => setShowImageRepository(false)}
      />

      {/* Export Modal */}
      <ExportModal
        isOpen={showExportModal}
        onClose={() => setShowExportModal(false)}
        siteId={siteId ? Number.parseInt(siteId) : null}
        currentPageMeta={currentPageMeta}
        onSaveBeforeExport={handleSave}
        hasUnsavedChanges={saveStatus === 'unsaved'}
      />
    </div>
    </ClipboardProvider>
  );
};
