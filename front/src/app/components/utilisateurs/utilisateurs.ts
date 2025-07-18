import { Component, OnInit } from "@angular/core"
import { CommonModule } from "@angular/common"
import { MatTableModule } from "@angular/material/table"
import { MatFormFieldModule } from "@angular/material/form-field"
import { MatInputModule } from "@angular/material/input"
import { MatSelectModule } from "@angular/material/select"
import { MatButtonModule } from "@angular/material/button"
import { MatIconModule } from "@angular/material/icon"
import { FormsModule } from "@angular/forms"
import { AdminService } from "../../services/admin"
import { MatDialog } from "@angular/material/dialog"
import { MatDialogRef } from "@angular/material/dialog";
import { MatSidenavModule } from "@angular/material/sidenav"
import { MatListModule } from "@angular/material/list"
import { Router } from "@angular/router"
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';
import { debounceTime, Subject } from "rxjs"
import { UserDialogComponent } from "./user-dialog/user-dialog"
import { MatDialogModule } from '@angular/material/dialog'
import { MatCardModule } from '@angular/material/card'
import { AuthentificationService } from '../../services/authentification'; // adapte le chemin si besoin
import { UserProfileDialogComponent } from "../user-profile-dialog/user-profile-dialog"
import { MatTooltipModule } from '@angular/material/tooltip';
interface User {
  id: number
  nom: string
  prenom: string
  email: string
  role: string
  department?: { id: number, nom: string }
  // Ajouter aussi departement pour compatibilité
  departement?: { id: number, nom: string }
}

@Component({
  selector: "app-utilisateurs",
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    FormsModule,
    MatSidenavModule,
    MatListModule,
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatCardModule,
    MatTooltipModule
  ],
  templateUrl: "./utilisateurs.html",
  styleUrls: ["./utilisateurs.css"],
})
export class Utilisateurs implements OnInit {
  utilisateurs: User[] = []
  allUtilisateurs: User[] = []

  // Pagination
  pageSize = 5
  pageIndex = 0

  // Filtres et recherche
  searchTerm = ""
  roleFilter = ""
  departmentFilter = ""
  departmentList: { id: number, nom: string }[] = []

  // Tri
  sortField: string = ''
  sortDirection: 'asc' | 'desc' = 'asc'

  // Pour la recherche dynamique
  private searchSubject = new Subject<string>()

  // Pour la confirmation de suppression
  userToDelete: User | null = null;

  isSidebarCollapsed = false;

  constructor(
    private readonly adminService: AdminService,
    private readonly router: Router,
    private dialog: MatDialog,
    private authService: AuthentificationService // ajoute ce service
  ) {}

  ngOnInit() {
    this.loadDepartments()
    this.loadUtilisateurs()
    this.searchSubject.pipe(debounceTime(200)).subscribe(term => {
      this.searchTerm = term
      this.applyFilter()
    })
  }

  loadDepartments() {
    this.adminService.getDepartments().subscribe({
      next: (res) => {
        console.log('Départements reçus:', res); // Affiche toute la réponse
        this.departmentList = res.departments || res.departements || res || [];
      },
      error: () => {
        this.departmentList = [];
      }
    });
  }

  loadUtilisateurs() {
    // Charge tous les utilisateurs pour la recherche côté client
    this.adminService.getUsers({}).subscribe({
      next: (res) => {
        this.allUtilisateurs = res.utilisateurs || []
        this.applyFilter()
      },
      error: () => {
        this.allUtilisateurs = []
        this.applyFilter()
      },
    })
  }
  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  onSearchInput(term: string) {
    this.searchSubject.next(term)
  }

  applyFilter() {
    let filtered = [...this.allUtilisateurs]

    // Recherche sur tous les champs
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase()
      filtered = filtered.filter(u =>
        (u.nom && u.nom.toLowerCase().includes(term)) ||
        (u.prenom && u.prenom.toLowerCase().includes(term)) ||
        (u.email && u.email.toLowerCase().includes(term)) ||
        (u.role && this.getRoleDisplay(u.role).toLowerCase().includes(term)) ||
        (u.department?.nom && u.department.nom.toLowerCase().includes(term))
      )
    }

