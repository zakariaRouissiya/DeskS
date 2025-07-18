import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { jwtDecode } from 'jwt-decode';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class AuthentificationService {
  private tokenKey = 'access_token';
  private userKey = 'currentUser';
  private apiUrl = environment.apiUrl + '/auth/login';

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, motDePasse: string): Observable<any> {
    return this.http.post(this.apiUrl, { email, motDePasse }).pipe(
      tap((response: any) => {
        
        this.setToken(response['access-token']);

        const userObject = {
          email: email,
          role: response.role,
        };

        this.setCurrentUser(userObject);
        
      })
    );
  }

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  removeToken(): void {
    localStorage.removeItem(this.tokenKey);
  }

  setCurrentUser(user: any): void {
    localStorage.setItem(this.userKey, JSON.stringify(user));
  }

  getCurrentUser(): any {
    const userData = localStorage.getItem(this.userKey);
    return userData ? JSON.parse(userData) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    if (!user?.role) return false;
    
    const roleString = user.role.toString().toUpperCase();
    
    const isAdminResult = roleString.includes('ADMINISTRATEUR') || 
                          roleString.includes('ADMIN') || 
                          roleString === 'ADMINISTRATEUR';
    
    return isAdminResult;
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    localStorage.removeItem('saved_email');
    sessionStorage.clear();
    this.router.navigate(['/login']);
  }

  getUserFromToken(): any {
    const token = this.getToken();
    if (!token) return null;

    try {
      const decoded: any = jwtDecode(token);
      return decoded;
    } catch {
      return null;
    }
  }
}