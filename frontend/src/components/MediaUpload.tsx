import React, { useState, useRef } from 'react';
import { Media } from '../types';
import { mediaService } from '../services/mediaService';
import './MediaUpload.css';

interface MediaUploadProps {
  lessonId: number;
  onUploadComplete: (media: Media) => void;
  onError: (error: string) => void;
  acceptedTypes?: string[];
  maxSizeMB?: number;
}

const MediaUpload: React.FC<MediaUploadProps> = ({
  lessonId,
  onUploadComplete,
  onError,
  acceptedTypes = ['image/*', 'video/*', 'application/pdf', 'audio/*'],
  maxSizeMB = 1000,
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): boolean => {
    const maxSize = maxSizeMB * 1024 * 1024;

    if (file.size > maxSize) {
      onError(`File size exceeds maximum limit of ${maxSizeMB}MB`);
      return false;
    }

    const isValidType = acceptedTypes.some(type => {
      if (type.endsWith('/*')) {
        const baseType = type.split('/')[0];
        return file.type.startsWith(baseType + '/');
      }
      return file.type === type;
    });

    if (!isValidType) {
      onError(`File type ${file.type} is not supported`);
      return false;
    }

    return true;
  };

  const generatePreview = (file: File) => {
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    } else {
      setPreview(null);
    }
  };

  const handleFileSelect = (file: File | null) => {
    if (!file) return;

    if (validateFile(file)) {
      setSelectedFile(file);
      generatePreview(file);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    handleFileSelect(file);
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const file = e.dataTransfer.files?.[0];
    handleFileSelect(file);
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setUploading(true);
      setUploadProgress(10);

      // Simulate progress (real progress would require backend streaming support)
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => {
          if (prev >= 85) {
            clearInterval(progressInterval);
            return 85;
          }
          return prev + 5;
        });
      }, 300);

      const uploadedMedia = await mediaService.uploadFile(lessonId, selectedFile);

      clearInterval(progressInterval);
      setUploadProgress(100);

      // Show success state briefly before clearing
      setTimeout(() => {
        onUploadComplete(uploadedMedia);

        // Reset after a delay to show success
        setTimeout(() => {
          setSelectedFile(null);
          setPreview(null);
          setUploadProgress(0);
          if (fileInputRef.current) {
            fileInputRef.current.value = '';
          }
        }, 1000);
      }, 500);
    } catch (err) {
      setUploadProgress(0);
      onError('Failed to upload file. Please try again.');
      console.error('Upload error:', err);
    } finally {
      setTimeout(() => {
        setUploading(false);
      }, 1500);
    }
  };

  const handleCancel = () => {
    setSelectedFile(null);
    setPreview(null);
    setUploadProgress(0);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="media-upload">
      <div
        className={`upload-zone ${dragActive ? 'drag-active' : ''}`}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleInputChange}
          accept={acceptedTypes.join(',')}
          className="file-input"
          disabled={uploading}
        />

        {!selectedFile ? (
          <div className="upload-prompt">
            <div className="upload-icon">📁</div>
            <p className="upload-text">
              Drag and drop a file here, or click to select
            </p>
            <p className="upload-hint">
              Supported: Images, Videos, PDFs, Audio (Max {maxSizeMB}MB)
            </p>
          </div>
        ) : (
          <div className="file-selected">
            {preview && (
              <div className="file-preview">
                <img src={preview} alt="Preview" />
              </div>
            )}
            <div className="file-info">
              <div className="file-name">{selectedFile.name}</div>
              <div className="file-size">
                {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB
              </div>
            </div>
          </div>
        )}
      </div>

      {uploadProgress > 0 && (
        <div className="upload-progress">
          <div className="progress-bar">
            <div
              className="progress-fill"
              style={{ width: `${uploadProgress}%` }}
            />
          </div>
          <div className="progress-text">
            {uploadProgress === 100 ? '✓ Upload Complete!' : `Uploading... ${uploadProgress}%`}
          </div>
        </div>
      )}

      {selectedFile && !uploading && uploadProgress === 0 && (
        <div className="upload-actions">
          <button
            className="btn btn-secondary"
            onClick={handleCancel}
          >
            Cancel
          </button>
          <button
            className="btn btn-primary"
            onClick={handleUpload}
          >
            Upload File
          </button>
        </div>
      )}
    </div>
  );
};

export default MediaUpload;
