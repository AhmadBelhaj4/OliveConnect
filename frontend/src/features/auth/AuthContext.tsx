import React, { createContext, useContext, useEffect, useState } from 'react';
import { apiFetch, tokenStore } from '../../api/http';
import type { LoginRequest, LoginResponse, UserResponse } from '../../types/contract';

interface AuthState {
  user: UserResponse | null;
  loading: boolean;
  login: (req: LoginRequest) => Promise<UserResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(Boolean(tokenStore.get()));

  useEffect(() => {
    if (!tokenStore.get()) return;
    apiFetch<UserResponse>('/users/me')
      .then(setUser)
      .catch(() => {
        tokenStore.clear();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const login = async (req: LoginRequest) => {
    const res = await apiFetch<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(req),
    });
    tokenStore.set(res.accessToken);
    setUser(res.user);
    return res.user;
  };

  const logout = () => {
    tokenStore.clear();
    setUser(null);
  };

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans AuthProvider');
  return ctx;
}
