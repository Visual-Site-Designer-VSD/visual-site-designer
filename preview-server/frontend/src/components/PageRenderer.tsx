import { useState, useEffect } from 'react';
import ComponentRenderer from './ComponentRenderer';

interface PageRendererProps {
  pageName: string;
}

export default function PageRenderer({ pageName }: PageRendererProps) {
  const [pageData, setPageData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);

    fetch(`/api/pages/${pageName}`, { credentials: 'same-origin' })
      .then(res => {
        if (!res.ok) throw new Error('Page not found');
        return res.json();
      })
      .then(data => {
        setPageData(data);
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  }, [pageName]);

  if (loading) {
    return (
      <div className="page-loading" style={{
        display: 'flex', justifyContent: 'center', alignItems: 'center',
        minHeight: '200px', color: '#666',
      }}>
        Loading...
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-error" style={{
        display: 'flex', justifyContent: 'center', alignItems: 'center',
        minHeight: '200px', color: '#e53e3e',
      }}>
        Error: {error}
      </div>
    );
  }

  if (!pageData) return null;

  const components = pageData.components || [];
  const dataSources = pageData.data || {};

  return (
    <div className="page" data-page={pageName}>
      {components.map((component: any) => (
        <ComponentRenderer
          key={component.instanceId}
          component={component}
          dataSources={dataSources}
        />
      ))}
    </div>
  );
}
