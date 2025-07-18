import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface Utilisateur {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  department?: {
    id: number;
    nom: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/users`;
  private baseUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    });
  }

  getUserProfile(): Observable<any> {
    return this.http.get(`${this.apiUrl}/profile`, { headers: this.getHeaders() }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération du profil:', error);
        throw error;
      })
    );
  }

  updateUserProfile(userData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/profile`, userData, { 
      headers: this.getHeaders() 
    }).pipe(
      map((response: any) => {
        if (response && response.success) {
          return response.user || response;
        }
        return response;
      }),
      catchError(error => {
        console.error('Erreur lors de la mise à jour du profil:', error);
        throw error;
      })
    );
  }

  updatePassword(passwordData: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/password`, passwordData, { 
      headers: this.getHeaders() 
    }).pipe(
      map((response: any) => {
        if (response && response.success === true) {
          return response;
        } else if (response && response.success === false) {
          throw new Error(response.message || 'Erreur lors de la mise à jour du mot de passe');
        }
        return response;
      }),
      catchError(error => {
        console.error('Erreur lors de la mise à jour du mot de passe:', error);
        throw error;
      })
    );
  }

  getEmployees(): Observable<Utilisateur[]> {
    return this.http.get<any>(`${this.baseUrl}/employees`, { headers: this.getHeaders() }).pipe(
      map(response => {
        if (response && Array.isArray(response)) {
          return response;
        } else if (response && response.success && Array.isArray(response.users)) {
          return response.users;
        } else if (response && response.users && Array.isArray(response.users)) {
          return response.users;
        }
        return response || [];
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des employés:', error);
        return of([]);
      })
    );
  }

  getTechniciens(): Observable<Utilisateur[]> {
    return this.http.get<any>(`${this.baseUrl}/techniciens`, { headers: this.getHeaders() }).pipe(
      map(response => {
        if (response && Array.isArray(response)) {
          return response;
        } else if (response && response.success && Array.isArray(response.users)) {
          return response.users;
        } else if (response && response.users && Array.isArray(response.users)) {
          return response.users;
        }
        return response || [];
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des techniciens:', error);
        return of([]);
      })
    );
  }

  getTechniciensByDepartment(departmentId: number): Observable<Utilisateur[]> {
    const url = `${environment.apiUrl}/utilisateurs/departement/${departmentId}/role/TECHNICIEN_SUPPORT`;
    return this.http.get<Utilisateur[]>(url, { 
      headers: this.getHeaders() 
    }).pipe(
      map(response => response),
      catchError(error => {
        console.error('Erreur lors de la récupération des techniciens par département:', error);
        return of([]);
      })
    );
  }

  getTechniciensForUser(userId: number): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.baseUrl}/techniciens/pour-utilisateur/${userId}`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des techniciens pour l\'utilisateur:', error);
        return of([]);
      })
    );
  }

  getUsersByRole(role: string): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.baseUrl}/utilisateurs/role/${role}`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(error => {
        console.error(`Erreur lors de la récupération des utilisateurs avec le rôle ${role}:`, error);
        return of([]);
      })
    );
  }

  getManagers(): Observable<Utilisateur[]> {
    return this.getUsersByRole('MANAGER_IT');
  }

  getAdministrateurs(): Observable<Utilisateur[]> {
    return this.getUsersByRole('ADMINISTRATEUR');
  }

  getUserById(userId: number): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}/utilisateurs/${userId}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération de l\'utilisateur:', error);
        return throwError(() => error);
      })
    );
  }

  getEmployeesByDepartment(departmentId: number): Observable<Utilisateur[]> {
    const url = `${environment.apiUrl}/utilisateurs/departement/${departmentId}/role/EMPLOYEE`;
    return this.http.get<Utilisateur[]>(url, { 
      headers: this.getHeaders() 
    }).pipe(
      map(response => response),
      catchError(error => {
        console.error('Erreur lors de la récupération des employés par département:', error);
        return of([]);
      })
    );
  }
}