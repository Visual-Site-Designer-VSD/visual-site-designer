/**
 * Preview Broadcast Service
 *
 * Handles cross-window communication between the builder and preview windows.
 * Uses BroadcastChannel API for real-time updates with localStorage fallback.
 */

import type { PageDefinition } from '../types/builder';
import type { Page } from '../types/site';

// Message types for preview communication
export type PreviewMessageType =
  | 'PAGE_UPDATE'      // Page content updated
  | 'PAGE_NAVIGATE'    // Navigate to a different page
  | 'PAGES_LIST'       // List of all pages updated
  | 'PREVIEW_READY'    // Preview window is ready
  | 'BUILDER_PING'     // Builder pinging preview
  | 'PREVIEW_PONG'     // Preview responding to ping
  | 'CLOSE_PREVIEW';   // Close the preview window

export interface PreviewMessage {
  type: PreviewMessageType;
  timestamp: number;
  payload?: unknown;
}

export interface PageUpdatePayload {
  page: PageDefinition;
  pageMeta: Page;
}

export interface NavigatePayload {
  path: string;
}

export interface PagesListPayload {
  pages: Page[];
  currentPageId?: number;
  siteId?: number;
}

const CHANNEL_NAME = 'vsd-preview-channel';
const STORAGE_KEY = 'vsd-preview-message';

class PreviewBroadcastService {
  private channel: BroadcastChannel | null = null;
  private listeners: Map<PreviewMessageType, Set<(payload: unknown) => void>> = new Map();
  private isPreviewWindow = false;
  private useStorageFallback = false;

  constructor() {
    // Try to use BroadcastChannel, fall back to localStorage
    try {
      this.channel = new BroadcastChannel(CHANNEL_NAME);
      this.channel.onmessage = (event) => this.handleMessage(event.data);
    } catch {
      console.log('[PreviewBroadcast] BroadcastChannel not available, using localStorage fallback');
      this.useStorageFallback = true;
      window.addEventListener('storage', this.handleStorageEvent);
    }
  }

  /**
   * Mark this window as the preview window
   */
  setAsPreviewWindow() {
    this.isPreviewWindow = true;
    // Notify builder that preview is ready
    this.send('PREVIEW_READY', { windowId: Date.now() });
  }

  /**
   * Send a message to other windows
   */
  send(type: PreviewMessageType, payload?: unknown) {
    const message: PreviewMessage = {
      type,
      timestamp: Date.now(),
      payload,
    };

    if (this.channel) {
      this.channel.postMessage(message);
    }

    if (this.useStorageFallback) {
      // Use localStorage for fallback
      localStorage.setItem(STORAGE_KEY, JSON.stringify(message));
      // Clear after a short delay to allow multiple messages
      setTimeout(() => localStorage.removeItem(STORAGE_KEY), 100);
    }
  }

  /**
   * Send page update to preview window
   */
  sendPageUpdate(page: PageDefinition, pageMeta: Page) {
    const payload: PageUpdatePayload = { page, pageMeta };
    this.send('PAGE_UPDATE', payload);
  }

  /**
   * Send navigation request to preview
   */
  sendNavigate(path: string) {
    const payload: NavigatePayload = { path };
    this.send('PAGE_NAVIGATE', payload);
  }

  /**
   * Send pages list to preview
   */
  sendPagesList(pages: Page[], currentPageId?: number, siteId?: number) {
    const payload: PagesListPayload = { pages, currentPageId, siteId };
    this.send('PAGES_LIST', payload);
  }

  /**
   * Ping the preview window to check if it's alive
   */
  pingPreview(): Promise<boolean> {
    return new Promise((resolve) => {
      const timeout = setTimeout(() => {
        this.off('PREVIEW_PONG', handlePong);
        resolve(false);
      }, 1000);

      const handlePong = () => {
        clearTimeout(timeout);
        this.off('PREVIEW_PONG', handlePong);
        resolve(true);
      };

      this.on('PREVIEW_PONG', handlePong);
      this.send('BUILDER_PING', {});
    });
  }

  /**
   * Subscribe to a message type
   */
  on(type: PreviewMessageType, callback: (payload: unknown) => void) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set());
    }
    this.listeners.get(type)!.add(callback);
  }

  /**
   * Unsubscribe from a message type
   */
  off(type: PreviewMessageType, callback: (payload: unknown) => void) {
    const typeListeners = this.listeners.get(type);
    if (typeListeners) {
      typeListeners.delete(callback);
    }
  }

  /**
   * Handle incoming messages
   */
  private handleMessage(message: PreviewMessage) {
    // If we're the preview window and receive a ping, respond with pong
    if (message.type === 'BUILDER_PING' && this.isPreviewWindow) {
      this.send('PREVIEW_PONG', {});
      return;
    }

    const typeListeners = this.listeners.get(message.type);
    if (typeListeners) {
      typeListeners.forEach((callback) => callback(message.payload));
    }
  }

  /**
   * Handle storage events for fallback
   */
  private handleStorageEvent = (event: StorageEvent) => {
    if (event.key !== STORAGE_KEY || !event.newValue) return;

    try {
      const message: PreviewMessage = JSON.parse(event.newValue);
      this.handleMessage(message);
    } catch (e) {
      console.error('[PreviewBroadcast] Failed to parse storage message:', e);
    }
  };

  /**
   * Cleanup
   */
  destroy() {
    if (this.channel) {
      this.channel.close();
      this.channel = null;
    }
    if (this.useStorageFallback) {
      window.removeEventListener('storage', this.handleStorageEvent);
    }
    this.listeners.clear();
  }
}

// Singleton instance
export const previewBroadcast = new PreviewBroadcastService();

/**
 * Open preview in a new window
 */
export function openPreviewWindow(siteId?: number, pageId?: number): Window | null {
  const basePath = window.location.origin;
  let previewUrl = `${basePath}/preview`;

  if (siteId) {
    previewUrl += `/${siteId}`;
    if (pageId) {
      previewUrl += `/${pageId}`;
    }
  }

  // Window features for a typical preview window
  const features = [
    'width=1280',
    'height=800',
    'menubar=no',
    'toolbar=no',
    'location=no',
    'status=no',
    'resizable=yes',
    'scrollbars=yes',
  ].join(',');

  const previewWindow = window.open(previewUrl, 'vsd-preview', features);

  if (previewWindow) {
    previewWindow.focus();
  }

  return previewWindow;
}
