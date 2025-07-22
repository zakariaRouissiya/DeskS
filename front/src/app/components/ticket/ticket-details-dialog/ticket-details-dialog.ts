import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatMenuModule } from '@angular/material/menu';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Ticket, TicketService } from '../../../services/ticket.service';
import { DelegationService, DelegationResponse } from '../../../services/delegation.service';
import { CommentaireService, CommentaireResponse } from '../../../services/commentaire.service';
import { AuthentificationService } from '../../../services/authentification';
import { DeleteCommentDialog } from '../../commentaire/delete-comment-dialog/delete-comment-dialog';
import { MatDialog } from '@angular/material/dialog';
import { Editor, Toolbar, NgxEditorMenuComponent } from 'ngx-editor';
import { NgxEditorModule } from 'ngx-editor';

@Component({
  selector: 'app-ticket-details-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule,
    MatTabsModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    MatSnackBarModule,
    MatInputModule,
    MatFormFieldModule,
    MatMenuModule,
    FormsModule,
    NgxEditorMenuComponent,
    NgxEditorModule
],
  templateUrl: './ticket-details-dialog.html',
  styleUrls: ['./ticket-details-dialog.css']
})
export class TicketDetailsDialog implements OnInit, OnDestroy {
  ticket: Ticket;
  delegations: DelegationResponse[] = [];
  commentaires: CommentaireResponse[] = [];
  loadingDelegations = false;
  loadingCommentaires = false;
  
  newCommentaire = '';
  sendingComment = false;
  editingCommentId: number | null = null;
  editingCommentText = '';
  currentUser: any = null;

  editEditor!: Editor;
  toolbar: Toolbar = [
    ['bold', 'italic'],
    ['underline', 'strike'],
    ['ordered_list', 'bullet_list'],
    ['link', 'image'],
    ['text_color', 'background_color'],
    ['align_left', 'align_center', 'align_right', 'align_justify'],
  ];

  constructor(
    public dialogRef: MatDialogRef<TicketDetailsDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { ticket: Ticket },
    private readonly snackBar: MatSnackBar,
    private readonly http: HttpClient,
    private readonly ticketService: TicketService,
    private readonly delegationService: DelegationService,
    private readonly commentaireService: CommentaireService,
    private readonly authService: AuthentificationService,
    private readonly dialog: MatDialog
  ) {
    this.ticket = data.ticket;
    this.currentUser = this.authService.getCurrentUser();
  }

  ngOnInit(): void {
    this.loadDelegations();
    this.loadCommentaires();
    this.editEditor = new Editor();
  }

  ngOnDestroy() {
    this.editEditor?.destroy();
  }

  loadDelegations(): void {
    if (!this.ticket.id) {
      this.loadingDelegations = false;
      return;
    }

    this.loadingDelegations = true;
    this.delegationService.getDelegationsByTicket(this.ticket.id).subscribe({
      next: (delegations) => {
        this.delegations = delegations;
        this.loadingDelegations = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des délégations:', error);
        this.loadingDelegations = false;
      }
    });
  }

  loadCommentaires(): void {
    if (!this.ticket.id) {
      this.loadingCommentaires = false;
      return;
    }

    this.loadingCommentaires = true;
    this.commentaireService.getCommentairesByTicket(this.ticket.id).subscribe({
      next: (commentaires) => {
        this.commentaires = commentaires;
        this.loadingCommentaires = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des commentaires:', error);
        this.loadingCommentaires = false;
        this.snackBar.open('Erreur lors du chargement des commentaires', 'Fermer', { duration: 3000 });
      }
    });
  }

  sendCommentaire(): void {
    if (!this.newCommentaire.trim() || this.sendingComment) {
      return;
    }

    if (!this.currentUser?.id) {
      this.snackBar.open('Vous devez être connecté pour envoyer un commentaire', 'Fermer', { duration: 3000 });
      return;
    }

    this.sendingComment = true;
    const request = {
      auteurId: this.currentUser.id,
      contenu: this.newCommentaire.trim()
    };

    this.commentaireService.createCommentaire(this.ticket.id!, request).subscribe({
      next: (response) => {
        this.newCommentaire = '';
        this.sendingComment = false;
        this.snackBar.open('Commentaire envoyé avec succès', 'Fermer', { duration: 2000 });
        this.loadCommentaires();
      },
      error: (error) => {
        console.error('Erreur lors de l\'envoi du commentaire:', error);
        this.sendingComment = false;
        this.snackBar.open('Erreur lors de l\'envoi du commentaire', 'Fermer', { duration: 3000 });
      }
    });
  }

