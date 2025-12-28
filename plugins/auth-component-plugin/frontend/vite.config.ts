import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

/**
 * Vite configuration for building the auth-component-plugin frontend bundle.
 *
 * This builds a library bundle that:
 * 1. Exports all component renderers
 * 2. Treats React as an external (provided by host app)
 * 3. Outputs to ../src/main/resources/frontend/ so it's included in the JAR
 */
export default defineConfig({
  plugins: [react()],
  build: {
    // Output to Maven resources directory so it's bundled in the JAR
    outDir: resolve(__dirname, '../src/main/resources/frontend'),
    emptyOutDir: true,
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'AuthComponentPlugin',
      formats: ['es'],
      fileName: () => 'bundle.js',
    },
    rollupOptions: {
      // React is provided by the host application
      external: ['react', 'react-dom', 'react/jsx-runtime'],
      output: {
        // Provide global variables for externals in UMD build
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM',
          'react/jsx-runtime': 'jsxRuntime',
        },
        // Single CSS file
        assetFileNames: (assetInfo) => {
          if (assetInfo.name === 'style.css') {
            return 'bundle.css';
          }
          return assetInfo.name || 'asset';
        },
      },
    },
    // Generate source maps for debugging
    sourcemap: true,
    // Minimize for production
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: false, // Keep console logs for debugging
      },
    },
  },
  // Resolve aliases
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
});
