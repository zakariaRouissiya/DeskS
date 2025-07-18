import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthentificationService } from '../../services/authentification';
import { AdminService } from '../../services/admin';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { RouterModule } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { UserProfileDialogComponent } from '../user-profile-dialog/user-profile-dialog';
import { MatDialogModule } from '@angular/material/dialog';

interface DashboardStats {
  totalUsers: number;
  activeTickets: number;
  openIncidents: number;
  closedTickets: number;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    RouterModule,
    MatDialogModule
  ],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css']
})
export class AdminDashboardComponent implements OnInit {
  stats: DashboardStats = { totalUsers: 0, activeTickets: 0, openIncidents: 0, closedTickets: 0 };
  utilisateurs: any[] = [];
  displayedColumns: string[] = ['nom', 'prenom', 'email', 'role', 'department'];
  loading = true;
  error = '';
  isSidebarCollapsed = false; 

  constructor(
    private authService: AuthentificationService,
    private adminService: AdminService,
    private router: Router,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.loadDashboardStats();
    this.loadUtilisateurs();
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  loadDashboardStats() {
    this.loading = true;
    this.adminService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats = {
          totalUsers: data.totalUsers ?? 0,
          activeTickets: data.activeTickets ?? 0,
          openIncidents: data.openIncidents ?? 0,
          closedTickets: data.closedTickets ?? 0 
        };
        this.loading = false;
      },
      error: () => {
        this.error = "Erreur lors du chargement des statistiques";
        this.loading = false;
      }
    });
  }

  loadUtilisateurs() {
    this.adminService.getUsers({ size: 5 }).subscribe({
      next: (res) => {
        this.utilisateurs = res.utilisateurs || [];
      },
      error: () => {
        this.utilisateurs = [];
      }
    });
  }

  onManageUsers() {
    this.router.navigate(['/admin/users']);
  }
  onManageDepartments() {
    this.router.navigate(['/admin/departments']);
  }
  navigateTo(route: string) {
    this.router.navigate([route]);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  openUserProfile(): void {
    const dialogRef = this.dialog.open(UserProfileDialogComponent, {
      width: '750px',
      height: 'auto', 
      panelClass: 'profile-dialog-container',
      disableClose: false,
      autoFocus: true,
      restoreFocus: true, 
      ariaDescribedBy: 'profile-dialog-description'
    });

    dialogRef.afterClosed().subscribe(() => {
      this.loadDashboardStats();
    });
  }
}