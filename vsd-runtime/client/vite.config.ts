import { resolve } from 'path';

// Same convention as the plugins: build with the root frontend's toolchain and React copy
// (run `npm ci` in ../../frontend once). Output lands on the runtime's classpath so Spring
// serves it at /_vsd/client/.
const frontendModules = resolve(__dirname, '../../frontend/node_modules');

export default {
  esbuild: { jsx: 'automatic' },
  define: { 'process.env.NODE_ENV': JSON.stringify('production') },
  resolve: {
    alias: [
      { find: /^react\/jsx-runtime$/, replacement: resolve(frontendModules, 'react/jsx-runtime.js') },
      { find: /^react-dom\/client$/, replacement: resolve(frontendModules, 'react-dom/client.js') },
      { find: /^react-dom$/, replacement: resolve(frontendModules, 'react-dom/index.js') },
      { find: /^react$/, replacement: resolve(frontendModules, 'react/index.js') },
    ],
  },
  build: {
    outDir: resolve(__dirname, '../src/main/resources/vsd-client'),
    emptyOutDir: true,
    sourcemap: true,
    lib: {
      entry: resolve(__dirname, 'src/main.tsx'),
      name: 'VsdRuntimeClient',
      formats: ['iife'],
      fileName: () => 'runtime.js',
    },
    rollupOptions: {
      output: {
        assetFileNames: (info: { name?: string }) => (info.name?.endsWith('.css') ? 'runtime.css' : info.name || 'asset'),
      },
    },
  },
};
