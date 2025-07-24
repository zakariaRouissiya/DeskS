import { Component, OnInit } from '@angular/core';
import { ReportingService } from '../../services/reporting.service';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { AuthentificationService } from '../../services/authentification';
import { Router } from '@angular/router';
import { UserProfileDialogComponent } from '../user-profile-dialog/user-profile-dialog';
import { MatDialog } from '@angular/material/dialog';

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
    MatCardModule,
    MatIconModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    NgxChartsModule,
    MatSidenavModule,
    MatListModule, 
  ],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css']
})
export class AdminDashboardComponent implements OnInit {
  loading = true;
   refreshing = false;
   errorMsg: string | null = null;
   selectedTimeframe = '7d';
 
   globalStats: any = {};
   ticketsByStatus: any[] = [];
   ticketVolumeData: any[] = [];
   ticketsByPriority: any[] = [];
   ticketsByDepartment: any[] = [];
   usersByRole: any[] = [];
   slaPerformance: any[] = [];
   technicianWorkload: any[] = [];
   avgResolutionTime = 0;
 
   colorScheme = { domain: ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4'] };
 
   constructor(
     private reporting: ReportingService,
     private authService: AuthentificationService,
     private router: Router,
     private dialog: MatDialog
   ) {}
 
   ngOnInit(): void {
     this.fetchAllData();
   }
 
     
   logout() {
     this.authService.logout();
     this.router.navigate(['/login']);
   }
 
   navigateTo(route: string) {
     this.router.navigate([route]);
   }
 
   onManageUsers() {
     this.router.navigate(['/admin/users']);
   }
   onManageDepartments() {
     this.router.navigate(['/admin/departments']);
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
       this.fetchAllData();
     });
   }
   
   fetchAllData() {
     this.loading = true;
     this.errorMsg = null;
     Promise.all([
       this.reporting.getGlobalStats().toPromise(),
       this.reporting.getTicketsByStatus().toPromise(),
       this.reporting.getTicketsByPriority().toPromise(),
       this.reporting.getTicketsByDepartment().toPromise(),
       this.reporting.getUsersByRole().toPromise(),
       this.reporting.getTicketVolumeByDay().toPromise(),
       this.reporting.getAverageResolutionTime().toPromise(),
       this.reporting.getTicketVolumeByTechnician().toPromise()
     ]).then(([
       globalStats,
       statusData,
       priorityData,
       departmentData,
       roleData,
       volumeData,
       resolutionData,
       technicianData
     ]) => {
       this.globalStats = globalStats || {};
       this.ticketsByStatus = this.mapToChartData(statusData);
       this.ticketsByPriority = this.mapToChartData(priorityData);
       this.ticketsByDepartment = this.mapToChartData(departmentData);
       this.usersByRole = this.mapToChartData(roleData);
       this.ticketVolumeData = this.mapVolumeData(volumeData);
       this.avgResolutionTime = Math.round(resolutionData?.averageResolutionTimeMinutes || 0);
     }).catch(err => {
       this.errorMsg = err.message || 'Erreur lors du chargement des données';
     }).finally(() => {
       this.loading = false;
       this.refreshing = false;
     });
   }
 
   refresh() {
     this.refreshing = true;
     this.fetchAllData();
   }
 
   setTimeframe(tf: string) {
     this.selectedTimeframe = tf;
     this.fetchAllData();
   }
 
   mapToChartData(obj: any): any[] {
     if (!obj) return [];
     return Object.keys(obj).map(key => ({
       name: key,
       value: obj[key]
     }));
   }
 
   mapVolumeData(obj: any): any[] {
     if (!obj) return [];
 
     const series = Object.entries(obj).map(([date, count]: any) => ({
       name: date,
       value: Number(count) || 0
     }));
 
     return [
       {
         name: 'Volume de tickets',
         series: series
       }
     ];
   }
 
   calculateTrend(current: number, previous: number = 0): number {
     if (!previous) return 0;
     return Math.round(((current - previous) / previous) * 100);
   }
 
   trackByTechnicianId(index: number, tech: any) {
     return tech.id;
   }
 
   getActivityIcon(type: string): string {
     const icons: { [key: string]: string } = {
       'ticket': 'confirmation_number',
       'user': 'person_add',
       'system': 'settings',
       'alert': 'warning'
     };
     return icons[type] || 'info';
   }
}