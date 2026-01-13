# CRITICAL: Dimension Synchronization Rules

## GOLDEN RULE - DO NOT CHANGE

The following CSS classes MUST ALWAYS have synchronized (identical) width and height:

1. `.resizable-component.child-component`
2. `.resizable-content`
3. `.component-content`
4. `.draggable-component.child-component`

And for layout containers:
5. `.layout-placeholder`
6. `.children-container`

And for leaf components (Image, Label, etc.):
7. `.component-placeholder`
8. The renderer's container (e.g., ImageRenderer's outermost div)

## Component Hierarchy

```
.draggable-component.child-component
  └── .component-content
      └── .resizable-component.child-component
          └── .resizable-content
              └── .component-placeholder (for leaf) OR .layout-placeholder (for containers)
                  └── [Renderer Component] (e.g., ImageRenderer's div)
```

## Why This Matters

When a parent container is resized:
- The resized pixel dimensions are stored in `component.size.width` and `component.size.height`
- ALL wrapper elements must use `width: 100%` and `height: 100%` to inherit from the parent
- The topmost element (resizable-component) controls the actual pixel size
- All descendants fill that size via the 100% chain

## The Height Chain

```
Container with explicit height (e.g., 300px from resize)
  └── .draggable-component (height from inline styles)
      └── .component-content (height: 100%)
          └── .resizable-component (height from getContainerStyles())
              └── .resizable-content (height: 100%)
                  └── .layout-placeholder OR .component-placeholder (height: 100%)
                      └── .children-container (height: 100%) [for layouts]
                          └── Child components...
```

## Key Files

- `ResizableComponent.tsx` - Sets dimensions via `getContainerStyles()`
- `ResizableComponent.css` - CSS rules for resizable wrappers
- `DraggableComponent.tsx` - Sets dimensions via `getComponentStyles()`
- `DraggableComponent.css` - CSS rules for draggable wrappers
- `BuilderCanvas.tsx` - Wraps leaf components in `.component-placeholder` with `height: 100%`
- `BuilderCanvas.css` - CSS rules for placeholders and containers

## CRITICAL: Leaf Component Wrapper

Non-layout components (Image, Label, etc.) MUST be wrapped in a `.component-placeholder` div
with `width: 100%` and `height: 100%` inline styles. This happens in `BuilderCanvas.tsx`
in the `renderComponent()` function. Without this wrapper, the height chain breaks and
leaf components won't follow their parent container's resize.

## NEVER DO

1. Never set `height: auto` on any of these wrapper elements when the parent has explicit dimensions
2. Never use `fit-content` for height on containers that need to fill their parent
3. Never add min-height constraints larger than 40px that would prevent shrinking
4. Never calculate min-size based on children's rendered dimensions (causes circular dependency)

## ALWAYS DO

1. Use `height: 100%` for all wrapper elements to inherit from parent
2. Use `width: 100%` for wrapper elements (except when explicit width is set)
3. Keep min-height at 40px to allow resizing to small sizes
4. Ensure the Image component uses `height: 100%` when inside a parent container
5. Use `align-self: stretch` for Image child components (overrides `flex-start`)
6. Use `min-width: 0` on ALL wrapper elements to allow shrinking below content width

## CRITICAL: Image Component CSS

Image components have special CSS rules in `DraggableComponent.css` that override the
default `align-self: flex-start` from `.child-component`. This is necessary because:

- `.child-component` uses `align-self: flex-start` to prevent stretching
- But Images need to stretch to fill their parent Container
- The Image CSS selector `[data-component-id^="Image-"]` sets `align-self: stretch`
- All Image wrapper elements use `width: 100%` and `height: 100%`

Without `align-self: stretch`, the Image won't fill the parent's height even with `height: 100%`.

## CRITICAL: Resize Must Update Both Wrappers

During resize operations, BOTH `.draggable-component` AND `.resizable-component` must be updated:

- `.draggable-component` controls the **width** in the flex/grid layout
- `.resizable-component` controls the **height** (and inner dimensions)

The `ResizableComponent.tsx` `handleResizeMove()` function must update both elements:

```typescript
// Update resizable-component
componentRef.current.style.width = `${newWidth}px`;
componentRef.current.style.height = `${newHeight}px`;

// ALSO update the parent draggable-component
const draggableWrapper = componentRef.current.closest('.draggable-component');
if (draggableWrapper) {
  draggableWrapper.style.width = `${newWidth}px`;
  draggableWrapper.style.height = `${newHeight}px`;
}
```

Without updating both, width resize won't work because the outer wrapper constrains the inner.

---

## IMAGE COMPONENT SIZING

### Problem Solved (2026-01-13)

Image components inside containers were not filling their parent's height properly. The issue was:

1. `component.size.height` was storing `'auto'` as a valid value
2. The ImageRenderer was using `storedHeight || '100%'` which resolved to `'auto'` instead of `'100%'`

### Solution

The ImageRenderer now treats `'auto'` as "not set" and falls back to `'100%'`:

```typescript
// OLD (broken):
const effectiveHeight = propsHeight || storedHeight || '100%';
// If storedHeight is 'auto', this resolves to 'auto' - NOT what we want

// NEW (fixed):
const effectiveHeight = propsHeight ||
  (storedHeight && storedHeight !== 'auto' ? storedHeight : null) ||
  '100%';
// Now 'auto' is treated as "not set" and falls back to '100%'
```

### Image Container Sizing Logic

The ImageRenderer uses a priority system for dimensions:

1. **Props (from Properties Panel)** - Highest priority
2. **Stored dimensions (from resize)** - But ignore `'auto'` as it means "not set"
3. **Parent-aware defaults** - `100%` for children, `auto` for root-level

```typescript
// Width: props > stored (if not auto) > parent-aware default
const effectiveWidth = propsWidth ||
  (storedWidth && storedWidth !== 'auto' ? storedWidth : null) ||
  (hasParent ? '100%' : 'auto');

// Height: Always use '100%' to fill parent container
// Applied at the END of containerStyles to ensure it's not overridden
const containerStyles: React.CSSProperties = {
  width: effectiveWidth,
  // ... other styles ...
  ...(component.styles as React.CSSProperties),
  height: '100%',  // MUST be last to override any stored 'auto' value
};
```

### Key Files for Image Sizing

| File | Purpose |
|------|---------|
| `ImageRenderer.tsx` | Sets `height: '100%'` on container div |
| `DraggableComponent.css` | Special CSS for `[data-component-id^="Image-"]` |
| `ResizableComponent.tsx` | Handles resize and stores new dimensions |

### Plugin Build Process

After modifying ImageRenderer.tsx, you MUST rebuild the plugin bundle:

```bash
# 1. Build the frontend bundle
cd plugins/image-component-plugin/frontend
npm run build

# 2. Package the Maven JAR (includes bundle.js)
cd ..
mvn clean package -DskipTests

# 3. Copy to core plugins (for runtime)
cp target/image-component-plugin-1.0.0.jar ../core/plugins/

# 4. Restart Spring Boot application to reload plugin
```

The browser loads `bundle.js` from the JAR, NOT the source TypeScript. If changes don't appear:
- Verify bundle was rebuilt (`npm run build`)
- Verify JAR was rebuilt (`mvn package`)
- Verify JAR was copied to `core/plugins/`
- Restart the Spring Boot application
- Hard refresh browser (Ctrl+Shift+R)

---

## Last Updated

2026-01-13 - Fixed Image component height to always use `100%` and ignore stored `'auto'` values
