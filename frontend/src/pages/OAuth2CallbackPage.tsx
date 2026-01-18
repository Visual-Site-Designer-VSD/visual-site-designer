import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { authService } from '../services/authService';
import './LoginPage.css';

/**
 * OAuth2 callback page that receives the JWT token after successful SSO login.
 * The backend redirects here with the token as a query parameter.
 */
export const OAuth2CallbackPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setTokens } = useAuthStore();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const handleCallback = async () => {
      const token = searchParams.get('token');
      const errorParam = searchParams.get('error');

      if (errorParam) {
        setError(`Authentication failed: ${errorParam}`);
        return;
      }

      if (!token) {
        setError('No authentication token received');
        return;
      }

      try {
        // Store the token (use same token as refresh since SSO doesn't provide separate refresh token)
        // Default expiration of 15 minutes (900 seconds)
        setTokens(token, token, 900);

        // Fetch the current user to verify the token works and populate user data
        await authService.getCurrentUser();

        // Redirect to the dashboard
        navigate('/', { replace: true });
      } catch (err) {
        console.error('Failed to authenticate with SSO token:', err);
        setError('Failed to complete authentication. Please try again.');
      }
    };

    handleCallback();
  }, [searchParams, setTokens, navigate]);

  if (error) {
    return (
      <div className="login-page">
        <div className="login-card">
          <h2 className="login-title">Authentication Failed</h2>
          <div className="error-message">{error}</div>
          <button
            onClick={() => navigate('/login', { replace: true })}
            className="btn btn-primary btn-block"
          >
            Back to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h2 className="login-title">Completing Sign In...</h2>
        <p className="login-subtitle">Please wait while we authenticate your session.</p>
        <div style={{ textAlign: 'center', marginTop: '20px' }}>
          <div className="spinner" style={{
            width: '40px',
            height: '40px',
            border: '4px solid #f3f3f3',
            borderTop: '4px solid #3498db',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            margin: '0 auto'
          }} />
        </div>
        <style>{`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    </div>
  );
};

export default OAuth2CallbackPage;
