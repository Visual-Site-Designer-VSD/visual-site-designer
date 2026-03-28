import React, { useState, useEffect } from 'react';
import type { RendererProps, Article } from '../types';

/**
 * ArticleViewerRenderer
 *
 * Displays a single article with full content.
 * Reads the article ID from the URL path (e.g., /article/123 -> extracts "123").
 * Public component - no authentication required.
 */
const ArticleViewerRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};

  const apiEndpoint = String(props.apiEndpoint || '/api/proxy/articles');
  const showAuthor = props.showAuthor !== false;
  const showDate = props.showDate !== false;
  const showTags = props.showTags !== false;
  const showCoverImage = props.showCoverImage !== false;
  const notFoundMessage = String(props.notFoundMessage || 'Article not found.');
  const backLinkText = String(props.backLinkText || 'Back to articles');
  const backLinkUrl = String(props.backLinkUrl || '/');

  const [article, setArticle] = useState<Article | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  const containerStyle: React.CSSProperties = {
    maxWidth: styles.maxWidth || '768px',
    padding: styles.padding || '24px',
    margin: styles.margin || '0 auto',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  };

  // Extract article ID from URL path
  const extractArticleId = (): string | null => {
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    // Look for the last numeric or alphanumeric segment after "article"
    const articleIndex = pathParts.findIndex(p => p === 'article' || p === 'articles');
    if (articleIndex >= 0 && articleIndex < pathParts.length - 1) {
      return pathParts[articleIndex + 1];
    }
    // Fallback: use the last path segment
    return pathParts[pathParts.length - 1] || null;
  };

  useEffect(() => {
    if (isEditMode) return;

    const articleId = extractArticleId();
    if (!articleId) {
      setNotFound(true);
      return;
    }

    const fetchArticle = async () => {
      setIsLoading(true);
      setError(null);
      setNotFound(false);
      try {
        const response = await fetch(`${apiEndpoint}/${articleId}`, {
          credentials: 'same-origin',
        });

        if (response.status === 404) {
          setNotFound(true);
          return;
        }

        if (!response.ok) {
          throw new Error(`Failed to fetch article (${response.status})`);
        }

        const data: Article = await response.json();
        setArticle(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load article');
      } finally {
        setIsLoading(false);
      }
    };

    fetchArticle();
  }, [isEditMode, apiEndpoint]);

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  // Edit mode: show sample article preview
  if (isEditMode) {
    return (
      <div className="article-viewer article-viewer--edit" style={containerStyle}>
        <a href="#" onClick={(e) => e.preventDefault()} style={{
          color: '#4f46e5',
          textDecoration: 'none',
          fontSize: '14px',
          display: 'inline-flex',
          alignItems: 'center',
          gap: '4px',
          marginBottom: '24px',
        }}>
          &#8592; {backLinkText}
        </a>

        {showCoverImage && (
          <div style={{
            width: '100%',
            height: '300px',
            backgroundColor: '#f3f4f6',
            borderRadius: '12px',
            marginBottom: '24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '48px',
            opacity: 0.3,
          }}>
            &#128240;
          </div>
        )}

        <h1 style={{ fontSize: '32px', fontWeight: 700, color: '#111827', margin: '0 0 16px 0', lineHeight: 1.2 }}>
          Sample Article Title
        </h1>

        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '24px', color: '#6b7280', fontSize: '14px' }}>
          {showAuthor && (
            <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{
                width: 28, height: 28, borderRadius: '50%', backgroundColor: '#4f46e5',
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                color: 'white', fontSize: '12px', fontWeight: 600,
              }}>JD</span>
              Jane Doe
            </span>
          )}
          {showDate && <span>January 15, 2024</span>}
        </div>

        {showTags && (
          <div style={{ display: 'flex', gap: '8px', marginBottom: '24px', flexWrap: 'wrap' }}>
            {['react', 'tutorial', 'web-dev'].map((tag) => (
              <span key={tag} style={{
                padding: '4px 12px', backgroundColor: '#f3f4f6', borderRadius: '16px',
                fontSize: '13px', color: '#4b5563',
              }}>{tag}</span>
            ))}
          </div>
        )}

        <div className="article-viewer__content" style={{
          fontSize: '16px', lineHeight: 1.8, color: '#374151',
        }}>
          <p>This is a preview of how an article will look when rendered. The actual content will be loaded from the API when the page is viewed.</p>
          <p>The article viewer supports <strong>rich HTML content</strong> including headings, paragraphs, images, code blocks, and more. Content is rendered with clean typography optimized for reading.</p>
          <h2 style={{ fontSize: '24px', fontWeight: 600, marginTop: '32px' }}>Section Heading</h2>
          <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>
        </div>
      </div>
    );
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="article-viewer article-viewer--loading" style={containerStyle}>
        <div style={{ animation: 'pulse 1.5s ease-in-out infinite' }}>
          <div style={{ width: '60%', height: '14px', backgroundColor: '#e5e7eb', borderRadius: '4px', marginBottom: '24px' }} />
          <div style={{ width: '100%', height: '300px', backgroundColor: '#f3f4f6', borderRadius: '12px', marginBottom: '24px' }} />
          <div style={{ width: '80%', height: '32px', backgroundColor: '#e5e7eb', borderRadius: '4px', marginBottom: '16px' }} />
          <div style={{ width: '40%', height: '14px', backgroundColor: '#e5e7eb', borderRadius: '4px', marginBottom: '32px' }} />
          <div style={{ width: '100%', height: '14px', backgroundColor: '#f3f4f6', borderRadius: '4px', marginBottom: '8px' }} />
          <div style={{ width: '95%', height: '14px', backgroundColor: '#f3f4f6', borderRadius: '4px', marginBottom: '8px' }} />
          <div style={{ width: '88%', height: '14px', backgroundColor: '#f3f4f6', borderRadius: '4px', marginBottom: '8px' }} />
        </div>
      </div>
    );
  }

  // Not found
  if (notFound) {
    return (
      <div className="article-viewer article-viewer--not-found" style={containerStyle}>
        <div style={{ textAlign: 'center', padding: '60px 20px' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px', opacity: 0.3 }}>&#128240;</div>
          <p style={{ fontSize: '18px', color: '#6b7280', margin: '0 0 16px 0' }}>{notFoundMessage}</p>
          <a href={backLinkUrl} style={{
            color: '#4f46e5', textDecoration: 'none', fontSize: '14px',
            display: 'inline-flex', alignItems: 'center', gap: '4px',
          }}>
            &#8592; {backLinkText}
          </a>
        </div>
      </div>
    );
  }

  // Error
  if (error) {
    return (
      <div className="article-viewer article-viewer--error" style={containerStyle}>
        <div style={{
          textAlign: 'center', padding: '40px 20px', color: '#ef4444',
          backgroundColor: '#fef2f2', borderRadius: '12px',
        }}>
          <p style={{ margin: '0 0 12px 0', fontWeight: 500 }}>{error}</p>
          <a href={backLinkUrl} style={{ color: '#4f46e5', textDecoration: 'none', fontSize: '14px' }}>
            &#8592; {backLinkText}
          </a>
        </div>
      </div>
    );
  }

  if (!article) return null;

  return (
    <div className="article-viewer" style={containerStyle}>
      {/* Back link */}
      <a href={backLinkUrl} className="article-viewer__back" style={{
        color: '#4f46e5', textDecoration: 'none', fontSize: '14px',
        display: 'inline-flex', alignItems: 'center', gap: '4px', marginBottom: '24px',
      }}>
        &#8592; {backLinkText}
      </a>

      {/* Cover image */}
      {showCoverImage && article.coverImageUrl && (
        <img
          src={article.coverImageUrl}
          alt={article.title}
          className="article-viewer__cover"
          style={{
            width: '100%',
            maxHeight: '400px',
            objectFit: 'cover',
            borderRadius: '12px',
            marginBottom: '24px',
          }}
        />
      )}

      {/* Title */}
      <h1 className="article-viewer__title" style={{
        fontSize: '32px', fontWeight: 700, color: '#111827',
        margin: '0 0 16px 0', lineHeight: 1.2,
      }}>
        {article.title}
      </h1>

      {/* Meta row */}
      <div className="article-viewer__meta" style={{
        display: 'flex', alignItems: 'center', gap: '16px',
        marginBottom: '24px', color: '#6b7280', fontSize: '14px', flexWrap: 'wrap',
      }}>
        {showAuthor && article.author && (
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            {article.author.avatarUrl ? (
              <img
                src={article.author.avatarUrl}
                alt={article.author.name}
                style={{ width: 28, height: 28, borderRadius: '50%', objectFit: 'cover' }}
              />
            ) : (
              <span style={{
                width: 28, height: 28, borderRadius: '50%', backgroundColor: '#4f46e5',
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                color: 'white', fontSize: '12px', fontWeight: 600,
              }}>
                {article.author.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)}
              </span>
            )}
            {article.author.name}
          </span>
        )}
        {showDate && (article.publishedAt || article.createdAt) && (
          <span>{formatDate(article.publishedAt || article.createdAt)}</span>
        )}
      </div>

      {/* Tags */}
      {showTags && article.tags && article.tags.length > 0 && (
        <div className="article-viewer__tags" style={{
          display: 'flex', gap: '8px', marginBottom: '32px', flexWrap: 'wrap',
        }}>
          {article.tags.map((tag) => (
            <span key={tag} style={{
              padding: '4px 12px', backgroundColor: '#f3f4f6', borderRadius: '16px',
              fontSize: '13px', color: '#4b5563',
            }}>{tag}</span>
          ))}
        </div>
      )}

      {/* Article content (rendered HTML) */}
      <div
        className="article-viewer__content"
        style={{
          fontSize: '16px',
          lineHeight: 1.8,
          color: '#374151',
        }}
        dangerouslySetInnerHTML={{ __html: article.content }}
      />
    </div>
  );
};

export default ArticleViewerRenderer;
