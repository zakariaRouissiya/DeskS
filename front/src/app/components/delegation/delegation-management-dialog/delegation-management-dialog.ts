import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DelegationService, DelegationResponse } from '../../../services/delegation.service';
import { UserService } from '../../../services/user.service';
import { CommonModule } from '@angular/common';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-delegation-management-dialog',
  templateUrl: './delegation-management-dialog.html',
  styleUrls: ['./delegation-management-dialog.css'],
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTabsModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    MatProgressSpinnerModule
  ],
})
export class DelegationManagementDialog implements OnInit {
  pendingDelegations: DelegationResponse[] = [];
  allDelegations: DelegationResponse[] = [];
  loading = false;
  commentaires: { [key: number]: string } = {};
  processingIds: Set<number> = new Set();
  currentUser: any = null;
  selectedTab = 0;

  constructor(
    private dialogRef: MatDialogRef<DelegationManagementDialog>,
    private delegationService: DelegationService,
    private userService: UserService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadPendingDelegations();
  }

  loadCurrentUser(): void {
    this.userService.getUserProfile().subscribe({
      next: (user) => {
        this.currentUser = user;
      },
      error: (error) => {
        console.error('Erreur lors du chargement du profil utilisateur:', error);
      }
    });
  }

  loadPendingDelegations(): void {
    this.loading = true;
    this.delegationService.getPendingDelegations().subscribe({
      next: (delegations) => {
        this.pendingDelegations = delegations;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des délégations:', error);
        this.snackBar.open('Erreur lors du chargement des délégations', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadAllDelegations(): void {
    this.loading = true;
    this.delegationService.getAllDelegations().subscribe({
      next: (delegations) => {
        this.allDelegations = delegations;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de l\'historique:', error);
        this.snackBar.open('Erreur lors du chargement de l\'historique', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  onTabChange(event: any): void {
    this.selectedTab = event.index;
    if (this.selectedTab === 1 && this.allDelegations.length === 0) {
      this.loadAllDelegations();
    }
  }

  approveDelegation(delegation: DelegationResponse): void {
    if (this.processingIds.has(delegation.id)) return;

    this.processingIds.add(delegation.id);
    const commentaire = this.commentaires[delegation.id] || '';

    this.delegationService.approveDelegation(delegation.id, commentaire).subscribe({
      next: () => {
        this.loadPendingDelegations();
        if (this.selectedTab === 1) {
          this.loadAllDelegations();
        }
        this.processingIds.delete(delegation.id);
      },
      error: (error) => {
        console.error('Erreur lors de l\'approbation:', error);
        const errorMessage = error.error?.error || 'Erreur lors de l\'approbation';
        this.snackBar.open(errorMessage, 'Fermer', { duration: 3000 });
        this.processingIds.delete(delegation.id);
      }
    });
  }

  rejectDelegation(delegation: DelegationResponse): void {
    if (this.processingIds.has(delegation.id)) return;

    const commentaire = this.commentaires[delegation.id];
    if (!commentaire || commentaire.trim().length === 0) {
      this.snackBar.open('Un commentaire est requis pour refuser une délégation', 'Fermer', { duration: 3000 });
      return;
    }

    this.processingIds.add(delegation.id);

    this.delegationService.rejectDelegation(delegation.id, commentaire).subscribe({
      next: () => {
        this.loadPendingDelegations();
        if (this.selectedTab === 1) {
          this.loadAllDelegations();
        }
        this.processingIds.delete(delegation.id);
      },
      error: (error) => {
        console.error('Erreur lors du refus:', error);
        const errorMessage = error.error?.error || 'Erreur lors du refus';
        this.snackBar.open(errorMessage, 'Fermer', { duration: 3000 });
        this.processingIds.delete(delegation.id);
      }
    });
  }

  getStatusClass(statut: string): string {
    return `delegation-status-${statut.toLowerCase().replace('_', '-')}`;
  }

  getStatusIcon(statut: string): string {
    const iconMap: { [key: string]: string } = {
      'EN_ATTENTE': 'hourglass_empty',
      'APPROUVE': 'check_circle',
      'REFUSE': 'cancel'
    };
    return iconMap[statut] || 'help';
  }

  getStatusLabel(statut: string): string {
    const labelMap: { [key: string]: string } = {
      'EN_ATTENTE': 'En attente',
      'APPROUVE': 'Approuvée',
      'REFUSE': 'Refusée'
    };
    return labelMap[statut] || statut;
  }

  getPriorityClass(priorite: string): string {
    return `priority-${priorite.toLowerCase()}`;
  }

  isProcessing(delegationId: number): boolean {
    return this.processingIds.has(delegationId);
  }

  getFullName(user: any): string {
    if (!user) return 'Nom non disponible';
    return `${user.prenom || ''} ${user.nom || ''}`.trim() || 'Nom non disponible';
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'Non définie';
    return new Date(dateString).toLocaleString('fr-FR');
  }

  getDepartmentName(): string {
    return this.currentUser?.department?.nom || 'Département non défini';
  }

  getPendingCount(): number {
    return this.pendingDelegations.length;
  }

  onClose(): void {
    this.dialogRef.close();
  }
}