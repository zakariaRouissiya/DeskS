import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TicketService, Ticket, Utilisateur } from '../../../services/ticket.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-ticket-assignment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatChipsModule,
    MatTooltipModule
  ],
  templateUrl: './ticket-assignment-dialog.html',
  styleUrls: ['./ticket-assignment-dialog.css']
})
export class TicketAssignmentDialog implements OnInit {
  assignmentForm: FormGroup;
  loading = false;
  technicians: Utilisateur[] = [];
  ticket: Ticket;
  technicianWorkloads: Map<number, number> = new Map();
  technicianWorkloadDetails: Map<number, any> = new Map(); // NOUVEAU

  constructor(
    private fb: FormBuilder,
    private ticketService: TicketService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<TicketAssignmentDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { ticket: Ticket, departmentId: number }
  ) {
    this.ticket = data.ticket;
    this.assignmentForm = this.fb.group({
      technicianId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadTechnicians();
  }

  loadTechnicians(): void {
    this.loading = true;
    this.ticketService.getDepartmentTechnicians(this.data.departmentId).subscribe({
      next: (techs) => {
        console.log('Techniciens récupérés:', techs);
        this.technicians = techs;
        this.loadTechnicianWorkloads();
      },
      error: (error) => {
        console.error('Erreur lors du chargement des techniciens:', error);
        this.snackBar.open('Erreur lors du chargement des techniciens', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  // CORRECTION: Charger les charges de travail avec les détails
  loadTechnicianWorkloads(): void {
    if (this.technicians.length === 0) {
      this.loading = false;
      return;
    }

    console.log('Chargement des charges de travail pour', this.technicians.length, 'techniciens');

    const workloadRequests = this.technicians.map(tech => 
      this.ticketService.getTechnicianWorkload(tech.id)
    );

    const workloadDetailsRequests = this.technicians.map(tech => 
      this.ticketService.getTechnicianWorkloadDetails(tech.id)
    );

    // Charger les charges simples
    forkJoin(workloadRequests).subscribe({
      next: (workloads) => {
        console.log('Charges de travail reçues:', workloads);
        this.technicians.forEach((tech, index) => {
          const workload = workloads[index] || 0;
          this.technicianWorkloads.set(tech.id, workload);
          console.log(`Technicien ${tech.prenom} ${tech.nom}: ${workload} tickets EN_COURS`);
        });
        
        // Charger les détails
        this.loadWorkloadDetails();
      },
      error: (error) => {
        console.error('Erreur lors du chargement des charges:', error);
        this.technicians.forEach(tech => {
          this.technicianWorkloads.set(tech.id, 0);
        });
        this.loading = false;
      }
    });
  }

  // NOUVEAU: Charger les détails de charge
  loadWorkloadDetails(): void {
    const workloadDetailsRequests = this.technicians.map(tech => 
      this.ticketService.getTechnicianWorkloadDetails(tech.id)
    );

    forkJoin(workloadDetailsRequests).subscribe({
      next: (details) => {
        console.log('Détails de charge reçus:', details);
        this.technicians.forEach((tech, index) => {
          this.technicianWorkloadDetails.set(tech.id, details[index] || {});
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des détails:', error);
        this.loading = false;
      }
    });
  }

  // CORRECTION: Récupérer la charge de travail (tickets EN_COURS uniquement)
  getTechnicianWorkload(technician: Utilisateur): number {
    const workload = this.technicianWorkloads.get(technician.id) || 0;
    console.log(`Charge de travail pour ${technician.prenom} ${technician.nom}: ${workload} tickets EN_COURS`);
    return workload;
  }

  // NOUVEAU: Récupérer les détails de charge
  getTechnicianWorkloadDetails(technician: Utilisateur): any {
    return this.technicianWorkloadDetails.get(technician.id) || {};
  }

  // NOUVEAU: Récupérer le nombre de tickets EN_COURS
  getTechnicianActiveTickets(technician: Utilisateur): number {
    const details = this.getTechnicianWorkloadDetails(technician);
    return details.enCours || 0;
  }

  // NOUVEAU: Récupérer le nombre de tickets OUVERT
  getTechnicianOpenTickets(technician: Utilisateur): number {
    const details = this.getTechnicianWorkloadDetails(technician);
    return details.ouvert || 0;
  }

  // NOUVEAU: Récupérer le nombre de tickets RESOLU
  getTechnicianResolvedTickets(technician: Utilisateur): number {
    const details = this.getTechnicianWorkloadDetails(technician);
    return details.resolu || 0;
  }

  // CORRECTION: Calcul du pourcentage basé sur la charge réelle
  getTechnicianWorkloadPercent(technician: Utilisateur): number {
    const workload = this.getTechnicianWorkload(technician);
    const maxWorkload = Math.max(...Array.from(this.technicianWorkloads.values()));
    
    if (maxWorkload === 0) return 0;
    
    const percent = (workload / maxWorkload) * 100;
    console.log(`Pourcentage charge pour ${technician.prenom}: ${percent}%`);
    return percent;
  }

  // CORRECTION: Classes CSS basées sur la charge réelle
  getWorkloadClass(technician: Utilisateur): string {
    const workload = this.getTechnicianWorkload(technician);
    console.log(`Classe CSS pour ${technician.prenom} (charge: ${workload})`);
    
    if (workload === 0) return 'workload-none';
    if (workload <= 2) return 'workload-low';
    if (workload <= 5) return 'workload-medium';
    return 'workload-high';
  }

  // NOUVEAU: Texte descriptif de la charge
  getWorkloadText(technician: Utilisateur): string {
    const workload = this.getTechnicianWorkload(technician);
    const details = this.getTechnicianWorkloadDetails(technician);
    
    if (workload === 0) {
      return 'Aucun ticket en cours';
    }
    
    let text = `${workload} ticket${workload > 1 ? 's' : ''} en cours`;
    
    if (details.ouvert && details.ouvert > 0) {
      text += ` • ${details.ouvert} ouvert${details.ouvert > 1 ? 's' : ''}`;
    }
    
    return text;
  }

  // CORRECTION: Assignation automatique basée sur la charge réelle
  autoAssign(): void {
    if (this.technicians.length === 0) {
      this.snackBar.open('Aucun technicien disponible', 'Fermer', { duration: 3000 });
      return;
    }
    
    console.log('Début de l\'assignation automatique');
    console.log('Charges actuelles:', Array.from(this.technicianWorkloads.entries()));
    
    let leastBusyTech = this.technicians[0];
    let minWorkload = this.getTechnicianWorkload(leastBusyTech);
    
    this.technicians.forEach(tech => {
      const workload = this.getTechnicianWorkload(tech);
      console.log(`Technicien ${tech.prenom} ${tech.nom}: ${workload} tickets`);
      
      if (workload < minWorkload) {
        minWorkload = workload;
        leastBusyTech = tech;
      }
    });
    
    console.log(`Technicien le moins chargé: ${leastBusyTech.prenom} ${leastBusyTech.nom} (${minWorkload} tickets)`);
    
    this.assignmentForm.patchValue({ technicianId: leastBusyTech.id });
    
    this.snackBar.open(
      `Technicien le moins chargé sélectionné: ${this.getFullName(leastBusyTech)} (${minWorkload} tickets EN_COURS)`, 
      'Fermer', 
      { duration: 4000 }
    );
  }

  // CORRECTION: Assignation avec mise à jour de la charge
  assign(): void {
    if (this.assignmentForm.invalid) {
      this.snackBar.open('Veuillez sélectionner un technicien', 'Fermer', { duration: 3000 });
      return;
    }
    
    this.loading = true;
    const technicianId = this.assignmentForm.value.technicianId;
    const selectedTech = this.technicians.find(t => t.id === technicianId);
    
    console.log('Assignation du ticket au technicien:', technicianId);
    
    this.ticketService.assignTicket(this.ticket.id!, technicianId).subscribe({
      next: (result) => {
        console.log('Ticket assigné avec succès:', result);
        
        if (selectedTech) {
          const currentWorkload = this.getTechnicianWorkload(selectedTech);
          this.snackBar.open(
            `Ticket assigné à ${this.getFullName(selectedTech)} (nouvelle charge: ${currentWorkload + 1} tickets)`, 
            'Fermer', 
            { duration: 4000 }
          );
        } else {
          this.snackBar.open('Ticket assigné avec succès', 'Fermer', { duration: 3000 });
        }
        
        this.dialogRef.close(result);
      },
      error: (error) => {
        console.error('Erreur lors de l\'assignation:', error);
        this.snackBar.open('Erreur lors de l\'assignation', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  getFullName(user: any): string {
    if (!user) return 'Nom non disponible';
    return `${user.prenom || ''} ${user.nom || ''}`.trim() || 'Nom non disponible';
  }

  getInitials(nom: string, prenom: string): string {
    const firstInitial = prenom ? prenom.charAt(0).toUpperCase() : '';
    const lastInitial = nom ? nom.charAt(0).toUpperCase() : '';
    return firstInitial + lastInitial;
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

  getPriorityClass(priority: string): string {
    return `priority-${priority.toLowerCase()}`;
  }
}