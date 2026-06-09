import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { API_BASE_URL } from './api.config';

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  photo: string | null;
  role: 'ROLE_USER' | 'ROLE_ADMIN' | 'ROLE_EDITOR';
  createdAt: string;
  active: boolean;
  planType: 'FREE' | 'PREMIUM_MONTHLY' | 'PREMIUM_YEARLY';
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

interface StoredSession extends AuthTokenResponse {
  storedAt: number;
}

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly http = inject(HttpClient);
  private readonly storageKey = 'ld-auth-session';
  private readonly sessionState = signal<StoredSession | null>(this.readStoredSession());

  readonly session = computed(() => this.sessionState());
  readonly currentUser = computed(() => this.sessionState()?.user ?? null);
  readonly accessToken = computed(() => this.sessionState()?.accessToken ?? null);
  readonly isAuthenticated = computed(() => this.sessionState() !== null);

  constructor() {
    if (this.sessionState()) {
      this.syncCurrentUser().subscribe({
        error: () => this.logout()
      });
    }
  }

  login(payload: { email: string; password: string }): Observable<AuthTokenResponse> {
    return this.http.post<AuthTokenResponse>(`${API_BASE_URL}/auth/login`, payload).pipe(
      tap((response) => this.persistSession(response))
    );
  }

  register(payload: { name: string; email: string; password: string }): Observable<AuthTokenResponse> {
    return this.http.post<AuthTokenResponse>(`${API_BASE_URL}/auth/register`, { ...payload, role: 'ROLE_USER' }).pipe(
      tap((response) => this.persistSession(response))
    );
  }

  syncCurrentUser(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${API_BASE_URL}/users/me`).pipe(
      tap((user) => this.updateStoredUser(user))
    );
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    this.sessionState.set(null);
  }

  private persistSession(response: AuthTokenResponse): void {
    const session: StoredSession = { ...response, storedAt: Date.now() };
    localStorage.setItem(this.storageKey, JSON.stringify(session));
    this.sessionState.set(session);
  }

  private updateStoredUser(user: AuthUser): void {
    const currentSession = this.sessionState();
    if (!currentSession) {
      return;
    }

    const nextSession: StoredSession = { ...currentSession, user };
    localStorage.setItem(this.storageKey, JSON.stringify(nextSession));
    this.sessionState.set(nextSession);
  }

  private readStoredSession(): StoredSession | null {
    const rawValue = localStorage.getItem(this.storageKey);
    if (!rawValue) {
      return null;
    }

    try {
      return JSON.parse(rawValue) as StoredSession;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}