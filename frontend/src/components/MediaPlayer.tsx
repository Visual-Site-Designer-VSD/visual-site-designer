import React, { useState } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import { Media } from '../types';
import { mediaService } from '../services/mediaService';
import './MediaPlayer.css';

// Configure PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.min.js`;

interface MediaPlayerProps {
  media: Media;
  className?: string;
  onError?: (error: string) => void;
  thumbnail?: boolean;
}

const MediaPlayer: React.FC<MediaPlayerProps> = ({
  media,
  className = '',
  onError,
  thumbnail = false,
}) => {
  const [numPages, setNumPages] = useState<number | null>(null);
  const [pageNumber, setPageNumber] = useState(1);
  const [error, setError] = useState<string | null>(null);

  const mediaUrl = mediaService.getMediaUrl(media.fileName);

  const handleError = (err: string) => {
    setError(err);
    onError?.(err);
  };

  const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
    setNumPages(numPages);
    setPageNumber(1);
  };

  const renderMedia = () => {
    const mediaType = media.mediaType.toLowerCase();

    try {
      switch (mediaType) {
        case 'image':
          return (
            <div className={`media-image-container ${thumbnail ? 'media-image-thumbnail' : ''}`}>
              <img
                src={mediaUrl}
                alt={media.originalFileName || 'Media'}
                className={thumbnail ? 'media-image-thumb' : 'media-image'}
                onError={() => handleError('Failed to load image')}
              />
            </div>
          );

        case 'video':
          if (thumbnail) {
            return (
              <div className="media-video-container media-video-thumbnail">
                <div className="video-thumbnail-overlay">
                  <div className="play-icon">▶</div>
                  <span className="video-label">Click to play</span>
                </div>
                <video
                  src={mediaUrl}
                  preload="metadata"
                  className="media-video-preview"
                  muted
                  playsInline
                >
                  Your browser does not support the video tag.
                </video>
              </div>
            );
          }
          return (
            <div className="media-video-container">
              <video
                src={mediaUrl}
                controls
                controlsList="nodownload"
                preload="metadata"
                className="media-video"
                onError={() => handleError('Failed to load video')}
              >
                Your browser does not support the video tag.
              </video>
            </div>
          );

        case 'audio':
          if (thumbnail) {
            return (
              <div className="media-audio-container media-audio-thumbnail">
                <div className="audio-thumbnail-icon">🎵</div>
                <span className="audio-label">Click to play</span>
              </div>
            );
          }
          return (
            <div className="media-audio-container">
              <audio
                src={mediaUrl}
                controls
                controlsList="nodownload"
                preload="metadata"
                className="media-audio"
                onError={() => handleError('Failed to load audio')}
              >
                Your browser does not support the audio tag.
              </audio>
            </div>
          );

        case 'pdf':
          if (thumbnail) {
            return (
              <div className="media-pdf-container media-pdf-thumbnail">
                <div className="pdf-thumbnail-icon">📄</div>
                <span className="pdf-label">PDF Document</span>
              </div>
            );
          }
          return (
            <div className="media-pdf-container">
              <Document
                file={mediaUrl}
                onLoadSuccess={onDocumentLoadSuccess}
                onLoadError={() => handleError('Failed to load PDF')}
              >
                <Page pageNumber={pageNumber} />
              </Document>
              {numPages && (
                <div className="pdf-controls">
                  <button
                    className="btn btn-sm btn-secondary"
                    onClick={() => setPageNumber(Math.max(1, pageNumber - 1))}
                    disabled={pageNumber <= 1}
                  >
                    Previous
                  </button>
                  <span className="pdf-page-info">
                    Page {pageNumber} of {numPages}
                  </span>
                  <button
                    className="btn btn-sm btn-secondary"
                    onClick={() => setPageNumber(Math.min(numPages, pageNumber + 1))}
                    disabled={pageNumber >= numPages}
                  >
                    Next
                  </button>
                </div>
              )}
            </div>
          );

        default:
          return (
            <div className="media-unsupported">
              <p>Unsupported media type: {media.mediaType}</p>
              <a
                href={mediaUrl}
                download={media.originalFileName}
                className="btn btn-primary btn-sm"
              >
                Download File
              </a>
            </div>
          );
      }
    } catch (err) {
      return (
        <div className="media-error">
          <p>Error loading media</p>
        </div>
      );
    }
  };

  return (
    <div className={`media-player ${className} ${thumbnail ? 'media-player-thumbnail' : ''}`}>
      {error ? (
        <div className="media-error">
          <p>{error}</p>
        </div>
      ) : (
        renderMedia()
      )}
      {!thumbnail && (
        <div className="media-info">
          <span className="media-filename" title={media.originalFileName}>
            {media.originalFileName}
          </span>
          {media.fileSize && (
            <span className="media-filesize">
              {(media.fileSize / (1024 * 1024)).toFixed(2)} MB
            </span>
          )}
        </div>
      )}
    </div>
  );
};

export default MediaPlayer;
