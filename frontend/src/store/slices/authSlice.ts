import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface User {
  username: string;
  role: string;
  department: string;
}

interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
}

// Restore auth state from localStorage
const token = localStorage.getItem('token');
const userStr = localStorage.getItem('user');
let user: User | null = null;

if (userStr) {
  try {
    user = JSON.parse(userStr);
  } catch (e) {
    console.error('Failed to parse user from localStorage');
  }
}

const initialState: AuthState = {
  isAuthenticated: !!token && !!user,
  user: user,
  token: token,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginSuccess: (state, action: PayloadAction<{ user: User; token: string }>) => {
      state.isAuthenticated = true;
      state.user = action.payload.user;
      state.token = action.payload.token;
      localStorage.setItem('token', action.payload.token);
      localStorage.setItem('user', JSON.stringify(action.payload.user));
    },
    logout: (state) => {
      state.isAuthenticated = false;
      state.user = null;
      state.token = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    },
    setUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
      state.isAuthenticated = true;
    },
  },
});

export const { loginSuccess, logout, setUser } = authSlice.actions;
export default authSlice.reducer;
