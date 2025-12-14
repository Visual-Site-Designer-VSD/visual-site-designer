import React, { useState } from 'react';
import { CreateLessonRequest } from '../types';
import RichTextEditor from './RichTextEditor';
import './LessonForm.css';

interface LessonFormProps {
  onSubmit: (lesson: CreateLessonRequest) => void;
  onCancel: () => void;
  initialData?: CreateLessonRequest;
  isLoading?: boolean;
}

const LessonForm: React.FC<LessonFormProps> = ({
  onSubmit,
  onCancel,
  initialData,
  isLoading = false,
}) => {
  const [formData, setFormData] = useState<CreateLessonRequest>(
    initialData || {
      title: '',
      description: '',
      content: '',
      contentFormat: 'html',
      displayOrder: 0,
    }
  );

  const [errors, setErrors] = useState<{
    title?: string;
    description?: string;
    content?: string;
    displayOrder?: string;
  }>({});

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'displayOrder' ? parseInt(value) || 0 : value,
    }));
    // Clear error when user starts typing
    if (errors[name as keyof typeof errors]) {
      setErrors((prev) => ({
        ...prev,
        [name]: undefined,
      }));
    }
  };

  const handleContentChange = (value: string) => {
    setFormData((prev) => ({
      ...prev,
      content: value,
    }));
    // Clear error when user starts typing
    if (errors.content) {
      setErrors((prev) => ({
        ...prev,
        content: undefined,
      }));
    }
  };

  const validate = (): boolean => {
    const newErrors: {
      title?: string;
      description?: string;
      content?: string;
      displayOrder?: string;
    } = {};

    if (!formData.title.trim()) {
      newErrors.title = 'Title is required';
    } else if (formData.title.trim().length < 3) {
      newErrors.title = 'Title must be at least 3 characters';
    }

    if (!formData.description.trim()) {
      newErrors.description = 'Description is required';
    } else if (formData.description.trim().length < 10) {
      newErrors.description = 'Description must be at least 10 characters';
    }

    if (!formData.content.trim()) {
      newErrors.content = 'Content is required';
    } else if (formData.content.trim().length < 10) {
      newErrors.content = 'Content must be at least 10 characters';
    }

    if (formData.displayOrder !== undefined && formData.displayOrder < 0) {
      newErrors.displayOrder = 'Display order must be 0 or greater';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (validate()) {
      onSubmit(formData);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="lesson-form">
      <div className="form-group">
        <label htmlFor="title">Lesson Title *</label>
        <input
          type="text"
          id="title"
          name="title"
          className={`form-control ${errors.title ? 'error' : ''}`}
          value={formData.title}
          onChange={handleChange}
          placeholder="e.g., Understanding Props in React"
          disabled={isLoading}
        />
        {errors.title && <div className="error-message">{errors.title}</div>}
      </div>

      <div className="form-group">
        <label htmlFor="description">Description *</label>
        <textarea
          id="description"
          name="description"
          className={`form-control ${errors.description ? 'error' : ''}`}
          value={formData.description}
          onChange={handleChange}
          placeholder="Brief description of what students will learn..."
          rows={3}
          disabled={isLoading}
        />
        {errors.description && (
          <div className="error-message">{errors.description}</div>
        )}
      </div>

      <RichTextEditor
        label="Lesson Content *"
        value={formData.content}
        onChange={handleContentChange}
        placeholder="Enter the lesson content with rich text formatting..."
        error={errors.content}
        disabled={isLoading}
        height="400px"
      />

      <div className="form-group">
        <label htmlFor="displayOrder">Display Order</label>
        <input
          type="number"
          id="displayOrder"
          name="displayOrder"
          className={`form-control ${errors.displayOrder ? 'error' : ''}`}
          value={formData.displayOrder || 0}
          onChange={handleChange}
          min="0"
          disabled={isLoading}
        />
        {errors.displayOrder && (
          <div className="error-message">{errors.displayOrder}</div>
        )}
        <small className="form-text">Order in which this lesson appears (0 = first)</small>
      </div>

      <div className="form-actions">
        <button
          type="button"
          className="btn btn-secondary"
          onClick={onCancel}
          disabled={isLoading}
        >
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={isLoading}>
          {isLoading ? 'Saving...' : initialData ? 'Update Lesson' : 'Create Lesson'}
        </button>
      </div>
    </form>
  );
};

export default LessonForm;
