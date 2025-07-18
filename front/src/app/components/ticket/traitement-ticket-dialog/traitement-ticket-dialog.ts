import { Component, Inject, OnInit, OnDestroy } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogContent } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Ticket, TicketService } from '../../../services/ticket.service';
import { CommentaireService } from '../../../services/commentaire.service';
import { AuthentificationService } from '../../../services/authentification';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDivider } from "@angular/material/divider";
import { NgxEditorModule, Editor, Toolbar } from 'ngx-editor';
import { FormGroup, FormControl, Validators, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-traitement-ticket-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDivider,
    NgxEditorModule,
    ReactiveFormsModule,
    MatDialogContent
],
  templateUrl: './traitement-ticket-dialog.html',
  styleUrls: ['./traitement-ticket-dialog.css']
})
export class TraitementTicketDialog implements OnInit, OnDestroy {
  ticket: Ticket;
  sendingSolution = false;
  currentUser: any = null;

  editor!: Editor;
  toolbar: Toolbar = [
    ['bold', 'italic'],
    ['underline', 'strike'],
    ['code', 'blockquote'],
    ['ordered_list', 'bullet_list'],
    [{ heading: ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'] }],
    ['link', 'image'],
    ['text_color', 'background_color'],
    ['align_left', 'align_center', 'align_right', 'align_justify'],
  ];

  form = new FormGroup({
    solution: new FormControl('', Validators.required)
  });

  get solution() {
    return this.form.get('solution')?.value || '';
  }

  constructor(
    public dialogRef: MatDialogRef<TraitementTicketDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { ticket: Ticket },
    private readonly snackBar: MatSnackBar,
    private readonly ticketService: TicketService,
    private readonly commentaireService: CommentaireService,
    private readonly authService: AuthentificationService
  ) {
    this.ticket = data.ticket;
    this.currentUser = this.authService.getCurrentUser();
  }

  ngOnInit(): void {
    this.editor = new Editor();
  }

  ngOnDestroy(): void {
    this.editor.destroy();
  }

  sendSolution(): void {
    if (!this.solution.trim() || this.sendingSolution) {
      return;
    }
    if (!this.currentUser?.id) {
      this.snackBar.open('Vous devez être connecté pour envoyer la solution', 'Fermer', { duration: 3000 });
      return;
    }
    this.sendingSolution = true;
    const request = {
      auteurId: this.currentUser.id,
      contenu: this.solution.trim()
    };
    this.commentaireService.createCommentaire(this.ticket.id!, request).subscribe({
      next: () => {
        this.ticketService.updateStatut(this.ticket.id!, 'RESOLU').subscribe({
          next: () => {
            this.sendingSolution = false;
            this.snackBar.open('Solution enregistrée et ticket résolu', 'Fermer', { duration: 2000 });
            this.dialogRef.close(true);
          },
          error: () => {
            this.sendingSolution = false;
            this.snackBar.open('Erreur lors du changement de statut', 'Fermer', { duration: 3000 });
          }
        });
      },
      error: (error) => {
        console.error('Erreur lors de l\'enregistrement de la solution:', error);
        this.sendingSolution = false;
        this.snackBar.open('Erreur lors de l\'enregistrement', 'Fermer', { duration: 3000 });
      }
    });
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

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  isPreviewable(mimeType: string): boolean {
    return mimeType.includes('image') || 
           mimeType.includes('pdf') || 
           mimeType.includes('text');
  }

  getFileIcon(mimeType: string): string {
    if (mimeType.includes('pdf')) return 'picture_as_pdf';
    if (mimeType.includes('image')) return 'image';
    if (mimeType.includes('text')) return 'description';
    if (mimeType.includes('video')) return 'video_file';
    if (mimeType.includes('audio')) return 'audio_file';
    return 'attach_file';
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
}
