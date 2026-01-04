import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

// Path to generated-types directory for cross-plugin imports
const generatedTypesDir = resolve(__dirname, '../../../generated-types');

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: resolve(__dirname, '../src/main/resources/frontend'),
    emptyOutDir: true,
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'NewsletterFormPlugin',
      formats: ['iife'],
      fileName: () => 'bundle.js',
    },
    rollupOptions: {
      external: ['react', 'react-dom', 'react/jsx-runtime'],
      output: {
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM',
          'react/jsx-runtime': 'jsxRuntime',
        },
        extend: true,
        exports: 'named',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name === 'style.css') {
            return 'bundle.css';
          }
          return assetInfo.name || 'asset';
        },
      },
    },
    sourcemap: true,
    minify: 'esbuild',
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      // Cross-plugin imports - VSD component aliases
      '@vsd/components': resolve(generatedTypesDir, 'plugins/index.ts'),
      '@vsd/button-component-plugin': resolve(generatedTypesDir, 'plugins/button-component-plugin/index.ts'),
      '@vsd/label-component-plugin': resolve(generatedTypesDir, 'plugins/label-component-plugin/index.ts'),
      '@vsd/textbox-component-plugin': resolve(generatedTypesDir, 'plugins/textbox-component-plugin/index.ts'),
    },
  },
});
