import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { DelegationService } from '../../../services/delegation.service';
import { UserService } from '../../../services/user.service';
import { TicketService } from '../../../services/ticket.service';
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatButtonModule } from "@angular/material/button";
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-delegation-request-dialog',
  templateUrl: './delegation-request-dialog.html',
  styleUrls: ['./delegation-request-dialog.css'],
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule, 
    MatDialogModule, 
    ReactiveFormsModule, 
    MatFormFieldModule,
    MatInputModule, 
    MatSelectModule,
    MatButtonModule
  ],
})
export class DelegationRequestDialog implements OnInit {
  delegationForm: FormGroup;
  loading = false;
  technicians: any[] = [];
  currentUserDetails: any = null;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<DelegationRequestDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { ticket: any, currentUser: any },
    private delegationService: DelegationService,
    private userService: UserService,
    private ticketService: TicketService,
    private snackBar: MatSnackBar
  ) {
    this.delegationForm = this.fb.group({
      toTechnicianId: ['', Validators.required],
      justification: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
    });
  }

  ngOnInit(): void {
    console.log('Data reçue dans le dialog:', this.data);
    console.log('Current user:', this.data.currentUser);
    
    this.loadCurrentUserDetails();
    this.loadTechnicians();
  }

  loadCurrentUserDetails(): void {
    this.currentUserDetails = this.data.currentUser;
    
    const userId = this.data.currentUser?.id;
    if (userId) {
      this.userService.getUserById(userId).subscribe({
        next: (user) => {
          this.currentUserDetails = user;
          console.log('Détails utilisateur chargés depuis API:', this.currentUserDetails);
        },
        error: (error) => {
          console.error('Erreur lors du chargement des détails utilisateur:', error);
          this.currentUserDetails = this.data.currentUser;
        }
      });
    }
  }

  loadTechnicians(): void {
    const departmentId = this.data.ticket.department?.id || 
                        this.data.ticket.user?.department?.id || 
                        this.data.currentUser?.department?.id;
    
    console.log('Chargement des techniciens pour le département:', departmentId);
    console.log('Ticket:', this.data.ticket);
    console.log('Current user:', this.data.currentUser);
    
    if (!departmentId) {
      console.error('Aucun département trouvé');
      console.error('Ticket department:', this.data.ticket.department);
      console.error('User department:', this.data.ticket.user?.department);
      console.error('Current user department:', this.data.currentUser?.department);
      
      this.snackBar.open('Impossible de déterminer le département', 'Fermer', { duration: 3000 });
      return;
    }

    this.ticketService.getDepartmentTechnicians(departmentId).subscribe({
      next: (technicians) => {
        console.log('Techniciens reçus:', technicians);
        
        this.technicians = technicians.filter(tech => tech.id !== this.data.ticket.assignedTo?.id);
        
        console.log('Techniciens après filtrage:', this.technicians);
        
        this.loadTechniciansWorkload();
      },
      error: (error) => {
        console.error('Erreur lors du chargement des techniciens:', error);
        console.error('Status:', error.status);
        console.error('Message:', error.message);
        console.error('URL:', error.url);
        
        this.snackBar.open('Erreur lors du chargement des techniciens: ' + 
          (error.error?.message || error.message), 'Fermer', { duration: 5000 });
      }
    });
  }

  loadTechniciansWorkload(): void {
    this.technicians.forEach(technician => {
      this.ticketService.getTechnicianWorkload(technician.id).subscribe({
        next: (workload) => {
          technician.workload = workload;
        },
        error: (error) => {
          console.error(`Erreur lors du chargement de la charge pour ${technician.id}:`, error);
          technician.workload = 0;
        }
      });
    });
  }

  onSubmit(): void {
    if (this.delegationForm.valid) {
      this.loading = true;
      
      const ticketId = this.data.ticket.id;
      const toTechnicianId = this.delegationForm.get('toTechnicianId')?.value;
      const justification = this.delegationForm.get('justification')?.value;

      this.delegationService.createDelegation(ticketId, toTechnicianId, justification).subscribe({
        next: (response) => {
          this.snackBar.open('Demande de délégation créée avec succès', 'Fermer', { duration: 3000 });
          this.dialogRef.close(response);
        },
        error: (error) => {
          console.error('Erreur lors de la création de la demande:', error);
          this.snackBar.open(error.error?.error || 'Erreur lors de la création de la demande', 'Fermer', { duration: 3000 });
        },
        complete: () => {
          this.loading = false;
        }
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  getPriorityIcon(priority: string): string {
    switch (priority?.toLowerCase()) {
      case 'faible': return 'low_priority';
      case 'moyenne': return 'priority_high';
      case 'haute': return 'report_problem';
      case 'critique': return 'warning';
      default: return 'help';
    }
  }

  getWorkloadClass(workload: number): string {
    if (workload === undefined || workload === null) return 'low';
    if (workload <= 2) return 'low';
    if (workload <= 5) return 'medium';
    return 'high';
  }

  getWorkloadText(workload: number): string {
    if (workload === undefined || workload === null) return 'Charge inconnue';
    if (workload <= 2) return 'Charge faible';
    if (workload <= 5) return 'Charge moyenne';
    return 'Charge élevée';
  }

  getSelectedTechnicianName(): string {
    const selectedId = this.delegationForm.get('toTechnicianId')?.value;
    const technician = this.technicians.find(t => t.id === selectedId);
    return technician ? `${technician.prenom} ${technician.nom}` : 'Non sélectionné';
  }

  getCurrentUserName(): string {
    console.log('getCurrentUserName - currentUserDetails:', this.currentUserDetails);
    console.log('getCurrentUserName - data.currentUser:', this.data.currentUser);
    
    const user = this.currentUserDetails || this.data.currentUser;
    
    if (user) {
      const prenom = user.prenom || user.firstName || '';
      const nom = user.nom || user.lastName || user.name || '';
      
      console.log('Prenom:', prenom, 'Nom:', nom);
      
      if (prenom && nom) {
        return `${prenom} ${nom}`;
      } else if (prenom) {
        return prenom;
      } else if (nom) {
        return nom;
      } else if (user.email) {
        return user.email;
      }
    }
    
    try {
      const userFromStorage = localStorage.getItem('user');
      if (userFromStorage) {
        const parsedUser = JSON.parse(userFromStorage);
        const prenom = parsedUser.prenom || parsedUser.firstName || '';
        const nom = parsedUser.nom || parsedUser.lastName || parsedUser.name || '';
        
        console.log('Depuis localStorage - Prenom:', prenom, 'Nom:', nom);
        
        if (prenom && nom) {
          return `${prenom} ${nom}`;
        } else if (parsedUser.email) {
          return parsedUser.email;
        }
      }
    } catch (e) {
      console.error('Erreur lors de la lecture du localStorage:', e);
    }
    
    try {
      const token = localStorage.getItem('token');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const prenom = payload.prenom || payload.firstName || '';
        const nom = payload.nom || payload.lastName || payload.name || '';
        
        console.log('Depuis token - Prenom:', prenom, 'Nom:', nom);
        
        if (prenom && nom) {
          return `${prenom} ${nom}`;
        } else if (payload.email) {
          return payload.email;
        }
      }
    } catch (e) {
      console.error('Erreur lors de la lecture du token:', e);
    }
    
    return 'Utilisateur inconnu';
  }

  formatDate(date: string | Date): string {
    if (!date) return 'Date inconnue';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getCurrentDate(): string {
    return new Date().toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}