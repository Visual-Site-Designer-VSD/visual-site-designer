import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { useEffect } from 'react';
import './App.css';

// Pages
import { BuilderPage } from './pages/BuilderPage';
import { LoginPage } from './pages/LoginPage';
import { OAuth2CallbackPage } from './pages/OAuth2CallbackPage';

// Auth components
import { ProtectedRoute } from './components/auth/ProtectedRoute';

// Services
import { authService } from './services/authService';
import { registerCoreExportTemplates } from './services/coreExportTemplates';

// Register core export templates on module load (before components render)
registerCoreExportTemplates();

function App() {
  // Initialize auth on app load
  useEffect(() => {
    authService.initializeAuth().catch((err) => {
      console.log('Auth initialization failed:', err);
    });
  }, []);

  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />

        {/* Protected Builder Routes - Full Screen */}
        <Route
          path="/builder/sites/:siteId/pages/:pageId"
          element={
            <ProtectedRoute requiredRoles={['ADMIN', 'DESIGNER']}>
              <BuilderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/builder/new"
          element={
            <ProtectedRoute requiredRoles={['ADMIN', 'DESIGNER']}>
              <BuilderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/"
          element={
            <ProtectedRoute requiredRoles={['ADMIN', 'DESIGNER']}>
              <BuilderPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="*"
          element={
            <ProtectedRoute requiredRoles={['ADMIN', 'DESIGNER']}>
              <BuilderPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;
