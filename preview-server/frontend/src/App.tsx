import { useState, useEffect } from 'react';
import ContextProviderTree from './components/ContextProviderTree';
import PageRenderer from './components/PageRenderer';
import { loadPlugins } from './services/pluginLoader';

interface PageMeta {
  pageName: string;
  title: string;
  path: string;
  description?: string;
}

function useRouter() {
  const [currentPath, setCurrentPath] = useState(window.location.pathname);

  useEffect(() => {
    const handlePopState = () => setCurrentPath(window.location.pathname);
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  return currentPath;
}

export default function App() {
  const currentPath = useRouter();
  const [pages, setPages] = useState<PageMeta[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch page list and load plugin bundles
  useEffect(() => {
    async function init() {
      try {
        // Fetch page list
        const pagesRes = await fetch('/api/pages', { credentials: 'same-origin' });
        if (!pagesRes.ok) throw new Error('Failed to load pages');
        const pageList: PageMeta[] = await pagesRes.json();
        setPages(pageList);

        // Discover plugin IDs from page manifest
        try {
          const manifestRes = await fetch('/api/pages/_manifest', { credentials: 'same-origin' });
          if (manifestRes.ok) {
            const manifest = await manifestRes.json();
            const pluginIds: string[] = manifest.plugins || [];
            if (pluginIds.length > 0) {
              await loadPlugins(pluginIds);
            }
          }
        } catch {
          // Manifest is optional — plugins may also be loaded via ContextProviderTree
        }
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }

    init();
  }, []);

  if (loading) {
    return (
      <div style={{
        display: 'flex', justifyContent: 'center', alignItems: 'center',
        minHeight: '100vh', fontFamily: 'system-ui, sans-serif', color: '#666',
      }}>
        Loading preview...
      </div>
    );
  }

  if (error) {
    return (
      <div style={{
        display: 'flex', justifyContent: 'center', alignItems: 'center',
        minHeight: '100vh', fontFamily: 'system-ui, sans-serif', color: '#e53e3e',
      }}>
        Error: {error}
      </div>
    );
  }

  // Find matching route
  let matchedPage = pages.find(p => p.path === currentPath || p.path === currentPath.replace(/\/$/, ''));
  if (!matchedPage && pages.length > 0) {
    matchedPage = pages.find(p => p.path === '/') || pages[0];
  }

  return (
    <ContextProviderTree>
      {matchedPage ? (
        <PageRenderer key={matchedPage.pageName} pageName={matchedPage.pageName} />
      ) : (
        <div style={{
          display: 'flex', justifyContent: 'center', alignItems: 'center',
          minHeight: '100vh', fontFamily: 'system-ui, sans-serif',
        }}>
          404 - Page Not Found
        </div>
      )}
    </ContextProviderTree>
  );
}
