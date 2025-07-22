import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogActions, MatDialogContent } from '@angular/material/dialog';
import { TicketFormDialogComponent } from './ticket-form-dialog/ticket-form-dialog';
import { TicketAssignmentDialog } from './ticket-assignment-dialog/ticket-assignment-dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { Router } from '@angular/router';
import { AuthentificationService } from '../../services/authentification';
import { UserProfileDialogComponent } from '../user-profile-dialog/user-profile-dialog';
import { TicketDetailsDialog } from './ticket-details-dialog/ticket-details-dialog';
import { DelegationRequestDialog } from '../delegation/delegation-request-dialog/delegation-request-dialog';
import { DelegationManagementDialog } from '../delegation/delegation-management-dialog/delegation-management-dialog';
import { Ticket, TicketService, TicketResponseDTO } from '../../services/ticket.service';
import { UserService } from '../../services/user.service';
import { RoleService } from '../../services/role.service';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { TraitementTicketDialog } from './traitement-ticket-dialog/traitement-ticket-dialog';

@Component({
  standalone: true,
  selector: 'app-ticket',
  templateUrl: './ticket.html',
  styleUrls: ['./ticket.css'],
  imports: [
    CommonModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatTabsModule,
    MatBadgeModule,
    MatDialogContent,
    MatDialogActions,
    MatSelectModule,
    FormsModule
  ]
})
export class TicketComponent implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  
  dataSource = new MatTableDataSource<Ticket>([]);
  displayedColumns: string[] = [];
  loading = false;
  currentUser: any = null;
  ticketToDelete: Ticket | null = null;
  departmentStatistics: any = null;

  selectedStatus: string = '';
  selectedType: string = '';
  searchText: string = '';

  // Pour stocker tous les tickets chargés
  allTickets: Ticket[] = [];

  constructor(
    private ticketService: TicketService,
    private userService: UserService,
    private roleService: RoleService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private router: Router,
    private authService: AuthentificationService
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadCurrentUser(): void {
    this.loading = true;


    const userData = this.authService.getCurrentUser();

    this.userService.getUserProfile().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.authService.setCurrentUser(user);
        this.setupDisplayColumns();
        this.loadTickets();
      },
      error: (error) => {
        console.error('Erreur lors du chargement du profil:', error);
        this.snackBar.open('Erreur lors du chargement du profil', 'Fermer', { duration: 3000 });
        this.loading = false;
        this.router.navigate(['/login']);
      }
    });
  }

  setupDisplayColumns(): void {
    if (this.roleService.isEmployee()) {
      this.displayedColumns = ['titre', 'description', 'priorite', 'statut', 'dateCreation', 'actions'];
    } else if (this.roleService.isManager()) {
      this.displayedColumns = ['titre', 'utilisateur', 'priorite', 'statut', 'assignation', 'dateCreation', 'actions'];
    } else if (this.roleService.isTechnician()) {
      this.displayedColumns = ['titre', 'utilisateur', 'priorite', 'statut', 'dateCreation', 'actions'];
    } else {
      this.displayedColumns = ['titre', 'description', 'priorite', 'statut', 'dateCreation', 'actions'];
    }
  }
  
  loadTickets(): void {
    if (!this.currentUser?.id) {
      console.error('Aucun ID utilisateur trouvé');
      this.loading = false;
      return;
    }

    this.loading = true;

    if (this.roleService.isEmployee()) {
      this.loadEmployeeTickets();
    } else if (this.roleService.isManager()) {
      this.loadManagerTickets();
    } else if (this.roleService.isTechnician()) {
      this.loadTechnicianTickets();
    } else {
      this.loadEmployeeTickets();
    }
  }

  loadEmployeeTickets(): void {
    this.ticketService.getMyTickets(this.currentUser.id).subscribe({
      next: (dtos: TicketResponseDTO[]) => {
        const tickets = dtos.map(dto => this.ticketService.convertDTOToTicket(dto));
        this.allTickets = tickets;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des tickets:', error);
        this.snackBar.open('Erreur lors du chargement des tickets', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadManagerTickets(): void {
    if (!this.currentUser.department?.id) {
      this.snackBar.open('Aucun département assigné', 'Fermer', { duration: 3000 });
      this.loading = false;
      return;
    }

    this.ticketService.getDepartmentTickets(this.currentUser.department.id).subscribe({
      next: (dtos: TicketResponseDTO[]) => {
        const tickets = dtos.map(dto => this.ticketService.convertDTOToTicket(dto));
        this.allTickets = tickets;
        this.loadDepartmentStatistics();
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Erreur lors du chargement des tickets du département', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadTechnicianTickets(): void {
    this.ticketService.getAssignedTickets(this.currentUser.id).subscribe({
      next: (dtos: TicketResponseDTO[]) => {
        const tickets = dtos.map(dto => this.ticketService.convertDTOToTicket(dto));
        this.allTickets = tickets;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des tickets assignés:', error);
        this.snackBar.open('Erreur lors du chargement des tickets assignés', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadDepartmentStatistics(): void {
    if (this.roleService.isManager() && this.currentUser.department?.id) {
      this.ticketService.getDepartmentStatistics(this.currentUser.department.id).subscribe({
        next: (stats) => {
          this.departmentStatistics = stats;
        },
        error: () => {
          console.error('Erreur lors du chargement des statistiques');
        }
      });
    }
  }

  autoDispatchAllTickets(): void {
    if (!this.roleService.isManager() || !this.currentUser.department?.id) return;

    const unassignedTickets = this.dataSource.data.filter(ticket => 
      !ticket.assignedTo && ticket.statut === 'OUVERT'
    );

    if (unassignedTickets.length === 0) {
      this.snackBar.open('Aucun ticket non assigné à dispatcher', 'Fermer', { duration: 3000 });
      return;
    }

    this.ticketService.autoDispatchTickets(this.currentUser.department.id).subscribe({
      next: (result) => {
        this.snackBar.open(`${result.assignedCount} tickets assignés automatiquement`, 'Fermer', { duration: 3000 });
        this.loadTickets();
      },
      error: () => {
        this.snackBar.open('Erreur lors de la répartition automatique', 'Fermer', { duration: 3000 });
      }
    });
  }

  openAssignmentDialog(ticket: Ticket): void {
    if (!this.roleService.canAssignTicket() || !this.currentUser.department?.id) return;

    const dialogRef = this.dialog.open(TicketAssignmentDialog, {
      width: '700px',
      data: { 
        ticket: ticket,
        departmentId: this.currentUser.department.id
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTickets();
      }
    });
  }

  openDelegationManagement(): void {
    if (!this.isManager) return;

    const dialogRef = this.dialog.open(DelegationManagementDialog, {
      width: '1000px',
      maxWidth: '95vw',
      height: '80vh',
      panelClass: 'delegation-management-dialog-container'
    });

    dialogRef.afterClosed().subscribe(result => {
      this.loadTickets();
    });
  }

  openDelegationDialog(ticket: any): void {
    const currentUser = {
      id: this.currentUser?.id,
      prenom: this.currentUser?.prenom || this.currentUser?.firstName,
      nom: this.currentUser?.nom || this.currentUser?.lastName || this.currentUser?.name,
      email: this.currentUser?.email,
      department: this.currentUser?.department
    };

    const dialogRef = this.dialog.open(DelegationRequestDialog, {
      width: '800px',
      maxWidth: '90vw',
      data: {
        ticket: ticket,
        currentUser: currentUser
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTickets();
      }
    });
  }

  refreshTickets(): void {
    this.loadTickets();
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  applyFilters(): void {
    let filtered = [...this.allTickets];

    if (this.selectedStatus) {
      filtered = filtered.filter(ticket => ticket.statut === this.selectedStatus);
    }
    if (this.selectedType) {
      filtered = filtered.filter(ticket => ticket.type === this.selectedType);
    }
    if (this.searchText && this.searchText.trim() !== '') {
      const search = this.searchText.trim().toLowerCase();
      filtered = filtered.filter(ticket =>
        (ticket.titre && ticket.titre.toLowerCase().includes(search)) ||
        (ticket.description && ticket.description.toLowerCase().includes(search))
      );
    }
    this.dataSource.data = filtered;
  }

  getTicketsByStatus(status: string): Ticket[] {
    return this.dataSource.data.filter(ticket => ticket.statut === status);
  }

  getTypeIcon(type: string): string {
    const iconMap: { [key: string]: string } = {
      'Matériel': 'computer',
      'Logiciel': 'apps',
      'Réseau': 'wifi',
      'Compte': 'account_circle',
      'Autre': 'help'
    };
    return iconMap[type] || 'help';
  }

  getPriorityClass(priority: string): string {
    return `priority-${priority.toLowerCase()}`;
  }

  getPriorityIcon(priority: string): string {
    const iconMap: { [key: string]: string } = {
      'FAIBLE': 'keyboard_arrow_down',
      'MOYENNE': 'remove',
      'HAUTE': 'keyboard_arrow_up',
      'CRITIQUE': 'warning'
    };
    return iconMap[priority] || 'remove';
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase().replace('_', '-')}`;
  }

  getStatusIcon(status: string): string {
    const iconMap: { [key: string]: string } = {
      'OUVERT': 'hourglass_empty',
      'EN_COURS': 'sync',
      'RESOLU': 'check_circle',
      'FERME': 'lock'
    };
    return iconMap[status] || 'help';
  }

  getStatusLabel(status: string): string {
    const labelMap: { [key: string]: string } = {
      'OUVERT': 'Ouvert',
      'EN_COURS': 'En cours',
      'RESOLU': 'Résolu',
      'FERME': 'Fermé'
    };
    return labelMap[status] || status;
  }

  getAssignmentStatus(ticket: Ticket): string {
    if (ticket.assignedTo) {
      return `${ticket.assignedTo.prenom} ${ticket.assignedTo.nom}`;
    }
    return 'Non assigné';
  }

  getAssignmentIcon(ticket: Ticket): string {
    return ticket.assignedTo ? 'person' : 'person_off';
  }

  getAssignmentClass(ticket: Ticket): string {
    return ticket.assignedTo ? 'assigned' : 'unassigned';
  }

  getUserFullName(user: any): string {
    if (!user) return 'Utilisateur inconnu';
    return `${user.prenom || ''} ${user.nom || ''}`.trim() || 'Nom non disponible';
  }
  
  createTicket(): void {
    if (!this.roleService.canCreateTicket()) return;

    const dialogRef = this.dialog.open(TicketFormDialogComponent, {
      width: '600px',
      data: {
        mode: 'create',
        currentUser: this.currentUser
      }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTickets();
      }
    });
  }
  
  updateTicket(ticket: Ticket): void {
    if (!this.roleService.canEditTicket()) return;

    const dialogRef = this.dialog.open(TicketFormDialogComponent, {
      width: '600px',
      data: {
        mode: 'edit',
        currentUser: this.currentUser,
        ticket
      }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTickets();
      }
    });
  }

  confirmDeleteTicket(ticket: Ticket): void {
    if (!this.roleService.canDeleteTicket()) return;
    this.ticketToDelete = ticket;
  }

  cancelDeleteTicket(): void {
    this.ticketToDelete = null;
  }

  deleteTicketConfirmed(): void {
    if (!this.ticketToDelete?.id || !this.roleService.canDeleteTicket()) return;
    
    this.ticketService.deleteTicketByEmployee(this.ticketToDelete.id, this.currentUser.id).subscribe({
      next: () => {
        this.snackBar.open('Ticket supprimé avec succès', 'Fermer', { duration: 3000 });
        this.loadTickets();
        this.ticketToDelete = null;
      },
      error: () => {
        this.snackBar.open('Erreur lors de la suppression', 'Fermer', { duration: 3000 });
        this.ticketToDelete = null;
      }
    });
  }

  deleteTicket(ticket: Ticket): void {
    this.confirmDeleteTicket(ticket);
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

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  viewTicketDetails(ticket: Ticket): void {
    if (!ticket.id) return;

    this.ticketService.getTicketDetails(ticket.id).subscribe({
      next: (dto: TicketResponseDTO) => {
        const fullTicket = this.ticketService.convertDTOToTicket(dto);
        const dialogRef = this.dialog.open(TicketDetailsDialog, {
          data: { 
            ticket: fullTicket,
            canAssign: this.roleService.canAssignTicket(),
            departmentId: this.currentUser.department?.id
          },
          panelClass: 'fullscreen-dialog'
        });

        dialogRef.afterClosed().subscribe(result => {
          if (result === 'statut-updated' || result === 'assigned') {
            this.loadTickets();
          }
        });
      },
      error: () => {
        this.snackBar.open('Erreur lors du chargement des détails', 'Fermer', { duration: 3000 });
      }
    });
  }

  trackByCommentId(index: number, comment: any): number {
    return comment.id;
  }

  get isEmployee(): boolean {
    return this.roleService.isEmployee();
  }

  get isManager(): boolean {
    return this.roleService.isManager();
  }

  get isTechnician(): boolean {
    return this.roleService.isTechnician();
  }

  get canCreateTicket(): boolean {
    return this.roleService.isEmployee();
  }

  get canAssignTicket(): boolean {
    return this.roleService.canAssignTicket();
  }

  get headerTitle(): string {
    if (this.roleService.isEmployee()) return 'Mes Tickets';
    if (this.roleService.isManager()) return 'Gestion des Tickets';
    if (this.roleService.isTechnician()) return 'Tickets Assignés';
    return 'Tickets';
  }

  get headerSubtitle(): string {
    if (this.roleService.isEmployee()) return 'Gérez vos demandes de support technique';
    if (this.roleService.isManager()) return 'Supervisez les tickets de votre département';
    if (this.roleService.isTechnician()) return 'Traitez les tickets qui vous sont assignés';
    return 'Système de gestion des tickets';
  }

  get emptyStateMessage(): string {
    if (this.roleService.isEmployee()) return 'Aucun ticket créé. Commencez par créer votre première demande.';
    if (this.roleService.isManager()) return 'Aucun ticket dans votre département pour le moment.';
    if (this.roleService.isTechnician()) return 'Aucun ticket ne vous est assigné actuellement.';
    return 'Aucun ticket trouvé.';
  }

  get emptyStateButtonText(): string {
    if (this.roleService.isEmployee()) return 'Créer un ticket';
    if (this.roleService.isManager()) return 'Gérer les assignations';
    return '';
  }

  get unassignedTicketsCount(): number {
    if (!this.roleService.isManager()) return 0;
    return this.dataSource.data.filter(ticket => 
      !ticket.assignedTo && ticket.statut === 'OUVERT'
    ).length;
  }

  processTicket(ticket: Ticket): void {
    const dialogRef = this.dialog.open(TraitementTicketDialog, {
      width: '900px',
      maxWidth: '98vw',
      data: { ticket }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTickets(); 
      }
    });
  }

  delegateTicket(ticket: any): void {
    if (!this.isTechnician || ticket.assignedTo?.id !== this.currentUser?.id) {
      this.snackBar.open('Vous ne pouvez déléguer que vos tickets assignés', 'Fermer', { duration: 3000 });
      return;
    }

    const dialogRef = this.dialog.open(DelegationRequestDialog, {
      width: '600px',
      data: { ticket: ticket }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTickets();
      }
    });
  }
}