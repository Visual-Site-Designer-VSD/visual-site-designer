import React, { useState } from 'react';
import { CreateChapterRequest } from '../types';
import './ChapterForm.css';

interface ChapterFormProps {
  onSubmit: (chapter: CreateChapterRequest) => void;
  onCancel: () => void;
  initialData?: CreateChapterRequest;
  isLoading?: boolean;
}

const ChapterForm: React.FC<ChapterFormProps> = ({
  onSubmit,
  onCancel,
  initialData,
  isLoading = false,
}) => {
  const [formData, setFormData] = useState<CreateChapterRequest>(
    initialData || {
      title: '',
      description: '',
      displayOrder: 0,
    }
  );

  const [errors, setErrors] = useState<{
    title?: string;
    description?: string;
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

  const validate = (): boolean => {
    const newErrors: {
      title?: string;
      description?: string;
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
    <form onSubmit={handleSubmit} className="chapter-form">
      <div className="form-group">
        <label htmlFor="title">Chapter Title *</label>
        <input
          type="text"
          id="title"
          name="title"
          className={`form-control ${errors.title ? 'error' : ''}`}
          value={formData.title}
          onChange={handleChange}
          placeholder="e.g., Introduction to React"
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
          placeholder="Describe what this chapter covers..."
          rows={4}
          disabled={isLoading}
        />
        {errors.description && (
          <div className="error-message">{errors.description}</div>
        )}
      </div>

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
        <small className="form-text">Order in which this chapter appears (0 = first)</small>
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
          {isLoading ? 'Saving...' : initialData ? 'Update Chapter' : 'Create Chapter'}
        </button>
      </div>
    </form>
  );
};

export default ChapterForm;
