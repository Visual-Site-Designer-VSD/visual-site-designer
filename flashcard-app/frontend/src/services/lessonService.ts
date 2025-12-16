import api from './api';
import { Lesson, CreateLessonRequest } from '../types';

export const lessonService = {
  // Get all lessons for a module
  getLessonsByModuleId: async (courseId: number, moduleId: number): Promise<Lesson[]> => {
    const response = await api.get<Lesson[]>(`/courses/${courseId}/modules/${moduleId}/lessons`);
    return response.data;
  },

  // Get a single lesson by ID
  getLessonById: async (courseId: number, moduleId: number, lessonId: number): Promise<Lesson> => {
    const response = await api.get<Lesson>(`/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}`);
    return response.data;
  },

  // Get a lesson with flashcards eagerly loaded
  getLessonWithFlashcards: async (courseId: number, moduleId: number, lessonId: number): Promise<Lesson> => {
    const response = await api.get<Lesson>(
      `/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}/with-flashcards`
    );
    return response.data;
  },

  // Get a lesson with media eagerly loaded
  getLessonWithMedia: async (courseId: number, moduleId: number, lessonId: number): Promise<Lesson> => {
    const response = await api.get<Lesson>(
      `/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}/with-media`
    );
    return response.data;
  },

  // Create a new lesson in a module
  createLesson: async (
    courseId: number,
    moduleId: number,
    lesson: CreateLessonRequest
  ): Promise<Lesson> => {
    const response = await api.post<Lesson>(
      `/courses/${courseId}/modules/${moduleId}/lessons`,
      lesson
    );
    return response.data;
  },

  // Update a lesson
  updateLesson: async (
    courseId: number,
    moduleId: number,
    lessonId: number,
    lesson: CreateLessonRequest
  ): Promise<Lesson> => {
    const response = await api.put<Lesson>(
      `/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}`,
      lesson
    );
    return response.data;
  },

  // Delete a lesson
  deleteLesson: async (courseId: number, moduleId: number, lessonId: number): Promise<void> => {
    await api.delete(`/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}`);
  },

  // Update lesson display order
  updateLessonOrder: async (
    courseId: number,
    moduleId: number,
    lessonId: number,
    displayOrder: number
  ): Promise<Lesson> => {
    const response = await api.patch<Lesson>(
      `/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}/order`,
      displayOrder,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );
    return response.data;
  },
};
