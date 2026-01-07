import type React from 'react';

export interface ComponentPosition {
  x: number;
  y: number;
}

export interface ComponentSize {
  width: string;
  height: string;
}

export interface ComponentInstance {
  instanceId: string;
  pluginId: string;
  componentId: string;
  position: ComponentPosition;
  size: ComponentSize;
  props: Record<string, unknown>;
  styles: Record<string, string>;
  children?: ComponentInstance[];
}

export interface RendererProps {
  component?: ComponentInstance;
  isEditMode?: boolean;
  [key: string]: unknown;
}

export type RendererComponent = React.FC<RendererProps>;

export interface PluginBundle {
  pluginId: string;
  renderers: Record<string, RendererComponent>;
  version?: string;
}
