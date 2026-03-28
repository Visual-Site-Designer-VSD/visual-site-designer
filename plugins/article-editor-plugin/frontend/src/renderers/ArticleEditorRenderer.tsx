import React, { useState, useCallback } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import type { RendererProps, ArticlePayload } from '../types';
import { useAuthContext } from '../hooks/useAuthContext';

/**
 * ArticleEditorRenderer
 *
 * Medium-like rich text editor for writing articles.
 * Uses TipTap (bundled) for the editing experience.
 * Requires authentication - shows login prompt when not authenticated.
 */
const ArticleEditorRenderer: React.FC<RendererProps> = ({ component, isEditMode }) => {
  const props = component.props || {};
  const styles = component.styles || {};

  const apiEndpoint = String(props.apiEndpoint || '/api/proxy/articles');
  const placeholder = String(props.placeholder || 'Start writing your story...');
  const showToolbar = props.showToolbar !== false;
  const showTitleField = props.showTitleField !== false;
  const showTagsField = props.showTagsField !== false;
  const showCoverImage = props.showCoverImage !== false;
  const showSaveDraft = props.showSaveDraft !== false;
  const showPublish = props.showPublish !== false;
  const redirectAfterPublish = String(props.redirectAfterPublish || '/');
  const titlePlaceholder = String(props.titlePlaceholder || 'Article title...');
  const tagsPlaceholder = String(props.tagsPlaceholder || 'Add tags separated by commas...');
  const coverImagePlaceholder = String(props.coverImagePlaceholder || 'Cover image URL...');
  const saveDraftText = String(props.saveDraftText || 'Save Draft');
  const publishText = String(props.publishText || 'Publish');
  const loginPromptText = String(props.loginPromptText || 'Please sign in to write articles.');
  const loginButtonText = String(props.loginButtonText || 'Sign In');
  const loginUrl = String(props.loginUrl || '/login');

  const auth = useAuthContext();

  const [title, setTitle] = useState('');
  const [tags, setTags] = useState('');
  const [coverImageUrl, setCoverImageUrl] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState<string | null>(null);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({
        heading: { levels: [1, 2, 3] },
      }),
      Image.configure({ inline: false, allowBase64: true }),
      Link.configure({ openOnClick: false, autolink: true }),
      Placeholder.configure({ placeholder }),
    ],
    editable: !isEditMode,
    content: '',
  });

  const containerStyle: React.CSSProperties = {
    maxWidth: styles.maxWidth || '768px',
    padding: styles.padding || '24px',
    margin: styles.margin || '0 auto',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  };

  const handleSave = useCallback(async (status: 'draft' | 'published') => {
    if (!editor) return;

    const content = editor.getHTML();
    if (!title.trim()) {
      setSaveError('Please enter a title.');
      return;
    }
    if (content === '<p></p>' || !content.trim()) {
      setSaveError('Please write some content.');
      return;
    }

    setIsSaving(true);
    setSaveError(null);
    setSaveSuccess(null);

    const payload: ArticlePayload = {
      title: title.trim(),
      content,
      summary: extractSummary(content),
      coverImageUrl: coverImageUrl.trim() || undefined,
      tags: tags.split(',').map(t => t.trim()).filter(Boolean),
      status,
    };

    try {
      const response = await fetch(apiEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || `Failed to save (${response.status})`);
      }

      if (status === 'published') {
        setSaveSuccess('Article published!');
        setTimeout(() => {
          window.location.href = redirectAfterPublish;
        }, 1000);
      } else {
        setSaveSuccess('Draft saved.');
        setTimeout(() => setSaveSuccess(null), 3000);
      }
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Failed to save article');
    } finally {
      setIsSaving(false);
    }
  }, [editor, title, tags, coverImageUrl, apiEndpoint, redirectAfterPublish]);

  // Extract first 200 chars of text content as summary
  const extractSummary = (html: string): string => {
    const div = document.createElement('div');
    div.innerHTML = html;
    const text = div.textContent || '';
    return text.substring(0, 200).trim() + (text.length > 200 ? '...' : '');
  };

  const addImage = useCallback(() => {
    if (!editor) return;
    const url = window.prompt('Enter image URL:');
    if (url) {
      editor.chain().focus().setImage({ src: url }).run();
    }
  }, [editor]);

  const addLink = useCallback(() => {
    if (!editor) return;
    const url = window.prompt('Enter link URL:');
    if (url) {
      editor.chain().focus().extendMarkRange('link').setLink({ href: url }).run();
    }
  }, [editor]);

  // Edit mode: show non-functional preview
  if (isEditMode) {
    return (
      <div className="article-editor article-editor--edit" style={containerStyle}>
        {showTitleField && (
          <input
            type="text"
            placeholder={titlePlaceholder}
            disabled
            style={{
              width: '100%',
              fontSize: '28px',
              fontWeight: 700,
              border: 'none',
              outline: 'none',
              padding: '8px 0',
              marginBottom: '16px',
              backgroundColor: 'transparent',
              color: '#111827',
            }}
          />
        )}
        {showCoverImage && (
          <input
            type="text"
            placeholder={coverImagePlaceholder}
            disabled
            style={{
              width: '100%',
              fontSize: '14px',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              padding: '10px 12px',
              marginBottom: '12px',
              backgroundColor: '#f9fafb',
            }}
          />
        )}
        {showToolbar && (
          <div style={{
            display: 'flex',
            gap: '4px',
            padding: '8px',
            borderBottom: '1px solid #e5e7eb',
            marginBottom: '16px',
            flexWrap: 'wrap',
          }}>
            {['B', 'I', 'S', 'H1', 'H2', 'H3', '""', '[]', '-', 'img', 'link', '< >'].map((btn) => (
              <button key={btn} disabled style={{
                padding: '4px 8px',
                border: '1px solid #e5e7eb',
                borderRadius: '4px',
                backgroundColor: 'white',
                fontSize: '12px',
                cursor: 'not-allowed',
                color: '#6b7280',
                fontWeight: btn === 'B' ? 700 : btn === 'I' ? 400 : 500,
                fontStyle: btn === 'I' ? 'italic' : 'normal',
                textDecoration: btn === 'S' ? 'line-through' : 'none',
              }}>
                {btn}
              </button>
            ))}
          </div>
        )}
        <div style={{
          minHeight: '200px',
          padding: '16px',
          color: '#9ca3af',
          fontSize: '16px',
          lineHeight: 1.8,
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
        }}>
          {placeholder}
        </div>
        {showTagsField && (
          <input
            type="text"
            placeholder={tagsPlaceholder}
            disabled
            style={{
              width: '100%',
              fontSize: '14px',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              padding: '10px 12px',
              marginTop: '16px',
              backgroundColor: '#f9fafb',
            }}
          />
        )}
        <div style={{ display: 'flex', gap: '12px', marginTop: '20px', justifyContent: 'flex-end' }}>
          {showSaveDraft && (
            <button disabled style={{
              padding: '10px 20px', border: '1px solid #d1d5db', borderRadius: '8px',
              backgroundColor: 'white', fontSize: '14px', cursor: 'not-allowed',
            }}>{saveDraftText}</button>
          )}
          {showPublish && (
            <button disabled style={{
              padding: '10px 20px', border: 'none', borderRadius: '8px',
              backgroundColor: '#4f46e5', color: 'white', fontSize: '14px', cursor: 'not-allowed',
              opacity: 0.6,
            }}>{publishText}</button>
          )}
        </div>
      </div>
    );
  }

  // Not authenticated: show login prompt
  if (!auth || !auth.isAuthenticated) {
    return (
      <div className="article-editor article-editor--unauthenticated" style={containerStyle}>
        <div style={{
          textAlign: 'center',
          padding: '60px 20px',
          backgroundColor: '#f9fafb',
          borderRadius: '12px',
          border: '1px solid #e5e7eb',
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>&#9998;</div>
          <p style={{ fontSize: '18px', color: '#374151', margin: '0 0 20px 0' }}>
            {loginPromptText}
          </p>
          <a
            href={loginUrl}
            onClick={(e) => {
              if (auth && 'providers' in auth && Array.isArray((auth as any).providers) && (auth as any).providers.length > 0) {
                e.preventDefault();
                auth.login();
              }
            }}
            style={{
              display: 'inline-block',
              padding: '12px 24px',
              backgroundColor: '#4f46e5',
              color: 'white',
              borderRadius: '8px',
              textDecoration: 'none',
              fontSize: '16px',
              fontWeight: 500,
            }}
          >
            {loginButtonText}
          </a>
        </div>
      </div>
    );
  }

  // Authenticated: show editor
  return (
    <div className="article-editor" style={containerStyle}>
      {/* Title */}
      {showTitleField && (
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={titlePlaceholder}
          className="article-editor__title"
          style={{
            width: '100%',
            fontSize: '28px',
            fontWeight: 700,
            border: 'none',
            outline: 'none',
            padding: '8px 0',
            marginBottom: '16px',
            backgroundColor: 'transparent',
            color: '#111827',
          }}
        />
      )}

      {/* Cover Image URL */}
      {showCoverImage && (
        <input
          type="text"
          value={coverImageUrl}
          onChange={(e) => setCoverImageUrl(e.target.value)}
          placeholder={coverImagePlaceholder}
          className="article-editor__cover-input"
          style={{
            width: '100%',
            fontSize: '14px',
            border: '1px solid #e5e7eb',
            borderRadius: '8px',
            padding: '10px 12px',
            marginBottom: '12px',
          }}
        />
      )}

      {/* Cover Image Preview */}
      {coverImageUrl && (
        <img
          src={coverImageUrl}
          alt="Cover preview"
          style={{
            width: '100%',
            maxHeight: '250px',
            objectFit: 'cover',
            borderRadius: '8px',
            marginBottom: '16px',
          }}
          onError={(e) => { e.currentTarget.style.display = 'none'; }}
        />
      )}

      {/* Toolbar */}
      {showToolbar && editor && (
        <div className="article-editor__toolbar" style={{
          display: 'flex',
          gap: '4px',
          padding: '8px',
          borderTop: '1px solid #e5e7eb',
          borderBottom: '1px solid #e5e7eb',
          marginBottom: '16px',
          flexWrap: 'wrap',
          position: 'sticky',
          top: 0,
          backgroundColor: 'white',
          zIndex: 10,
        }}>
          <ToolbarButton
            onClick={() => editor.chain().focus().toggleBold().run()}
            active={editor.isActive('bold')}
            title="Bold"
          >
            <strong>B</strong>
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().toggleItalic().run()}
            active={editor.isActive('italic')}
            title="Italic"
          >
            <em>I</em>
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().toggleStrike().run()}
            active={editor.isActive('strike')}
            title="Strikethrough"
          >
            <s>S</s>
          </ToolbarButton>

          <div style={{ width: '1px', backgroundColor: '#e5e7eb', margin: '0 4px' }} />

          <ToolbarButton
            onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
            active={editor.isActive('heading', { level: 1 })}
            title="Heading 1"
          >
            H1
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
            active={editor.isActive('heading', { level: 2 })}
            title="Heading 2"
          >
            H2
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
            active={editor.isActive('heading', { level: 3 })}
            title="Heading 3"
          >
            H3
          </ToolbarButton>

          <div style={{ width: '1px', backgroundColor: '#e5e7eb', margin: '0 4px' }} />

          <ToolbarButton
            onClick={() => editor.chain().focus().toggleBulletList().run()}
            active={editor.isActive('bulletList')}
            title="Bullet List"
          >
            &#8226;
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().toggleOrderedList().run()}
            active={editor.isActive('orderedList')}
            title="Ordered List"
          >
            1.
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().toggleBlockquote().run()}
            active={editor.isActive('blockquote')}
            title="Blockquote"
          >
            &#8220;&#8221;
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().toggleCodeBlock().run()}
            active={editor.isActive('codeBlock')}
            title="Code Block"
          >
            {'</>'}
          </ToolbarButton>

          <div style={{ width: '1px', backgroundColor: '#e5e7eb', margin: '0 4px' }} />

          <ToolbarButton onClick={addImage} title="Insert Image">
            &#128247;
          </ToolbarButton>
          <ToolbarButton onClick={addLink} active={editor.isActive('link')} title="Insert Link">
            &#128279;
          </ToolbarButton>

          <div style={{ width: '1px', backgroundColor: '#e5e7eb', margin: '0 4px' }} />

          <ToolbarButton
            onClick={() => editor.chain().focus().setHorizontalRule().run()}
            title="Horizontal Rule"
          >
            &#8212;
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().undo().run()}
            title="Undo"
          >
            &#8617;
          </ToolbarButton>
          <ToolbarButton
            onClick={() => editor.chain().focus().redo().run()}
            title="Redo"
          >
            &#8618;
          </ToolbarButton>
        </div>
      )}

      {/* Editor Content */}
      <div className="article-editor__content" style={{
        minHeight: '300px',
        fontSize: '16px',
        lineHeight: 1.8,
        color: '#374151',
      }}>
        <EditorContent editor={editor} />
      </div>

      {/* Tags */}
      {showTagsField && (
        <input
          type="text"
          value={tags}
          onChange={(e) => setTags(e.target.value)}
          placeholder={tagsPlaceholder}
          className="article-editor__tags-input"
          style={{
            width: '100%',
            fontSize: '14px',
            border: '1px solid #e5e7eb',
            borderRadius: '8px',
            padding: '10px 12px',
            marginTop: '16px',
          }}
        />
      )}

      {/* Error/Success Messages */}
      {saveError && (
        <div style={{
          marginTop: '12px',
          padding: '10px 14px',
          backgroundColor: '#fef2f2',
          color: '#dc2626',
          borderRadius: '8px',
          fontSize: '14px',
        }}>
          {saveError}
        </div>
      )}
      {saveSuccess && (
        <div style={{
          marginTop: '12px',
          padding: '10px 14px',
          backgroundColor: '#f0fdf4',
          color: '#16a34a',
          borderRadius: '8px',
          fontSize: '14px',
        }}>
          {saveSuccess}
        </div>
      )}

      {/* Action Buttons */}
      <div style={{
        display: 'flex',
        gap: '12px',
        marginTop: '20px',
        justifyContent: 'flex-end',
      }}>
        {showSaveDraft && (
          <button
            onClick={() => handleSave('draft')}
            disabled={isSaving}
            className="article-editor__btn article-editor__btn--draft"
            style={{
              padding: '10px 20px',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              backgroundColor: 'white',
              fontSize: '14px',
              fontWeight: 500,
              cursor: isSaving ? 'not-allowed' : 'pointer',
              opacity: isSaving ? 0.6 : 1,
            }}
          >
            {isSaving ? 'Saving...' : saveDraftText}
          </button>
        )}
        {showPublish && (
          <button
            onClick={() => handleSave('published')}
            disabled={isSaving}
            className="article-editor__btn article-editor__btn--publish"
            style={{
              padding: '10px 20px',
              border: 'none',
              borderRadius: '8px',
              backgroundColor: '#4f46e5',
              color: 'white',
              fontSize: '14px',
              fontWeight: 500,
              cursor: isSaving ? 'not-allowed' : 'pointer',
              opacity: isSaving ? 0.6 : 1,
            }}
          >
            {isSaving ? 'Publishing...' : publishText}
          </button>
        )}
      </div>
    </div>
  );
};

// ==================== ToolbarButton Sub-component ====================

interface ToolbarButtonProps {
  onClick: () => void;
  active?: boolean;
  title?: string;
  children: React.ReactNode;
}

const ToolbarButton: React.FC<ToolbarButtonProps> = ({ onClick, active, title, children }) => (
  <button
    onClick={onClick}
    title={title}
    style={{
      padding: '4px 8px',
      border: '1px solid transparent',
      borderRadius: '4px',
      backgroundColor: active ? '#e0e7ff' : 'transparent',
      color: active ? '#4f46e5' : '#6b7280',
      fontSize: '13px',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minWidth: '28px',
      height: '28px',
      transition: 'all 0.1s ease',
    }}
    onMouseEnter={(e) => {
      if (!active) e.currentTarget.style.backgroundColor = '#f3f4f6';
    }}
    onMouseLeave={(e) => {
      if (!active) e.currentTarget.style.backgroundColor = 'transparent';
    }}
  >
    {children}
  </button>
);

export default ArticleEditorRenderer;
