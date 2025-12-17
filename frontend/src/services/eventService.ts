import { ComponentInstance } from '../types/builder';

/**
 * Request body for backend event invocation
 */
export interface EventInvokeRequest {
  instanceId: string;
  componentId: string;
  pluginId: string;
  eventType: string;
  timestamp: number;
  props: Record<string, any>;
  styles: Record<string, string>;
  eventData: Record<string, any>;
  pageId?: number;
  pageName?: string;
  source: string;
}

/**
 * Response from backend event invocation
 */
export interface EventInvokeResponse {
  status: 'success' | 'failure' | 'partial';
  message?: string;
  data?: Record<string, any>;
  errors?: Record<string, string>;
  propUpdates?: Record<string, any>;
  styleUpdates?: Record<string, string>;
  commands?: Record<string, any>;
  broadcast?: Record<string, any>;
}

/**
 * Configuration for the event service
 */
interface EventServiceConfig {
  baseUrl: string;
  timeout: number;
}

const defaultConfig: EventServiceConfig = {
  baseUrl: '/api/events',
  timeout: 30000,
};

/**
 * EventService - Handles communication with backend event handlers
 *
 * This service:
 * 1. Invokes backend event handlers via REST API
 * 2. Processes response commands (navigate, showMessage, etc.)
 * 3. Applies prop/style updates from backend
 * 4. Handles WebSocket connections for real-time events
 */
class EventServiceClass {
  private config: EventServiceConfig;
  private webSocket: WebSocket | null = null;
  private wsReconnectAttempts = 0;
  private wsMaxReconnectAttempts = 5;
  private wsListeners: Map<string, Set<(data: any) => void>> = new Map();

  constructor(config: Partial<EventServiceConfig> = {}) {
    this.config = { ...defaultConfig, ...config };
  }