    // Filtres
    if (this.roleFilter) {
      filtered = filtered.filter(u => u.role === this.roleFilter)
    }
    if (this.departmentFilter) {
      // Compare en string pour éviter les soucis de typage
      filtered = filtered.filter(u => String(u.department?.id) === String(this.departmentFilter))
    }

    // Tri
    if (this.sortField) {
      filtered.sort((a, b) => {
        let aValue = this.getSortValue(a, this.sortField)
        let bValue = this.getSortValue(b, this.sortField)
        if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1
        if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1
        return 0
      })
    }

    // Pagination
    const start = this.pageIndex * this.pageSize
    const end = start + this.pageSize
    this.utilisateurs = filtered.slice(start, end)
  }

  getSortValue(user: User, field: string) {
    if (field === 'department') return user.department?.nom?.toLowerCase() || ''
    if (field === 'role') return this.getRoleDisplay(user.role).toLowerCase()
    return (user[field as keyof User] as string)?.toLowerCase?.() || ''
  }

  sortBy(field: string) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc'
    } else {
      this.sortField = field
      this.sortDirection = 'asc'
    }
    this.applyFilter()
  }

  // Pagination avancée
  previousPage() {
    if (this.pageIndex > 0) {
      this.pageIndex--
      this.applyFilter()
    }
  }
  nextPage() {
    this.pageIndex++
    this.applyFilter()
  }
  hasNextPage(): boolean {
    const filtered = this.getFilteredCount()
    return (this.pageIndex + 1) * this.pageSize < filtered
  }
  getFilteredCount(): number {
    let filtered = [...this.allUtilisateurs]
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase()
      filtered = filtered.filter(u =>
        (u.nom && u.nom.toLowerCase().includes(term)) ||
        (u.prenom && u.prenom.toLowerCase().includes(term)) ||
        (u.email && u.email.toLowerCase().includes(term)) ||
        (u.role && this.getRoleDisplay(u.role).toLowerCase().includes(term)) ||
        (u.department?.nom && u.department.nom.toLowerCase().includes(term))
      )
    }
    if (this.roleFilter) {
      filtered = filtered.filter(u => u.role === this.roleFilter)
    }
    if (this.departmentFilter) {
      filtered = filtered.filter(u => String(u.department?.id) === String(this.departmentFilter))
    }
    return filtered.length
  }

  refreshData() {
    this.loadUtilisateurs()
  }

  getRoleClass(role: string): string {
    const roleClasses: { [key: string]: string } = {
      ADMINISTRATEUR: "role-admin",
      TECHNICIEN_SUPPORT: "role-tech",
      EMPLOYEE: "role-employee",
      MANAGER_IT: "role-manager",
    }
    return roleClasses[role] || "role-badge"
  }

  getRoleDisplay(role: string): string {
    const roleDisplays: { [key: string]: string } = {
      ADMINISTRATEUR: "Administrateur",
      TECHNICIEN_SUPPORT: "Technicien",
      EMPLOYEE: "Employé",
      MANAGER_IT: "Manager IT",
    }
    return roleDisplays[role] || role
  }

  openCreateDialog() {
    const dialogRef = this.dialog.open(UserDialogComponent, {
      width: "600px",
      data: { mode: "create" },
    })

    dialogRef.afterClosed().subscribe((result) => {
      if (result) this.loadUtilisateurs()
    })
  }

  openEditDialog(user: User) {
    const dialogRef = this.dialog.open(UserDialogComponent, {
      width: "600px",
      data: { mode: "edit", user },
    })

    dialogRef.afterClosed().subscribe((result) => {
      if (result) this.loadUtilisateurs()
    })
  }

  deleteUser(user: User) {
      this.adminService.deleteUser(user.id).subscribe(() => {
        this.loadUtilisateurs()
    })
  }

  confirmDelete(user: User) {
    this.userToDelete = user;
  }

  cancelDelete() {
    this.userToDelete = null;
  }

  deleteUserConfirmed() {
    if (this.userToDelete) {
      this.deleteUser(this.userToDelete);
      this.userToDelete = null;
    }
  }

  navigateTo(route: string) {
    this.router.navigate([route])
  }

  onManageDepartments() {
    this.router.navigate(["/admin/departments"])
  }

  exportExcel() {
    // Récupérer tous les utilisateurs filtrés (pas seulement ceux affichés)
    let filteredUsers = this.getFilteredUsers();
    
    const data = filteredUsers.map(u => ({
      Nom: u.nom,
      Prénom: u.prenom,
      Email: u.email,
      Rôle: this.getRoleDisplay(u.role),
      Département: u.department?.nom || '-'
    }));

    const worksheet = XLSX.utils.json_to_sheet(data);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Utilisateurs');

    const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });
    const blob = new Blob([excelBuffer], { type: 'application/octet-stream' });
    
    // Nom du fichier avec info sur le filtrage
    const fileName = this.getExcelFileName();
    saveAs(blob, fileName);
  }

  // Nouvelle méthode pour obtenir tous les utilisateurs filtrés
  private getFilteredUsers(): User[] {
    let filtered = [...this.allUtilisateurs];

    // Appliquer la recherche
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(u =>
        (u.nom && u.nom.toLowerCase().includes(term)) ||
        (u.prenom && u.prenom.toLowerCase().includes(term)) ||
        (u.email && u.email.toLowerCase().includes(term)) ||
        (u.role && this.getRoleDisplay(u.role).toLowerCase().includes(term)) ||
        (u.department?.nom && u.department.nom.toLowerCase().includes(term))
      );
    }

    // Appliquer les filtres
    if (this.roleFilter) {
      filtered = filtered.filter(u => u.role === this.roleFilter);
    }
    if (this.departmentFilter) {
      filtered = filtered.filter(u => String(u.department?.id) === String(this.departmentFilter));
    }

    // Appliquer le tri
    if (this.sortField) {
      filtered.sort((a, b) => {
        let aValue = this.getSortValue(a, this.sortField);
        let bValue = this.getSortValue(b, this.sortField);
        if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1;
        if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1;
        return 0;
      });
    }

    return filtered;
  }

  // Méthode pour générer un nom de fichier dynamique
  private getExcelFileName(): string {
    const now = new Date();
    const dateStr = now.toISOString().split('T')[0]; // Format YYYY-MM-DD
    const timeStr = now.toTimeString().split(' ')[0].replace(/:/g, '-'); // Format HH-MM-SS
    
    let fileName = `utilisateurs_${dateStr}_${timeStr}`;
    
    // Ajouter des informations sur les filtres appliqués
    const filterInfo = [];
    
    if (this.searchTerm) {
      filterInfo.push(`recherche-${this.searchTerm.replace(/\s+/g, '-')}`);
    }
    
    if (this.roleFilter) {
      const roleDisplay = this.getRoleDisplay(this.roleFilter).replace(/\s+/g, '-');
      filterInfo.push(`role-${roleDisplay}`);
    }
    
    if (this.departmentFilter) {
      const dept = this.departmentList.find(d => String(d.id) === String(this.departmentFilter));
      if (dept) {
        filterInfo.push(`dept-${dept.nom.replace(/\s+/g, '-')}`);
      }
    }
    
    if (filterInfo.length > 0) {
      fileName += `_[${filterInfo.join('_')}]`;
    }
    
    return `${fileName}.xlsx`;
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  openUserProfile(): void {
      const dialogRef = this.dialog.open(UserProfileDialogComponent, {
        width: '750px',
        height: 'auto', // Laisser la hauteur s'adapter au contenu
        panelClass: 'profile-dialog-container',
        disableClose: false,
        autoFocus: true,
        restoreFocus: true, // Important pour l'accessibilité
        ariaDescribedBy: 'profile-dialog-description'
      });
    }

  getExportTooltip(): string {
    const filteredCount = this.getFilteredCount();
    const totalCount = this.allUtilisateurs.length;
    
    if (filteredCount === totalCount) {
      return `Exporter tous les utilisateurs (${totalCount})`;
    } else {
      return `Exporter les utilisateurs filtrés (${filteredCount} sur ${totalCount})`;
    }
  }
}

