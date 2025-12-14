import React from 'react';
import { Link } from 'react-router-dom';
import MediaPlayer from './MediaPlayer';
import { Lesson } from '../types';
import './LessonCard.css';

interface LessonCardProps {
  lesson: Lesson;
  isExpanded: boolean;
  onToggleExpand: (lessonId: number) => void;
  courseId: number;
  moduleId: number;
  onMoveUp?: () => void;
  onMoveDown?: () => void;
  canMoveUp?: boolean;
  canMoveDown?: boolean;
}

const LessonCard: React.FC<LessonCardProps> = ({
  lesson,
  isExpanded,
  onToggleExpand,
  courseId,
  moduleId,
  onMoveUp,
  onMoveDown,
  canMoveUp = false,
  canMoveDown = false,
}) => {
  const handleThumbnailClick = () => {
    if (lesson.id) {
      onToggleExpand(lesson.id);
    }
  };

  return (
    <div className={`lesson-card ${isExpanded ? 'expanded' : ''}`}>
      <div className="lesson-card-header">
        {/* Left-side thumbnail */}
        <div
          className="lesson-thumbnail"
          onClick={handleThumbnailClick}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === 'Enter' && handleThumbnailClick()}
        >
          {lesson.media ? (
            <MediaPlayer media={lesson.media} thumbnail={true} />
          ) : (
            <div className="no-media-placeholder">
              <span className="no-media-icon">📄</span>
              <span className="no-media-text">No media</span>
            </div>
          )}
          {lesson.media && (
            <div className="play-overlay">
              {isExpanded ? '▲' : '▶'}
            </div>
          )}
        </div>

        {/* Center - Lesson info */}
        <div className="lesson-info">
          <h3 className="lesson-title">{lesson.title}</h3>
          <p className="lesson-description">{lesson.description}</p>
        </div>

        {/* Right - Actions */}
        <div className="lesson-actions">
          {(onMoveUp || onMoveDown) && (
            <div className="reorder-buttons">
              <button
                className="btn-reorder"
                onClick={onMoveUp}
                disabled={!canMoveUp}
                title="Move up"
              >
                ↑
              </button>
              <button
                className="btn-reorder"
                onClick={onMoveDown}
                disabled={!canMoveDown}
                title="Move down"
              >
                ↓
              </button>
            </div>
          )}
          <Link
            to={`/courses/${courseId}/modules/${moduleId}/lessons/${lesson.id}`}
            className="view-lesson-btn"
          >
            View Full Lesson →
          </Link>
        </div>
      </div>

      {/* Expanded media section */}
      <div className={`lesson-card-content ${isExpanded ? 'show' : ''}`}>
        {isExpanded && lesson.media && (
          <div className="expanded-media">
            <MediaPlayer media={lesson.media} />
          </div>
        )}
      </div>
    </div>
  );
};

export default LessonCard;
