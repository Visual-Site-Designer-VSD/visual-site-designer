import React, { useState, useEffect, useCallback } from 'react';
import type { RendererProps, Article, ArticleListResponse } from '../types';

/**
 * ArticleListRenderer
 *
 * Displays a paginated list/grid of article cards.
 * Public component - no authentication required to view.
 *
 * Layouts:
 * - cards: Responsive grid of article cards with cover images
 * - list: Vertical list with horizontal card layout
 * - minimal: Text-only compact list
 */
const ArticleListRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};

  const apiEndpoint = String(props.apiEndpoint || '/api/proxy/articles');
  const pageSize = Number(props.pageSize || 10);
  const layout = String(props.layout || 'cards');
  const showAuthor = props.showAuthor !== false;
  const showDate = props.showDate !== false;
  const showSummary = props.showSummary !== false;
  const showCoverImage = props.showCoverImage !== false;
  const showTags = props.showTags !== false;
  const showPagination = props.showPagination !== false;
  const articleLinkPattern = String(props.articleLinkPattern || '/article/{id}');
  const emptyMessage = String(props.emptyMessage || 'No articles found.');
  const columns = Number(props.columns || 3);

  const [articles, setArticles] = useState<Article[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchArticles = useCallback(async (page: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const separator = apiEndpoint.includes('?') ? '&' : '?';
      const url = `${apiEndpoint}${separator}page=${page}&size=${pageSize}`;
      const response = await fetch(url, { credentials: 'same-origin' });

      if (!response.ok) {
        throw new Error(`Failed to fetch articles (${response.status})`);
      }

      const data: ArticleListResponse = await response.json();
      setArticles(data.content || []);
      setTotalPages(data.totalPages || 0);
      setCurrentPage(data.page || page);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load articles');
    } finally {
      setIsLoading(false);
    }
  }, [apiEndpoint, pageSize]);

  useEffect(() => {
    if (!isEditMode) {
      fetchArticles(0);
    }
  }, [isEditMode, fetchArticles]);

  const getArticleUrl = (article: Article) => {
    return articleLinkPattern.replace('{id}', article.id);
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      return new Date(dateStr).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  const containerStyle: React.CSSProperties = {
    padding: styles.padding || '16px',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  };

  // Edit mode: show placeholder article cards
  if (isEditMode) {
    const placeholders = [
      { id: '1', title: 'Getting Started with React', summary: 'Learn the fundamentals of React and build your first component.', author: { id: '1', name: 'Jane Smith' }, tags: ['react', 'tutorial'], publishedAt: '2024-01-15' },
      { id: '2', title: 'Understanding Spring Boot Security', summary: 'A deep dive into Spring Security OAuth2 integration.', author: { id: '2', name: 'John Doe' }, tags: ['spring', 'security'], publishedAt: '2024-01-10' },
      { id: '3', title: 'Building a Blog with VSD CMS', summary: 'Step-by-step guide to building a blog using drag-and-drop.', author: { id: '3', name: 'Alex Johnson' }, tags: ['vsd', 'cms', 'blog'], publishedAt: '2024-01-05' },
    ];

    return (
      <div className="article-list article-list--edit" style={containerStyle}>
        <div
          className={`article-list__grid article-list__grid--${layout}`}
          style={{
            display: layout === 'cards' ? 'grid' : 'flex',
            gridTemplateColumns: layout === 'cards' ? `repeat(${columns}, 1fr)` : undefined,
            flexDirection: layout !== 'cards' ? 'column' : undefined,
            gap: styles.gap || '24px',
          }}
        >
          {placeholders.map((article) => (
            <ArticleCard
              key={article.id}
              article={article as Article}
              layout={layout}
              showCoverImage={showCoverImage}
              showAuthor={showAuthor}
              showDate={showDate}
              showSummary={showSummary}
              showTags={showTags}
              getArticleUrl={getArticleUrl}
              formatDate={formatDate}
              isEditMode={true}
            />
          ))}
        </div>
      </div>
    );
  }

  // Loading state
  if (isLoading && articles.length === 0) {
    return (
      <div className="article-list article-list--loading" style={containerStyle}>
        <div style={{
          display: layout === 'cards' ? 'grid' : 'flex',
          gridTemplateColumns: layout === 'cards' ? `repeat(${columns}, 1fr)` : undefined,
          flexDirection: layout !== 'cards' ? 'column' : undefined,
          gap: styles.gap || '24px',
        }}>
          {[1, 2, 3].map((i) => (
            <div key={i} className="article-list__skeleton" style={{
              backgroundColor: '#f3f4f6',
              borderRadius: '12px',
              height: layout === 'cards' ? '320px' : '120px',
              animation: 'pulse 1.5s ease-in-out infinite',
            }} />
          ))}
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="article-list article-list--error" style={containerStyle}>
        <div style={{
          textAlign: 'center',
          padding: '40px 20px',
          color: '#ef4444',
          backgroundColor: '#fef2f2',
          borderRadius: '12px',
        }}>
          <p style={{ margin: 0, fontWeight: 500 }}>{error}</p>
          <button
            onClick={() => fetchArticles(currentPage)}
            style={{
              marginTop: '12px',
              padding: '8px 16px',
              border: '1px solid #ef4444',
              borderRadius: '6px',
              backgroundColor: 'white',
              color: '#ef4444',
              cursor: 'pointer',
              fontSize: '14px',
            }}
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  // Empty state
  if (articles.length === 0) {
    return (
      <div className="article-list article-list--empty" style={containerStyle}>
        <div style={{
          textAlign: 'center',
          padding: '60px 20px',
          color: '#6b7280',
        }}>
          <p style={{ margin: 0, fontSize: '16px' }}>{emptyMessage}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="article-list" style={containerStyle}>
      <div
        className={`article-list__grid article-list__grid--${layout}`}
        style={{
          display: layout === 'cards' ? 'grid' : 'flex',
          gridTemplateColumns: layout === 'cards' ? `repeat(${columns}, 1fr)` : undefined,
          flexDirection: layout !== 'cards' ? 'column' : undefined,
          gap: styles.gap || '24px',
        }}
      >
        {articles.map((article) => (
          <ArticleCard
            key={article.id}
            article={article}
            layout={layout}
            showCoverImage={showCoverImage}
            showAuthor={showAuthor}
            showDate={showDate}
            showSummary={showSummary}
            showTags={showTags}
            getArticleUrl={getArticleUrl}
            formatDate={formatDate}
            isEditMode={false}
          />
        ))}
      </div>

      {showPagination && totalPages > 1 && (
        <div className="article-list__pagination" style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          gap: '8px',
          marginTop: '32px',
          padding: '16px 0',
        }}>
          <button
            onClick={() => fetchArticles(currentPage - 1)}
            disabled={currentPage === 0}
            className="article-list__page-btn"
            style={{
              padding: '8px 14px',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              backgroundColor: 'white',
              cursor: currentPage === 0 ? 'not-allowed' : 'pointer',
              opacity: currentPage === 0 ? 0.5 : 1,
              fontSize: '14px',
            }}
          >
            Previous
          </button>

          <span style={{ fontSize: '14px', color: '#6b7280', padding: '0 8px' }}>
            Page {currentPage + 1} of {totalPages}
          </span>

          <button
            onClick={() => fetchArticles(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="article-list__page-btn"
            style={{
              padding: '8px 14px',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              backgroundColor: 'white',
              cursor: currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer',
              opacity: currentPage >= totalPages - 1 ? 0.5 : 1,
              fontSize: '14px',
            }}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

// ==================== ArticleCard Sub-component ====================

interface ArticleCardProps {
  article: Article;
  layout: string;
  showCoverImage: boolean;
  showAuthor: boolean;
  showDate: boolean;
  showSummary: boolean;
  showTags: boolean;
  getArticleUrl: (article: Article) => string;
  formatDate: (dateStr?: string) => string;
  isEditMode: boolean;
}

const ArticleCard: React.FC<ArticleCardProps> = ({
  article,
  layout,
  showCoverImage,
  showAuthor,
  showDate,
  showSummary,
  showTags,
  getArticleUrl,
  formatDate,
  isEditMode,
}) => {
  const url = getArticleUrl(article);

  if (layout === 'minimal') {
    return (
      <a
        href={isEditMode ? '#' : url}
        onClick={isEditMode ? (e) => e.preventDefault() : undefined}
        className="article-card article-card--minimal"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'baseline',
          padding: '12px 0',
          borderBottom: '1px solid #e5e7eb',
          textDecoration: 'none',
          color: 'inherit',
        }}
      >
        <span style={{ fontWeight: 500, color: '#111827' }}>{article.title}</span>
        {showDate && (
          <span style={{ fontSize: '13px', color: '#9ca3af', flexShrink: 0, marginLeft: '16px' }}>
            {formatDate(article.publishedAt || article.createdAt)}
          </span>
        )}
      </a>
    );
  }

  const isListLayout = layout === 'list';
  const placeholderBg = `hsl(${(parseInt(article.id, 10) || 0) * 60 % 360}, 40%, 90%)`;

  return (
    <a
      href={isEditMode ? '#' : url}
      onClick={isEditMode ? (e) => e.preventDefault() : undefined}
      className={`article-card article-card--${layout}`}
      style={{
        display: isListLayout ? 'flex' : 'block',
        flexDirection: isListLayout ? 'row' : undefined,
        gap: isListLayout ? '20px' : undefined,
        backgroundColor: '#ffffff',
        borderRadius: '12px',
        overflow: 'hidden',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06)',
        textDecoration: 'none',
        color: 'inherit',
        transition: 'box-shadow 0.2s ease, transform 0.2s ease',
      }}
      onMouseEnter={(e) => {
        if (!isEditMode) {
          e.currentTarget.style.boxShadow = '0 10px 15px rgba(0,0,0,0.1), 0 4px 6px rgba(0,0,0,0.05)';
          e.currentTarget.style.transform = 'translateY(-2px)';
        }
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06)';
        e.currentTarget.style.transform = 'translateY(0)';
      }}
    >
      {/* Cover Image */}
      {showCoverImage && (
        <div style={{
          width: isListLayout ? '200px' : '100%',
          height: isListLayout ? '140px' : '180px',
          flexShrink: 0,
          backgroundColor: placeholderBg,
          overflow: 'hidden',
        }}>
          {article.coverImageUrl ? (
            <img
              src={article.coverImageUrl}
              alt={article.title}
              style={{
                width: '100%',
                height: '100%',
                objectFit: 'cover',
              }}
            />
          ) : (
            <div style={{
              width: '100%',
              height: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '32px',
              opacity: 0.3,
            }}>
              &#128240;
            </div>
          )}
        </div>
      )}

      {/* Content */}
      <div style={{ padding: '16px', flex: 1 }}>
        <h3 style={{
          margin: '0 0 8px 0',
          fontSize: layout === 'cards' ? '16px' : '18px',
          fontWeight: 600,
          color: '#111827',
          lineHeight: 1.3,
        }}>
          {article.title}
        </h3>

        {showSummary && article.summary && (
          <p style={{
            margin: '0 0 12px 0',
            fontSize: '14px',
            color: '#6b7280',
            lineHeight: 1.5,
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }}>
            {article.summary}
          </p>
        )}

        {/* Meta row */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          fontSize: '13px',
          color: '#9ca3af',
          flexWrap: 'wrap',
        }}>
          {showAuthor && article.author && (
            <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
              {article.author.avatarUrl ? (
                <img
                  src={article.author.avatarUrl}
                  alt={article.author.name}
                  style={{ width: 20, height: 20, borderRadius: '50%', objectFit: 'cover' }}
                />
              ) : (
                <span style={{
                  width: 20, height: 20, borderRadius: '50%', backgroundColor: '#e5e7eb',
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '10px', fontWeight: 600, color: '#6b7280',
                }}>
                  {article.author.name[0]?.toUpperCase()}
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
          <div style={{
            display: 'flex',
            gap: '6px',
            flexWrap: 'wrap',
            marginTop: '10px',
          }}>
            {article.tags.map((tag) => (
              <span
                key={tag}
                style={{
                  padding: '2px 8px',
                  backgroundColor: '#f3f4f6',
                  borderRadius: '12px',
                  fontSize: '12px',
                  color: '#4b5563',
                }}
              >
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>
    </a>
  );
};

export default ArticleListRenderer;
