/**
 * Type definitions for article-list-plugin renderers.
 */

export interface ComponentPosition {
  x: number;
  y: number;
}

export interface ComponentSize {
  width: number;
  height: number;
}

export interface ComponentInstance {
  instanceId: string;
  pluginId: string;
  componentId: string;
  componentCategory?: string;
  parentId?: string | null;
  position: ComponentPosition;
  size: ComponentSize;
  props: Record<string, unknown>;
  styles: Record<string, string>;
  children?: ComponentInstance[];
  zIndex?: number;
  displayOrder?: number;
  isVisible?: boolean;
  reactBundlePath?: string;
}

export interface RendererProps {
  component: ComponentInstance;
  isEditMode: boolean;
}

export type RendererComponent = React.FC<RendererProps>;

export interface PluginBundle {
  pluginId: string;
  renderers: Record<string, RendererComponent>;
  styles?: string;
  version?: string;
}

/**
 * Article data shape returned by the article API.
 */
export interface Article {
  id: string;
  title: string;
  summary?: string;
  content?: string;
  coverImageUrl?: string;
  author?: {
    id: string;
    name: string;
    avatarUrl?: string;
  };
  tags?: string[];
  publishedAt?: string;
  createdAt?: string;
  status?: 'draft' | 'published';
}

/**
 * Paginated response from article API.
 */
export interface ArticleListResponse {
  content: Article[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
