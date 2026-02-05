import { create } from 'zustand';

interface User {
  id: number;
  email?: string;
}

interface AuthStore {
  user: User | null;
  isAuthenticated: boolean;
  login: (user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  isAuthenticated: false,
  login: (user) => {
    localStorage.setItem('userId', user.id.toString());
    set({ user, isAuthenticated: true });
  },
  logout: () => {
    localStorage.removeItem('userId');
    set({ user: null, isAuthenticated: false });
  },
}));
