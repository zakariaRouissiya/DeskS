import { AuthentificationService } from './authentification';
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { map, catchError, switchMap } from 'rxjs/operators';
import { Utilisateur } from '../models/ticket.model';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = environment.apiUrl + '/admin';

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthentificationService
  ) {}

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  // ==================== GESTION DES UTILISATEURS (ADMIN) ====================
  getUsers(params: any = {}): Observable<any> {
    // Supprimer la pagination par défaut pour récupérer TOUS les utilisateurs
    const allUsersParams = {
      ...params,
      page: 0,
      size: 1000 // Utiliser une taille suffisamment grande pour récupérer tous les utilisateurs
    };
    
    return this.http.get(`${this.apiUrl}/utilisateurs`, { 
      params: allUsersParams, 
      headers: this.getAuthHeaders() 
    }).pipe(
      map((response: any) => {
        console.log('Réponse serveur complète (utilisateurs):', response);
        const users = response.utilisateurs || response.users || response.content || [];
        return {
          ...response,
          utilisateurs: users,
          totalElements: response.totalElements || users.length
        };
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des utilisateurs:', error);
        return of({ utilisateurs: [], totalElements: 0 });
      })
    );
  }

  // Méthode alternative pour récupérer explicitement TOUS les utilisateurs
  getAllUsers(): Observable<any> {
    return this.http.get(`${this.apiUrl}/utilisateurs`, { 
      params: { page: 0, size: 1000 }, // Taille suffisante
      headers: this.getAuthHeaders() 
    }).pipe(
      map((response: any) => {
        console.log('Tous les utilisateurs récupérés:', response);
        const users = response.utilisateurs || response.users || response.content || [];
        return {
          utilisateurs: users,
          totalElements: response.totalElements || users.length
        };
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération de tous les utilisateurs:', error);
        return of({ utilisateurs: [], totalElements: 0 });
      })
    );
  }

  createUser(userData: any): Observable<any> {
    console.log('Données envoyées:', userData);
    return this.http.post(`${this.apiUrl}/creer-utilisateur`, userData, {
      headers: this.getAuthHeaders()
    });
  }

  updateUser(id: number, payload: any): Observable<any> {
    console.log('Données de modification envoyées:', payload);
    return this.http.put(`${this.apiUrl}/utilisateur/${id}`, payload, {
      headers: this.getAuthHeaders()
    });
  }

  deleteUser(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/utilisateur/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  // ==================== GESTION DES DÉPARTEMENTS (ADMIN) ====================
  getDepartments(params: any = {}): Observable<any> {
    // Supprimer la pagination par défaut pour récupérer TOUS les départements
    const allDepartmentsParams = {
      ...params,
      page: 0,
      size: 1000 // Utiliser une taille suffisamment grande pour récupérer tous les départements
    };
    
    return this.http.get(`${this.apiUrl}/departements`, { 
      params: allDepartmentsParams, 
      headers: this.getAuthHeaders() 
    }).pipe(
      map((response: any) => {
        console.log('Réponse brute des départements:', response);
        
        let departments = [];
        
        if (response && response.departements && Array.isArray(response.departements)) {
          departments = response.departements;
        } else if (response && response.content && Array.isArray(response.content)) {
          departments = response.content;
        } else if (response && Array.isArray(response)) {
          departments = response;
        } else {
          console.error('Structure de réponse inattendue:', response);
          departments = [];
        }
        
        return {
          departements: departments.map((dept: any) => ({
            id: dept.id,
            nom: dept.nom,
            description: dept.description
          })),
          totalElements: response?.totalElements || departments.length
        };
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des départements:', error);
        return of({ departements: [], totalElements: 0 });
      })
    );
  }

  // Méthode alternative pour récupérer explicitement TOUS les départements
  getAllDepartments(): Observable<any> {
    return this.http.get(`${this.apiUrl}/departements`, { 
      params: { page: 0, size: 1000 }, // Taille suffisante
      headers: this.getAuthHeaders() 
    }).pipe(
      map((response: any) => {
        console.log('Tous les départements récupérés:', response);
        
        let departments = [];
        
        if (response && response.departements && Array.isArray(response.departements)) {
          departments = response.departements;
        } else if (response && response.content && Array.isArray(response.content)) {
          departments = response.content;
        } else if (response && Array.isArray(response)) {
          departments = response;
        } else {
          departments = [];
        }
        
        return {
          departements: departments.map((dept: any) => ({
            id: dept.id,
            nom: dept.nom,
            description: dept.description
          })),
          totalElements: response?.totalElements || departments.length
        };
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération de tous les départements:', error);
        return of({ departements: [], totalElements: 0 });
      })
    );
  }

  createDepartment(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/creer-departement`, payload, {
      headers: this.getAuthHeaders()
    });
  }

  updateDepartment(id: number, payload: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/departement/${id}`, payload, {
      headers: this.getAuthHeaders()
    });
  }

  deleteDepartment(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/departement/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  // ==================== STATISTIQUES DASHBOARD (ADMIN) ====================
  getDashboardStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/dashboard-stats`, {
      headers: this.getAuthHeaders()
    }).pipe(
      map((response: any) => {
        console.log('Statistiques dashboard récupérées:', response);
        return response;
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des statistiques:', error);
        return of({});
      })
    );
  }

  // ==================== RÉCUPÉRATION DES TECHNICIENS (ADMIN) ====================
  getTechniciens(): Observable<any[]> {
    // Récupérer tous les utilisateurs puis filtrer les techniciens
    return this.http.get<any>(`${this.apiUrl}/utilisateurs`, {
      params: { page: 0, size: 1000 }, // Récupérer tous les utilisateurs
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => {
        console.log('Réponse pour techniciens:', response);
        const users = response?.utilisateurs || response?.users || response?.content || [];
        // Filtrer pour ne récupérer que les techniciens
        const techniciens = users.filter((user: { role: string; }) => 
          user.role === 'ROLE_TECHNICIEN_SUPPORT' || 
          user.role === 'TECHNICIEN_SUPPORT'
        );
        console.log('Techniciens filtrés:', techniciens);
        return techniciens;
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des techniciens:', error);
        return of([]);
      })
    );
  }

  // ==================== RÉCUPÉRATION DES MANAGERS (ADMIN) ====================
  getManagers(): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/utilisateurs`, {
      params: { page: 0, size: 1000 }, // Récupérer tous les utilisateurs
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => {
        console.log('Réponse pour managers:', response);
        const users = response?.utilisateurs || response?.users || response?.content || [];
        // Filtrer pour ne récupérer que les managers
        const managers = users.filter((user: { role: string; }) => 
          user.role === 'ROLE_MANAGER_IT' || 
          user.role === 'MANAGER_IT'
        );
        console.log('Managers filtrés:', managers);
        return managers;
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des managers:', error);
        return of([]);
      })
    );
  }

  // ==================== RÉCUPÉRATION DES EMPLOYÉS (ADMIN) ====================
  getEmployees(): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/utilisateurs`, {
      params: { page: 0, size: 1000 }, // Récupérer tous les utilisateurs
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => {
        console.log('Réponse pour employés:', response);
        const users = response?.utilisateurs || response?.users || response?.content || [];
        // Filtrer pour ne récupérer que les employés
        const employees = users.filter((user: { role: string; }) => 
          user.role === 'ROLE_EMPLOYEE' || 
          user.role === 'EMPLOYEE'
        );
        console.log('Employés filtrés:', employees);
        return employees;
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des employés:', error);
        return of([]);
      })
    );
  }

  // ==================== RÉCUPÉRATION DES ADMINISTRATEURS (ADMIN) ====================
  getAdministrators(): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/utilisateurs`, {
      params: { page: 0, size: 1000 }, // Récupérer tous les utilisateurs
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => {
        console.log('Réponse pour administrateurs:', response);
        const users = response?.utilisateurs || response?.users || response?.content || [];
        // Filtrer pour ne récupérer que les administrateurs
        const administrators = users.filter((user: { role: string; }) => 
          user.role === 'ROLE_ADMINISTRATEUR' || 
          user.role === 'ADMINISTRATEUR'
        );
        console.log('Administrateurs filtrés:', administrators);
        return administrators;
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des administrateurs:', error);
        return of([]);
      })
    );
  }

  // ==================== RÉCUPÉRATION PAR DÉPARTEMENT (ADMIN) ====================
  getUsersByDepartment(departmentId: number): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/utilisateurs`, {
      params: { 
        page: 0, 
        size: 1000, // Récupérer tous les utilisateurs
        departmentId: departmentId.toString() 
      },
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => {
        console.log('Réponse pour utilisateurs par département:', response);
        const users = response?.utilisateurs || response?.users || response?.content || [];
        // Filtrer par département côté client si nécessaire
        const departmentUsers = users.filter((user: any) => 
          user.department?.id === departmentId || user.departement?.id === departmentId
        );
        console.log('Utilisateurs du département filtrés:', departmentUsers);
        return departmentUsers;
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des utilisateurs par département:', error);
        return of([]);
      })
    );
  }

  // ==================== RECHERCHE GLOBALE (ADMIN) ====================
  searchUsers(searchTerm: string): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/utilisateurs`, {
      params: { 
        page: 0, 
        size: 1000, // Récupérer tous les utilisateurs
        search: searchTerm 
      },
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => {
        console.log('Réponse pour recherche utilisateurs:', response);
        const users = response?.utilisateurs || response?.users || response?.content || [];
        // Filtrer par terme de recherche côté client si nécessaire
        const searchResults = users.filter((user: any) => {
          const term = searchTerm.toLowerCase();
          return (
            user.nom?.toLowerCase().includes(term) ||
            user.prenom?.toLowerCase().includes(term) ||
            user.email?.toLowerCase().includes(term) ||
            user.role?.toLowerCase().includes(term) ||
            user.department?.nom?.toLowerCase().includes(term) ||
            user.departement?.nom?.toLowerCase().includes(term)
          );
        });
        console.log('Résultats de recherche filtrés:', searchResults);
        return searchResults;
      }),
      catchError(error => {
        console.error('Erreur lors de la recherche d\'utilisateurs:', error);
        return of([]);
      })
    );
  }

  // ==================== MÉTHODES UTILITAIRES ====================
  // Méthode pour compter les utilisateurs par rôle
  getUserCountByRole(): Observable<{[key: string]: number}> {
    return this.getAllUsers().pipe(
      map(response => {
        const users = response.utilisateurs || [];
        const count: {[key: string]: number} = {};
        
        users.forEach((user: any) => {
          const role = user.role || 'UNKNOWN';
          count[role] = (count[role] || 0) + 1;
        });
        
        return count;
      })
    );
  }

  // Méthode pour compter les utilisateurs par département
  getUserCountByDepartment(): Observable<{[key: string]: number}> {
    return this.getAllUsers().pipe(
      map(response => {
        const users = response.utilisateurs || [];
        const count: {[key: string]: number} = {};
        
        users.forEach((user: any) => {
          const departmentName = user.department?.nom || user.departement?.nom || 'Sans département';
          count[departmentName] = (count[departmentName] || 0) + 1;
        });
        
        return count;
      })
    );
  }
}
