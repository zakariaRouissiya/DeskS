import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { AdminService } from '../../services/admin';
import { AuthentificationService } from '../../services/authentification';
import { DepartmentDialog } from './department-dialog/department-dialog';
import { MatSelectModule } from '@angular/material/select';
import { UserProfileDialogComponent } from '../user-profile-dialog/user-profile-dialog';

interface Department {
  id: number;
  nom: string;
  description: string;
}

@Component({
  selector: 'app-departments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatTooltipModule,
    MatSidenavModule,
    MatListModule,
    MatSelectModule,
  ],
  templateUrl: './departments.html',
  styleUrls: ['./departments.css']
})
export class Departments implements OnInit {
  allDepartments: Department[] = [];
  departements: Department[] = [];
  searchTerm = '';
  pageSize = 5;
  pageIndex = 0;
  sortField: keyof Department | '' = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  private searchSubject = new Subject<string>();
  departementToDelete: Department | null = null;
  isSidebarCollapsed = false;

  constructor(
    private adminService: AdminService,
    private authService: AuthentificationService,
    private router: Router,
    private dialog: MatDialog
  ) {
    this.searchTerm = '';
    this.pageSize = 5;
    this.pageIndex = 0;
    this.sortField = '';
    this.sortDirection = 'asc';
    this.allDepartments = [];
    this.departements = [];
  }

  ngOnInit(): void {
    this.loadDepartments();
    this.searchSubject.pipe(debounceTime(300)).subscribe(() => {
      this.pageIndex = 0;
      this.applyFilter();
    });
  }

  loadDepartments(): void {
    console.log('Chargement des départements...');
    
    this.adminService.getDepartments().subscribe({
      next: (response) => {
        console.log('Réponse départements:', response);
        
        if (response && response.departements && Array.isArray(response.departements)) {
          this.allDepartments = response.departements;
        } else if (response && Array.isArray(response)) {
          this.allDepartments = response;
        } else {
          console.warn('Aucun département trouvé dans la réponse');
          this.allDepartments = [];
        }
        
        console.log('Départements chargés:', this.allDepartments);
        this.applyFilter();
      },
      error: (err) => {
        console.error('Erreur lors du chargement des départements:', err);
        this.allDepartments = [];
        this.departements = [];
      }
    });
  }

  applyFilter(): void {
    const term = this.searchTerm ? this.searchTerm.toLowerCase() : '';
    
    console.log('allDepartments avant filtrage:', this.allDepartments);
    
    let filtered = this.allDepartments;
    if (term) {
      filtered = this.allDepartments.filter(dep => {
        const nomMatch = dep.nom ? dep.nom.toLowerCase().includes(term) : false;
        const descMatch = dep.description ? dep.description.toLowerCase().includes(term) : false;
        return nomMatch || descMatch;
      });
    }
    
    console.log('Départements après filtrage:', filtered);
    
    const start = this.pageIndex * this.pageSize;
    const end = start + this.pageSize;
    
    this.departements = filtered.slice(start, Math.min(end, filtered.length));
    console.log('Départements paginés:', this.departements);
  }

  onSearchInput(term: string): void {
    this.searchTerm = term;
    this.searchSubject.next(term);
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(DepartmentDialog, {
      width: '500px',
      data: { mode: 'create' },
      ariaDescribedBy: 'dialog-description',
      autoFocus: true,
      restoreFocus: true,
      panelClass: 'accessible-dialog'
    });
    
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadDepartments();
      }
    });
  }

  openEditDialog(department: Department): void {
    const dialogRef = this.dialog.open(DepartmentDialog, {
      width: '500px',
      data: { mode: 'edit', department },
      ariaDescribedBy: 'dialog-description',
      autoFocus: true,
      restoreFocus: true,
      panelClass: 'accessible-dialog'
    });
    
    dialogRef.afterClosed().subscribe(result => {
      if (result) this.loadDepartments();
    });
  }

  confirmDelete(department: Department): void {
    this.departementToDelete = department;
  }

  cancelDelete(): void {
    this.departementToDelete = null;
  }

  deleteDepartementConfirmed(): void {
    if (!this.departementToDelete) return;
    this.adminService.deleteDepartment(this.departementToDelete.id).subscribe({
      next: () => {
        this.loadDepartments();
        this.departementToDelete = null;
      },
      error: (err) => console.error('Erreur suppression département', err)
    });
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  sortBy(field: keyof Department): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.applyFilter();
  }

  refreshData(): void {
    console.log('Rafraîchissement des données');
    this.loadDepartments();
  }

  exportExcel(): void {
    const headers = ['Nom', 'Description'];
    const rows = this.departements.map(dep => [dep.nom, dep.description]);

    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });

    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.href = url;
    link.download = 'departements.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  loadDepartements(): void {
    this.applyFilter();
  }

  previousPage(): void {
    if (this.pageIndex > 0) {
      this.pageIndex--;
      this.applyFilter();
    }
  }

  nextPage(): void {
    if (this.hasNextPage()) {
      this.pageIndex++;
      this.applyFilter();
    }
  }

  hasNextPage(): boolean {
    const totalDepartments = this.allDepartments.length;
    const currentShowing = (this.pageIndex + 1) * this.pageSize;
    return currentShowing < totalDepartments;
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
  }
}
