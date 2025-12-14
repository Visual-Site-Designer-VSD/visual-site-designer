import React from 'react';
import { Lesson } from '../types';
import TextToSpeech from './TextToSpeech';
import MediaPlayer from './MediaPlayer';
import './LessonViewer.css';

interface LessonViewerProps {
  lesson: Lesson;
  showMedia?: boolean;
  onMediaDelete?: (mediaId: number) => void;
  readOnly?: boolean;
}

/**
 * Helper function to strip HTML tags for text-to-speech
 */
const stripHtml = (html: string): string => {
  const tmp = document.createElement('DIV');
  tmp.innerHTML = html;
  return tmp.textContent || tmp.innerText || '';
};

const LessonViewer: React.FC<LessonViewerProps> = ({
  lesson,
  showMedia = true,
  onMediaDelete,
  readOnly = false,
}) => {
  // Determine if content is HTML or fallback to plain text
  const isHtmlContent = lesson.contentFormat === 'html' ||
                        (lesson.content && lesson.content.trim().startsWith('<'));

  return (
    <div className="lesson-viewer">
      <div className="lesson-header">
        <h1>{lesson.title}</h1>
        <p className="lesson-description">{lesson.description}</p>
      </div>

      <div className="lesson-content-container">
        <div className="lesson-text-section">
          <TextToSpeech text={isHtmlContent ? stripHtml(lesson.content) : lesson.content} />

          <div className="html-content lesson-content">
            {isHtmlContent ? (
              <div dangerouslySetInnerHTML={{ __html: lesson.content }} />
            ) : (
              <div className="plain-text-content">{lesson.content}</div>
            )}
          </div>
        </div>

        {showMedia && lesson.media && (
          <div className="lesson-media-section">
            <h3>Media Resource</h3>
            <MediaPlayer media={lesson.media} />
            {!readOnly && onMediaDelete && (
              <button
                className="btn btn-danger btn-sm delete-media-btn"
                onClick={() => onMediaDelete(lesson.media!.id!)}
              >
                Delete Media
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default LessonViewer;
