import React, { useState, useEffect, useCallback, useRef } from 'react';
import { contentService, ContentItem } from '../../services/contentService';
import './ImageRepositoryModal.css';

interface ImageRepositoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectImage?: (imageUrl: string) => void;
  selectionMode?: boolean;
}

/**
 * ImageRepositoryModal - Modal for managing and selecting images from the content repository
 * Allows users to view, upload, and select images for use in components
 */
export const ImageRepositoryModal: React.FC<ImageRepositoryModalProps> = ({
  isOpen,
  onClose,
  onSelectImage,
  selectionMode = false,
}) => {
  const [images, setImages] = useState<ContentItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedImage, setSelectedImage] = useState<ContentItem | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [copiedUrl, setCopiedUrl] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load images on mount
  useEffect(() => {
    if (isOpen) {
      loadImages();
    }
  }, [isOpen]);

  const loadImages = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await contentService.getImages();
      setImages(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load images');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFileSelect = useCallback(async (files: FileList | null) => {
    if (!files || files.length === 0) return;

    setError(null);
    setUploadProgress(0);

    const uploadedItems: ContentItem[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];

      // Validate file type
      if (!file.type.startsWith('image/')) {
        setError(`${file.name} is not an image file`);
        continue;
      }

      // Validate file size (50MB max)
      if (file.size > 50 * 1024 * 1024) {
        setError(`${file.name} exceeds maximum file size (50MB)`);
        continue;
      }

      try {
        const item = await contentService.uploadFile(file);
        uploadedItems.push(item);
        setUploadProgress(((i + 1) / files.length) * 100);
      } catch (err) {
        setError(`Failed to upload ${file.name}: ${err instanceof Error ? err.message : 'Unknown error'}`);
      }
    }

    if (uploadedItems.length > 0) {
      setImages(prev => [...uploadedItems, ...prev]);
    }

    setUploadProgress(null);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    handleFileSelect(e.dataTransfer.files);
  }, [handleFileSelect]);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  }, []);

  const handleDelete = async (item: ContentItem) => {
    if (!confirm(`Are you sure you want to delete "${item.originalName}"?`)) {
      return;
    }

    try {
      await contentService.deleteContent(item.id);
      setImages(prev => prev.filter(img => img.id !== item.id));
      if (selectedImage?.id === item.id) {
        setSelectedImage(null);
      }
    } catch (err) {
      setError(`Failed to delete: ${err instanceof Error ? err.message : 'Unknown error'}`);
    }
  };

  const handleCopyUrl = (url: string) => {
    navigator.clipboard.writeText(url);
    setCopiedUrl(url);
    setTimeout(() => setCopiedUrl(null), 2000);
  };

  const handleSelectImage = (item: ContentItem) => {
    if (selectionMode && onSelectImage) {
      onSelectImage(item.url);
      onClose();
    } else {
      setSelectedImage(selectedImage?.id === item.id ? null : item);
    }
  };

  const handleConfirmSelection = () => {
    if (selectedImage && onSelectImage) {
      onSelectImage(selectedImage.url);
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="image-repository-overlay" onClick={onClose}>
      <div className="image-repository-modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{selectionMode ? 'Select Image' : 'Image Repository'}</h2>
          <button className="close-button" onClick={onClose} title="Close">
            ✕
          </button>
        </div>

        <div className="modal-body">
          {/* Upload Zone */}
          <div
            className={`upload-zone ${dragOver ? 'drag-over' : ''}`}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              onChange={e => handleFileSelect(e.target.files)}
              style={{ display: 'none' }}
            />
            <div className="upload-content">
              <span className="upload-icon">📷</span>
              <p>Drag and drop images here, or click to browse</p>
              <span className="upload-hint">Supports JPG, PNG, GIF, WebP, SVG (max 50MB)</span>
            </div>
          </div>

          {/* Progress Bar */}
          {uploadProgress !== null && (
            <div className="upload-progress">
              <div className="progress-bar">
                <div className="progress-fill" style={{ width: `${uploadProgress}%` }} />
              </div>
              <span className="progress-text">Uploading... {Math.round(uploadProgress)}%</span>
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="error-message">
              <span className="error-icon">⚠️</span>
              {error}
              <button className="dismiss-button" onClick={() => setError(null)}>✕</button>
            </div>
          )}

          {/* Image Grid */}
          <div className="image-grid-container">
            {isLoading ? (
              <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading images...</p>
              </div>
            ) : images.length === 0 ? (
              <div className="empty-state">
                <span className="empty-icon">🖼️</span>
                <p>No images in repository</p>
                <span className="empty-hint">Upload images to get started</span>
              </div>
            ) : (
              <div className="image-grid">
                {images.map(item => (
                  <div
                    key={item.id}
                    className={`image-card ${selectedImage?.id === item.id ? 'selected' : ''}`}
                    onClick={() => handleSelectImage(item)}
                  >
                    <div className="image-preview">
                      <img src={item.url} alt={item.originalName} loading="lazy" />
                    </div>
                    <div className="image-info">
                      <span className="image-name" title={item.originalName}>
                        {item.originalName}
                      </span>
                      <span className="image-meta">
                        {contentService.formatFileSize(item.size)}
                        {item.width && item.height && ` • ${item.width}×${item.height}`}
                      </span>
                    </div>
                    <div className="image-actions">
                      <button
                        className="action-button copy-button"
                        onClick={e => {
                          e.stopPropagation();
                          handleCopyUrl(item.url);
                        }}
                        title={copiedUrl === item.url ? 'Copied!' : 'Copy URL'}
                      >
                        {copiedUrl === item.url ? '✓' : '📋'}
                      </button>
                      <button
                        className="action-button delete-button"
                        onClick={e => {
                          e.stopPropagation();
                          handleDelete(item);
                        }}
                        title="Delete"
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Selected Image Details */}
          {selectedImage && !selectionMode && (
            <div className="selected-details">
              <h4>Selected Image</h4>
              <div className="details-content">
                <div className="details-preview">
                  <img src={selectedImage.url} alt={selectedImage.originalName} />
                </div>
                <div className="details-info">
                  <p><strong>Name:</strong> {selectedImage.originalName}</p>
                  <p><strong>Size:</strong> {contentService.formatFileSize(selectedImage.size)}</p>
                  {selectedImage.width && selectedImage.height && (
                    <p><strong>Dimensions:</strong> {selectedImage.width} × {selectedImage.height}px</p>
                  )}
                  <p><strong>Type:</strong> {selectedImage.mimeType}</p>
                  <div className="url-field">
                    <label>URL:</label>
                    <input
                      type="text"
                      value={selectedImage.url}
                      readOnly
                      onClick={e => (e.target as HTMLInputElement).select()}
                    />
                    <button
                      className="copy-url-button"
                      onClick={() => handleCopyUrl(selectedImage.url)}
                    >
                      {copiedUrl === selectedImage.url ? '✓ Copied' : 'Copy'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="cancel-button" onClick={onClose}>
            Cancel
          </button>
          {selectionMode && selectedImage && (
            <button className="confirm-button" onClick={handleConfirmSelection}>
              Use Selected Image
            </button>
          )}
          <button className="refresh-button" onClick={loadImages} disabled={isLoading}>
            ↻ Refresh
          </button>
        </div>
      </div>
    </div>
  );
};

export default ImageRepositoryModal;
