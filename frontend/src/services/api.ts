import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // Set Content-Type only for non-FormData requests
    // Axios will automatically set multipart/form-data with boundary for FormData
    if (!(config.data instanceof FormData)) {
      config.headers['Content-Type'] = config.headers['Content-Type'] || 'application/json';
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // A 401 from the login call means "those credentials were wrong", not "your session
    // expired". Redirecting there would reload the page and throw away the very error
    // the form needs to show, so the sign-in endpoints are exempt.
    const url = error.config?.url ?? '';
    const isSignInAttempt = url.includes('/auth/login') || url.includes('/auth/register');

    if (error.response?.status === 401 && !isSignInAttempt) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
