import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthentificationService } from '../services/authentification';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  
  constructor(
    private authService: AuthentificationService,
    private router: Router
  ) {}

  canActivate(): boolean {
    console.log('AdminGuard: Vérification des permissions admin');
    
    if (!this.authService.isLoggedIn()) {
      console.log('AdminGuard: Utilisateur non connecté');
      this.router.navigate(['/login']);
      return false;
    }

    const user = this.authService.getCurrentUser();
    const isAdmin = this.authService.isAdmin();
    
    console.log('AdminGuard: Utilisateur:', user);
    console.log('AdminGuard: isAdmin:', isAdmin);
    
    if (!isAdmin) {
      console.log('AdminGuard: Accès refusé - pas administrateur');
      this.router.navigate(['/tickets']);
      return false;
    }

    console.log('AdminGuard: Accès autorisé');
    return true;
  }
}