  startEditComment(commentaire: CommentaireResponse): void {
    this.editingCommentId = commentaire.id;
    this.editingCommentText = commentaire.commentaire;
  }

  cancelEditComment(): void {
    this.editingCommentId = null;
    this.editingCommentText = '';
  }

  saveEditComment(): void {
    if (!this.editingCommentText.trim() || !this.editingCommentId) {
      return;
    }

    const request = {
      auteurId: this.currentUser.id,
      contenu: this.editingCommentText.trim()
    };

    this.commentaireService.updateCommentaire(this.editingCommentId, request).subscribe({
      next: (response) => {
        this.editingCommentId = null;
        this.editingCommentText = '';
        this.snackBar.open('Commentaire modifié avec succès', 'Fermer', { duration: 2000 });
        this.loadCommentaires();
      },
      error: (error) => {
        console.error('Erreur lors de la modification du commentaire:', error);
        this.snackBar.open('Erreur lors de la modification du commentaire', 'Fermer', { duration: 3000 });
      }
    });
  }

  deleteComment(commentaire: any): void {
    const dialogRef = this.dialog.open(DeleteCommentDialog, {
      data: { commentaire }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.commentaireService.deleteCommentaire(commentaire.id, this.currentUser.id).subscribe({
          next: () => {
            this.snackBar.open('Commentaire supprimé avec succès', 'Fermer', { duration: 2000 });
            this.loadCommentaires();
          },
          error: (error) => {
            console.error('Erreur lors de la suppression du commentaire:', error);
            this.snackBar.open('Erreur lors de la suppression du commentaire', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }

  canEditComment(commentaire: CommentaireResponse): boolean {
    return this.currentUser?.id === commentaire.auteur.id;
  }

  canDeleteComment(commentaire: CommentaireResponse): boolean {
    return this.currentUser?.id === commentaire.auteur.id;
  }

  getCommentaireCount(): number {
    return this.commentaires.length;
  }

  hasCommentaires(): boolean {
    return this.commentaires.length > 0;
  }

  closeDialog(): void {
    this.dialogRef.close();
  }

  getTypeIcon(type: string): string {
    const typeIcons: { [key: string]: string } = {
      'Matériel': 'computer',
      'Logiciel': 'apps',
      'Réseau': 'wifi',
      'Compte': 'account_circle',
      'Autre': 'help_outline'
    };
    return typeIcons[type] || 'help_outline';
  }

  getStatusIcon(statut: string): string {
    const statusIcons: { [key: string]: string } = {
      'OUVERT': 'lock_open',
      'EN_COURS': 'schedule',
      'RESOLU': 'check_circle',
      'FERME': 'lock'
    };
    return statusIcons[statut] || 'help_outline';
  }

  getStatusLabel(statut: string): string {
    const statusLabels: { [key: string]: string } = {
      'OUVERT': 'Ouvert',
      'EN_COURS': 'En cours',
      'RESOLU': 'Résolu',
      'FERME': 'Fermé'
    };
    return statusLabels[statut] || statut;
  }

  getStatusClass(statut: string): string {
    return `status-${statut.toLowerCase().replace('_', '-')}`;
  }

  getPriorityIcon(priorite: string): string {
    const priorityIcons: { [key: string]: string } = {
      'FAIBLE': 'keyboard_arrow_down',
      'MOYENNE': 'remove',
      'HAUTE': 'keyboard_arrow_up',
      'CRITIQUE': 'warning'
    };
    return priorityIcons[priorite] || 'remove';
  }

  getPriorityClass(priorite: string): string {
    return `priority-${priorite.toLowerCase()}`;
  }

  getFileIcon(mimeType: string): string {
    if (mimeType.includes('pdf')) return 'picture_as_pdf';
    if (mimeType.includes('image')) return 'image';
    if (mimeType.includes('text')) return 'description';
    if (mimeType.includes('video')) return 'video_file';
    if (mimeType.includes('audio')) return 'audio_file';
    return 'attach_file';
  }

  getInitials(nom: string, prenom: string): string {
    const firstInitial = prenom ? prenom.charAt(0).toUpperCase() : '';
    const lastInitial = nom ? nom.charAt(0).toUpperCase() : '';
    return firstInitial + lastInitial;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  getTimeSince(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMinutes / 60);
    const diffInDays = Math.floor(diffInHours / 24);

    if (diffInDays > 0) {
      return `Il y a ${diffInDays} jour${diffInDays > 1 ? 's' : ''}`;
    } else if (diffInHours > 0) {
      return `Il y a ${diffInHours} heure${diffInHours > 1 ? 's' : ''}`;
    } else if (diffInMinutes > 0) {
      return `Il y a ${diffInMinutes} minute${diffInMinutes > 1 ? 's' : ''}`;
    } else {
      return 'À l\'instant';
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));
    
    if (diffInMinutes < 1) {
      return 'À l\'instant';
    } else if (diffInMinutes < 60) {
      return `Il y a ${diffInMinutes} minute${diffInMinutes > 1 ? 's' : ''}`;
    } else if (diffInMinutes < 1440) {
      const hours = Math.floor(diffInMinutes / 60);
      return `Il y a ${hours} heure${hours > 1 ? 's' : ''}`;
    } else {
      const days = Math.floor(diffInMinutes / 1440);
      if (days < 7) {
        return `Il y a ${days} jour${days > 1 ? 's' : ''}`;
      } else {
        return date.toLocaleDateString('fr-FR', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        });
      }
    }
  }

  isPreviewable(mimeType: string): boolean {
    return mimeType.includes('image') || 
           mimeType.includes('pdf') || 
           mimeType.includes('text');
  }

  downloadFile(): void {
    if (!this.ticket.pieceJointe?.id) {
      this.snackBar.open('Aucun fichier disponible', 'Fermer', { duration: 3000 });
      return;
    }

    this.ticketService.downloadFile(this.ticket.pieceJointe.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = this.ticket.pieceJointe?.nomDuFichier || 'fichier';
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        window.URL.revokeObjectURL(url);
        this.snackBar.open('Téléchargement démarré', 'Fermer', { duration: 3000 });
      },
      error: (error) => {
        console.error('Erreur lors du téléchargement:', error);
        this.snackBar.open('Erreur lors du téléchargement', 'Fermer', { duration: 3000 });
      }
    });
  }

  previewFile(): void {
    if (!this.ticket.pieceJointe?.id) {
      this.snackBar.open('Aperçu non disponible', 'Fermer', { duration: 3000 });
      return;
    }

    this.ticketService.previewFile(this.ticket.pieceJointe.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (error) => {
        console.error('Erreur lors de l\'aperçu:', error);
        this.snackBar.open('Erreur lors de l\'aperçu', 'Fermer', { duration: 3000 });
      }
    });
  }

  getAssignmentCount(): number {
    let count = 0;
    if (this.ticket.assignedTo) count++;
    if (this.ticket.delegatedTo && this.ticket.delegatedTo.id !== this.ticket.assignedTo?.id) count++;
    return count;
  }

  hasAssignmentHistory(): boolean {
    return !!(this.ticket.assignedTo || this.ticket.delegatedTo || this.ticket.dateResolution);
  }

  getCurrentHandler(): any {
    return this.ticket.delegatedTo || this.ticket.assignedTo;
  }

  getAssignmentDate(): string {
    if (this.ticket.dateCreation) {
      const creationDate = new Date(this.ticket.dateCreation);
      creationDate.setHours(creationDate.getHours() + 1);
      return creationDate.toLocaleString('fr-FR');
    }
    return 'Date non disponible';
  }

  getDelegationDate(): string {
    if (this.ticket.dateCreation) {
      const creationDate = new Date(this.ticket.dateCreation);
      creationDate.setHours(creationDate.getHours() + 2);
      return creationDate.toLocaleString('fr-FR');
    }
    return 'Date non disponible';
  }

  getFullName(person: any): string {
    if (!person) return 'Nom non disponible';
    return `${person.prenom || ''} ${person.nom || ''}`.trim() || 'Nom non disponible';
  }

  getAssignmentStatus(): string {
    if (this.ticket.delegatedTo) {
      return 'Délégué';
    } else if (this.ticket.assignedTo) {
      return 'Assigné';
    } else {
      return 'Non assigné';
    }
  }

  getAssignmentStatusColor(): string {
    if (this.ticket.delegatedTo) {
      return '#8b5cf6';
    } else if (this.ticket.assignedTo) {
      return '#f59e0b';
    } else {
      return '#94a3b8';
    }
  }

  getDelegationCount(): number {
    return this.delegations.length;
  }

  hasDelegations(): boolean {
    return this.delegations.length > 0;
  }

  getDelegationStatusIcon(statut: string): string {
    const statusIcons: { [key: string]: string } = {
      'EN_ATTENTE': 'hourglass_empty',
      'APPROUVE': 'check_circle',
      'REFUSE': 'cancel'
    };
    return statusIcons[statut] || 'help';
  }

  getDelegationStatusLabel(statut: string): string {
    const statusLabels: { [key: string]: string } = {
      'EN_ATTENTE': 'En attente',
      'APPROUVE': 'Approuvée',
      'REFUSE': 'Refusée'
    };
    return statusLabels[statut] || statut;
  }

  getDelegationStatusClass(statut: string): string {
    return `delegation-status-${statut.toLowerCase().replace('_', '-')}`;
  }

  getCurrentDelegation(): DelegationResponse | null {
    return this.delegations.find(d => d.statut === 'APPROUVE') || null;
  }

  getPendingDelegation(): DelegationResponse | null {
    return this.delegations.find(d => d.statut === 'EN_ATTENTE') || null;
  }

  getLatestDelegation(): DelegationResponse | null {
    if (this.delegations.length === 0) return null;
    return this.delegations.sort((a, b) => new Date(b.dateDemande).getTime() - new Date(a.dateDemande).getTime())[0];
  }

  getRoleIcon(role: string): string {
    const roleIcons: { [key: string]: string } = {
      'EMPLOYEE': 'person',
      'TECHNICIEN_SUPPORT': 'support_agent',
      'MANAGER_IT': 'supervisor_account',
      'ADMINISTRATEUR': 'admin_panel_settings'
    };
    return roleIcons[role] || 'person';
  }

  getRoleLabel(role: string): string {
    const roleLabels: { [key: string]: string } = {
      'EMPLOYEE': 'Employé',
      'TECHNICIEN_SUPPORT': 'Technicien Support',
      'MANAGER_IT': 'Manager IT',
      'ADMINISTRATEUR': 'Administrateur'
    };
    return roleLabels[role] || role;
  }

  getRoleClass(role: string): string {
    return `role-${role.toLowerCase().replace('_', '-')}`;
  }

  canSendComments(): boolean {
    if (!this.currentUser) return false;
    
    const userRole = this.currentUser.role;
    const userId = this.currentUser.id;
    
    if (this.ticket.user?.id === userId) return true;
    
    if (this.ticket.assignedTo?.id === userId) return true;
    
    if (this.ticket.delegatedTo?.id === userId) return true;
    
    if (userRole === 'MANAGER_IT' || userRole === 'ADMINISTRATEUR') return true;
    
    return false;
  }

  isEmployee(): boolean {
    return this.currentUser?.role === 'EMPLOYEE';
  }

  isManager(): boolean {
    return this.currentUser?.role === 'MANAGER_IT' || this.currentUser?.role === 'ADMINISTRATEUR';
  }

  canValidateOrReopen(): boolean {
    return this.isManager() && this.ticket.statut === 'RESOLU';
  }

  validateTicket(): void {
    this.ticketService.updateStatut(this.ticket.id!, 'FERME', this.currentUser.id).subscribe({
      next: (updated) => {
        this.ticket.statut = 'FERME';
        this.snackBar.open('Ticket validé et fermé', 'Fermer', { duration: 2000 });
        this.dialogRef.close('statut-updated');
      },
      error: () => {
        this.snackBar.open('Erreur lors de la validation', 'Fermer', { duration: 3000 });
      }
    });
  }

  reopenTicket(): void {
    this.ticketService.updateStatut(this.ticket.id!, 'EN_COURS', this.currentUser.id).subscribe({
      next: (updated) => {
        this.ticket.statut = 'EN_COURS';
        this.snackBar.open('Ticket rouvert', 'Fermer', { duration: 2000 });
        this.dialogRef.close('statut-updated'); 
      },
      error: () => {
        this.snackBar.open('Erreur lors de la réouverture', 'Fermer', { duration: 3000 });
      }
    });
  }
  getSLA(): string | null {
    const { dateCreation, dateResolution, dateFermeture } = this.ticket;
    if (dateCreation && (dateResolution || dateFermeture)) {
      const start = new Date(dateCreation).getTime();
      const end = new Date(dateFermeture || dateResolution || '').getTime();
      if (!isNaN(start) && !isNaN(end) && end > start) {
        const diffMs = end - start;
        const hours = Math.floor(diffMs / 3600000);
        const minutes = Math.floor((diffMs % 3600000) / 60000);
        return `${hours}h ${minutes}min`;
      }
    }
    return null;
  }
}