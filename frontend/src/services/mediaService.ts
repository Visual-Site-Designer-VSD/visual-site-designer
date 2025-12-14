import api from './api';
import { Media } from '../types';

export const mediaService = {
  // Upload a file for a lesson
  uploadFile: async (lessonId: number, file: File): Promise<Media> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('lessonId', lessonId.toString());

    const response = await api.post<Media>('/media/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Get media metadata by ID
  getMediaById: async (id: number): Promise<Media> => {
    const response = await api.get<Media>(`/media/${id}`);
    return response.data;
  },

  // Get media for a lesson (single media per lesson)
  getMediaByLessonId: async (lessonId: number): Promise<Media | null> => {
    try {
      const response = await api.get<Media>(`/media/lesson/${lessonId}`);
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  // Delete media
  deleteMedia: async (id: number): Promise<void> => {
    await api.delete(`/media/${id}`);
  },

  // Get media file URL for display
  getMediaUrl: (fileName: string): string => {
    return `/api/media/files/${fileName}`;
  },
};