  /**
   * Invoke a backend event handler
   */
  async invokeBackendHandler(
    component: ComponentInstance,
    eventType: string,
    eventData: Record<string, any> = {},
    pageContext?: { pageId?: number; pageName?: string }
  ): Promise<EventInvokeResponse> {
    const request: EventInvokeRequest = {
      instanceId: component.instanceId,
      componentId: component.componentId,
      pluginId: component.pluginId,
      eventType,
      timestamp: Date.now(),
      props: component.props,
      styles: component.styles,
      eventData,
      pageId: pageContext?.pageId,
      pageName: pageContext?.pageName,
      source: this.detectSource(),
    };

    try {
      const response = await fetch(`${this.config.baseUrl}/invoke`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('[EventService] Backend invocation failed:', error);
      return {
        status: 'failure',
        message: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * Process response commands from backend
   */
  processCommands(
    response: EventInvokeResponse,
    callbacks: {
      navigate?: (url: string, newTab?: boolean) => void;
      showMessage?: (message: string, type: string) => void;
      openModal?: (modalId: string) => void;
      closeModal?: (modalId: string) => void;
      refreshComponent?: (instanceId: string) => void;
      updateProps?: (props: Record<string, any>) => void;
      updateStyles?: (styles: Record<string, string>) => void;
    }
  ): void {
    if (!response.commands) return;

    const { commands } = response;

    // Navigate
    if (commands.navigate) {
      if (typeof commands.navigate === 'string') {
        callbacks.navigate?.(commands.navigate);
      } else {
        const nav = commands.navigate as { url: string; newTab?: boolean };
        callbacks.navigate?.(nav.url, nav.newTab);
      }
    }

    // Show message
    if (commands.showMessage) {
      const msg = commands.showMessage as { message: string; type: string };
      callbacks.showMessage?.(msg.message, msg.type);
    }

    // Open modal
    if (commands.openModal) {
      callbacks.openModal?.(commands.openModal as string);
    }

    // Close modal
    if (commands.closeModal) {
      callbacks.closeModal?.(commands.closeModal as string);
    }

    // Refresh component
    if (commands.refreshComponent) {
      callbacks.refreshComponent?.(commands.refreshComponent as string);
    }

    // Apply prop updates
    if (response.propUpdates && Object.keys(response.propUpdates).length > 0) {
      callbacks.updateProps?.(response.propUpdates);
    }

    // Apply style updates
    if (response.styleUpdates && Object.keys(response.styleUpdates).length > 0) {
      callbacks.updateStyles?.(response.styleUpdates);
    }
  }

  /**
   * Detect the source of the event
   */
  private detectSource(): string {
    // Check if we're in the builder preview or a published page
    if (window.location.pathname.includes('/builder')) {
      return 'builder-preview';
    }
    if (window.location.pathname.includes('/preview')) {
      return 'page-preview';
    }
    return 'published-page';
  }

  // ==================== WebSocket Support ====================

  /**
   * Connect to WebSocket for real-time events
   */
  connectWebSocket(wsUrl?: string): void {
    if (this.webSocket?.readyState === WebSocket.OPEN) {
      return;
    }

    const url = wsUrl || `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/events`;

    try {
      this.webSocket = new WebSocket(url);

      this.webSocket.onopen = () => {
        console.log('[EventService] WebSocket connected');
        this.wsReconnectAttempts = 0;
      };

      this.webSocket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          this.handleWebSocketMessage(message);
        } catch (error) {
          console.error('[EventService] Failed to parse WebSocket message:', error);
        }
      };

      this.webSocket.onclose = () => {
        console.log('[EventService] WebSocket disconnected');
        this.attemptReconnect(url);
      };

      this.webSocket.onerror = (error) => {
        console.error('[EventService] WebSocket error:', error);
      };
    } catch (error) {
      console.error('[EventService] Failed to connect WebSocket:', error);
    }
  }

  /**
   * Attempt to reconnect WebSocket
   */
  private attemptReconnect(url: string): void {
    if (this.wsReconnectAttempts >= this.wsMaxReconnectAttempts) {
      console.warn('[EventService] Max WebSocket reconnect attempts reached');
      return;
    }

    this.wsReconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.wsReconnectAttempts), 30000);

    console.log(`[EventService] Reconnecting WebSocket in ${delay}ms (attempt ${this.wsReconnectAttempts})`);

    setTimeout(() => {
      this.connectWebSocket(url);
    }, delay);
  }

  /**
   * Handle incoming WebSocket message
   */
  private handleWebSocketMessage(message: { eventType: string; data: any }): void {
    const listeners = this.wsListeners.get(message.eventType);
    if (listeners) {
      listeners.forEach((listener) => {
        try {
          listener(message.data);
        } catch (error) {
          console.error('[EventService] WebSocket listener error:', error);
        }
      });
    }

    // Also notify global listeners
    const globalListeners = this.wsListeners.get('*');
    if (globalListeners) {
      globalListeners.forEach((listener) => {
        try {
          listener(message);
        } catch (error) {
          console.error('[EventService] WebSocket global listener error:', error);
        }
      });
    }
  }

  /**
   * Subscribe to WebSocket events
   */
  subscribeToRealTimeEvent(eventType: string, callback: (data: any) => void): () => void {
    if (!this.wsListeners.has(eventType)) {
      this.wsListeners.set(eventType, new Set());
    }
    this.wsListeners.get(eventType)!.add(callback);

    // Return unsubscribe function
    return () => {
      this.wsListeners.get(eventType)?.delete(callback);
    };
  }

  /**
   * Disconnect WebSocket
   */
  disconnectWebSocket(): void {
    if (this.webSocket) {
      this.webSocket.close();
      this.webSocket = null;
    }
  }

  /**
   * Send message via WebSocket
   */
  sendWebSocketMessage(eventType: string, data: any): void {
    if (this.webSocket?.readyState === WebSocket.OPEN) {
      this.webSocket.send(JSON.stringify({ eventType, data }));
    } else {
      console.warn('[EventService] WebSocket not connected, cannot send message');
    }
  }
}

// Export singleton instance
export const eventService = new EventServiceClass();

// Export class for testing
export { EventServiceClass };
