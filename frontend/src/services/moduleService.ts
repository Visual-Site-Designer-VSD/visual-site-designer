import api from './api';
import { Module, CreateModuleRequest } from '../types';

export const moduleService = {
  // Get all modules in a course
  getModulesByCourseId: async (courseId: number): Promise<Module[]> => {
    const response = await api.get<Module[]>(`/courses/${courseId}/modules`);
    return response.data;
  },

  // Get a single module by ID
  getModuleById: async (courseId: number, moduleId: number): Promise<Module> => {
    const response = await api.get<Module>(`/courses/${courseId}/modules/${moduleId}`);
    return response.data;
  },

  // Get a module with lessons eagerly loaded
  getModuleWithLessons: async (courseId: number, moduleId: number): Promise<Module> => {
    const response = await api.get<Module>(`/courses/${courseId}/modules/${moduleId}/with-lessons`);
    return response.data;
  },

  // Get a module with lessons and flashcards
  getModuleWithLessonsAndFlashcards: async (courseId: number, moduleId: number): Promise<Module> => {
    const response = await api.get<Module>(`/courses/${courseId}/modules/${moduleId}/with-lessons-and-flashcards`);
    return response.data;
  },

  // Create a new module
  createModule: async (courseId: number, module: CreateModuleRequest): Promise<Module> => {
    const response = await api.post<Module>(`/courses/${courseId}/modules`, module);
    return response.data;
  },

  // Update a module
  updateModule: async (courseId: number, moduleId: number, module: CreateModuleRequest): Promise<Module> => {
    const response = await api.put<Module>(`/courses/${courseId}/modules/${moduleId}`, module);
    return response.data;
  },

  // Delete a module
  deleteModule: async (courseId: number, moduleId: number): Promise<void> => {
    await api.delete(`/courses/${courseId}/modules/${moduleId}`);
  },

  // Search modules by title
  searchModules: async (keyword: string): Promise<Module[]> => {
    const response = await api.get<Module[]>('/courses/modules/search', {
      params: { keyword }
    });
    return response.data;
  },

  // Get lesson count for a module
  getLessonCount: async (courseId: number, moduleId: number): Promise<number> => {
    const response = await api.get<number>(`/courses/${courseId}/modules/${moduleId}/lesson-count`);
    return response.data;
  },
};
