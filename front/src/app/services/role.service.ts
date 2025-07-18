import { Injectable } from '@angular/core';
import { AuthentificationService } from './authentification';

export enum UserRole {
  EMPLOYEE = 'EMPLOYEE',
  TECHNICIEN_SUPPORT = 'TECHNICIEN_SUPPORT',
  MANAGER_IT = 'MANAGER_IT',
  ADMINISTRATEUR = 'ADMINISTRATEUR'
}

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  
  constructor(private readonly authService: AuthentificationService) {}

  getCurrentUserRole(): UserRole | null {
    const user = this.authService.getCurrentUser();
    if (!user?.role) return null;
    const roleString = user.role.toString().toUpperCase();
    if (roleString.includes('ADMINISTRATEUR') || roleString.includes('ADMIN')) {
      return UserRole.ADMINISTRATEUR;
    }
    if (roleString.includes('EMPLOYEE')) {
      return UserRole.EMPLOYEE;
    }
    if (roleString.includes('TECHNICIEN_SUPPORT')) {
      return UserRole.TECHNICIEN_SUPPORT;
    }
    if (roleString.includes('MANAGER_IT')) {
      return UserRole.MANAGER_IT;
    }
    return null;
  }

  isEmployee(): boolean {
    return this.getCurrentUserRole() === UserRole.EMPLOYEE;
  }

  isManager(): boolean {
    return this.getCurrentUserRole() === UserRole.MANAGER_IT;
  }

  isTechnician(): boolean {
    return this.getCurrentUserRole() === UserRole.TECHNICIEN_SUPPORT;
  }

  isAdmin(): boolean {
    return this.getCurrentUserRole() === UserRole.ADMINISTRATEUR;
  }

  canEditTicket(): boolean {
    return this.isEmployee();
  }

  canDeleteTicket(): boolean {
    return this.isEmployee();
  }

  canAssignTicket(): boolean {
    return this.isManager() || this.isTechnician();
  }

  canViewAllDepartmentTickets(): boolean {
    return this.isManager() || this.isAdmin();
  }

  canCreateTicket(): boolean {
    return this.isEmployee() || this.isManager();
  }
}