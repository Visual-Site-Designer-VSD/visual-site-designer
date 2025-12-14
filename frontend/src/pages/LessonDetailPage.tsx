import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { lessonService } from '../services/lessonService';
import { mediaService } from '../services/mediaService';
import { Lesson, Media } from '../types';
import LessonViewer from '../components/LessonViewer';
import MediaUpload from '../components/MediaUpload';
import './LessonDetailPage.css';

const LessonDetailPage: React.FC = () => {
  const { courseId, moduleId, lessonId } = useParams<{ courseId: string; moduleId: string; lessonId: string }>();

  const [lesson, setLesson] = useState<Lesson | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showUpload, setShowUpload] = useState(false);

  useEffect(() => {
    if (courseId && moduleId && lessonId) {
      loadLessonWithMedia();
    }
  }, [courseId, moduleId, lessonId]);

  const loadLessonWithMedia = async () => {
    try {
      setLoading(true);
      setError(null);
      const lessonData = await lessonService.getLessonWithMedia(
        parseInt(courseId!),
        parseInt(moduleId!),
        parseInt(lessonId!)
      );
      setLesson(lessonData);
    } catch (err) {
      setError('Failed to load lesson. Please try again.');
      console.error('Error loading lesson:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleMediaUpload = (uploadedMedia: Media) => {
    setLesson((prev) => prev ? { ...prev, media: uploadedMedia } : null);
    setShowUpload(false);
    setError(null);
    setSuccess(`Successfully uploaded ${uploadedMedia.originalFileName}`);

    // Clear success message after 5 seconds
    setTimeout(() => {
      setSuccess(null);
    }, 5000);
  };

  const handleMediaDelete = async (mediaId: number) => {
    try {
      await mediaService.deleteMedia(mediaId);
      setLesson((prev) => prev ? { ...prev, media: undefined } : null);
    } catch (err) {
      setError('Failed to delete media. Please try again.');
      console.error('Error deleting media:', err);
    }
  };

  const handleUploadError = (err: string) => {
    setError(err);
  };

  if (loading) {
    return (
      <div className="lesson-detail-page">
        <div className="loading">
          <div className="spinner"></div>
        </div>
      </div>
    );
  }

  if (!lesson) {
    return (
      <div className="lesson-detail-page">
        <div className="error">
          Lesson not found
        </div>
        <Link to={`/courses/${courseId}/modules/${moduleId}`} className="btn btn-primary">
          Back to Module
        </Link>
      </div>
    );
  }

  return (
    <div className="lesson-detail-page">
      {error && (
        <div className="error">
          {error}
          <button
            className="btn-close"
            onClick={() => setError(null)}
            aria-label="Close"
          >
            ×
          </button>
        </div>
      )}

      {success && (
        <div className="success">
          {success}
          <button
            className="btn-close"
            onClick={() => setSuccess(null)}
            aria-label="Close"
          >
            ×
          </button>
        </div>
      )}

      <div className="breadcrumb">
        <Link to="/courses">Courses</Link>
        <span className="breadcrumb-separator">/</span>
        <Link to={`/courses/${courseId}`}>Course</Link>
        <span className="breadcrumb-separator">/</span>
        <Link to={`/courses/${courseId}/modules/${moduleId}`}>Module</Link>
        <span className="breadcrumb-separator">/</span>
        <span className="breadcrumb-current">Lesson</span>
      </div>

      <div className="lesson-actions-header">
        <Link to={`/courses/${courseId}/modules/${moduleId}`} className="btn btn-secondary">
          ← Back to Module
        </Link>
      </div>

      <LessonViewer
        lesson={lesson}
        showMedia={true}
        onMediaDelete={handleMediaDelete}
        readOnly={false}
      />

      <div className="media-upload-section">
        <div className="section-header">
          <h3>Manage Media</h3>
          {lesson.media ? (
            <p className="media-status">
              Current media: {lesson.media.originalFileName}
            </p>
          ) : (
            <p className="media-status">No media attached</p>
          )}
          <button
            className="btn btn-primary"
            onClick={() => setShowUpload(!showUpload)}
          >
            {showUpload ? 'Cancel Upload' : lesson.media ? 'Replace Media' : '+ Upload Media'}
          </button>
        </div>

        {showUpload && (
          <MediaUpload
            lessonId={parseInt(lessonId!)}
            onUploadComplete={handleMediaUpload}
            onError={handleUploadError}
          />
        )}
      </div>
    </div>
  );
};

export default LessonDetailPage;
