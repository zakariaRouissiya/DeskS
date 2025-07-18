import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { TicketService, Ticket } from '../../../services/ticket.service';
import { UserService } from '../../../services/user.service';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-ticket-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatStepperModule,
    MatCardModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './ticket-form-dialog.html',
  styleUrls: ['./ticket-form-dialog.css']
})
export class TicketFormDialogComponent implements OnInit {
  ticketForm: FormGroup;
  loading = false;
  priorites = ['FAIBLE', 'MOYENNE', 'HAUTE', 'CRITIQUE'];
  types = ['Matériel', 'Logiciel', 'Réseau', 'Compte', 'Autre'];
  selectedFile: File | null = null;
  formMode: 'create' | 'edit' = 'create';
  employees: any[] = [];

  constructor(
    private fb: FormBuilder,
    private ticketService: TicketService,
    private userService: UserService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<TicketFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.formMode = this.data.mode || 'create';
    this.ticketForm = this.createForm();
  }

  ngOnInit(): void {
    if (this.formMode === 'create') {
      this.loadEmployeesByDepartment();
    } else if (this.formMode === 'edit' && this.data.ticket) {
      this.populateForm(this.data.ticket);
    }
  }

  createForm(): FormGroup {
    return this.fb.group({
      titre: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      priorite: ['MOYENNE', Validators.required],
      type: ['', Validators.required],
      userId: ['', Validators.required] // Toujours requis maintenant
    });
  }

  // MODIFIÉ: Charger seulement les employés du même département
  loadEmployeesByDepartment(): void {
    this.loading = true;
    
    const currentUserDepartmentId = this.data.currentUser?.department?.id;
    
    if (!currentUserDepartmentId) {
      this.snackBar.open('Impossible de déterminer votre département', 'Fermer', { duration: 3000 });
      this.loading = false;
      return;
    }

    console.log('Chargement des employés du département:', currentUserDepartmentId);
    
    // Utiliser la méthode pour récupérer les employés du même département
    this.userService.getEmployeesByDepartment(currentUserDepartmentId).subscribe({
      next: (employees) => {
        console.log('Employés du département reçus:', employees);
        this.employees = employees;
        
        // Pré-sélectionner l'utilisateur actuel s'il est un employé
        if (this.data.currentUser && this.data.currentUser.role?.includes('EMPLOYEE')) {
          this.ticketForm.patchValue({ userId: this.data.currentUser.id });
        }
        
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des employés:', error);
        this.snackBar.open('Impossible de charger la liste des employés du département', 'Fermer', { duration: 3000 });
        this.loading = false;
        
        // Fallback: utiliser seulement l'utilisateur actuel
        if (this.data.currentUser) {
          this.employees = [this.data.currentUser];
          this.ticketForm.patchValue({ userId: this.data.currentUser.id });
        }
      }
    });
  }

  populateForm(ticket: Ticket): void {
    this.ticketForm.patchValue({
      titre: ticket.titre,
      description: ticket.description,
      priorite: ticket.priorite,
      type: ticket.type,
      userId: ticket.user?.id || ''
    });
  }

  getTypeIcon(type: string): string {
    const iconMap: { [key: string]: string } = {
      'Matériel': 'computer',
      'Logiciel': 'apps',
      'Réseau': 'wifi',
      'Compte': 'account_circle',
      'Autre': 'help_outline'
    };
    return iconMap[type] || 'help_outline';
  }

  removeFile(): void {
    this.selectedFile = null;
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

  getFileIcon(fileType: string): string {
    if (fileType.includes('image')) return 'image';
    if (fileType.includes('pdf')) return 'picture_as_pdf';
    if (fileType.includes('word') || fileType.includes('document')) return 'description';
    if (fileType.includes('text')) return 'text_snippet';
    return 'attach_file';
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const zone = event.currentTarget as HTMLElement;
    zone.classList.add('drag-over');
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const zone = event.currentTarget as HTMLElement;
    zone.classList.remove('drag-over');
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const zone = event.currentTarget as HTMLElement;
    zone.classList.remove('drag-over');
    
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  private handleFile(file: File): void {
    const maxSize = 10 * 1024 * 1024; // 10MB
    const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf', 'text/plain', 
                         'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    
    if (file.size > maxSize) {
      this.snackBar.open('Le fichier est trop volumineux (max 10MB)', 'Fermer', { duration: 3000 });
      return;
    }
    
    if (!allowedTypes.includes(file.type)) {
      this.snackBar.open('Type de fichier non autorisé', 'Fermer', { duration: 3000 });
      return;
    }
    
    this.selectedFile = file;
  }

  onFileSelected(event: Event): void {
    const element = event.target as HTMLInputElement;
    if (element.files && element.files.length > 0) {
      this.handleFile(element.files[0]);
    }
  }

  onSubmit(): void {
    if (this.ticketForm.invalid) {
      this.markFormGroupTouched();
      return;
    }
    this.loading = true;

    if (this.formMode === 'create') {
      this.createTicket();
    } else {
      this.updateTicket();
    }
  }

  createTicket(): void {
    const formValue = this.ticketForm.value;
    const userId = formValue.userId;
    
    if (!userId) {
      this.loading = false;
      this.snackBar.open('Veuillez sélectionner un employé', 'Fermer', { duration: 3000 });
      return;
    }

    // AJOUTÉ: Vérifier que l'employé sélectionné appartient au même département
    const selectedEmployee = this.employees.find(emp => emp.id === userId);
    if (!selectedEmployee) {
      this.loading = false;
      this.snackBar.open('Employé sélectionné non valide', 'Fermer', { duration: 3000 });
      return;
    }

    const currentUserDepartmentId = this.data.currentUser?.department?.id;
    const selectedEmployeeDepartmentId = selectedEmployee.department?.id;

    if (currentUserDepartmentId !== selectedEmployeeDepartmentId) {
      this.loading = false;
      this.snackBar.open('Vous ne pouvez créer un ticket que pour des employés de votre département', 'Fermer', { duration: 3000 });
      return;
    }
    
    const ticket: Ticket = {
      titre: formValue.titre,
      description: formValue.description,
      priorite: formValue.priorite,
      type: formValue.type,
      statut: 'OUVERT'
    };

    this.ticketService.createTicket(ticket, userId, this.selectedFile || undefined).subscribe({
      next: (newTicket) => {
        this.snackBar.open('Ticket créé avec succès', 'Fermer', { duration: 3000 });
        this.loading = false;
        this.dialogRef.close(newTicket);
      },
      error: (err) => {
        console.error('Erreur création ticket:', err);
        this.snackBar.open('Erreur lors de la création du ticket', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  updateTicket(): void {
    if (!this.data.ticket?.id) {
      this.loading = false;
      return;
    }
    const formValue = this.ticketForm.value;
    const updatedTicket: Partial<Ticket> = {
      titre: formValue.titre,
      description: formValue.description,
      priorite: formValue.priorite,
      type: formValue.type
    };
    this.ticketService.updateTicket(this.data.ticket.id, updatedTicket).subscribe({
      next: (ticket) => {
        this.snackBar.open('Ticket mis à jour avec succès', 'Fermer', { duration: 3000 });
        this.loading = false;
        this.dialogRef.close(ticket);
      },
      error: () => {
        this.snackBar.open('Erreur lors de la mise à jour du ticket', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  markFormGroupTouched(): void {
    Object.keys(this.ticketForm.controls).forEach(key => {
      const control = this.ticketForm.get(key);
      control?.markAsTouched();
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  get titreError(): string {
    const control = this.ticketForm.get('titre');
    if (control?.hasError('required')) return 'Le titre est requis';
    if (control?.hasError('minlength')) return 'Le titre doit contenir au moins 5 caractères';
    if (control?.hasError('maxlength')) return 'Le titre ne peut pas dépasser 100 caractères';
    return '';
  }

  get descriptionError(): string {
    const control = this.ticketForm.get('description');
    if (control?.hasError('required')) return 'La description est requise';
    if (control?.hasError('minlength')) return 'La description doit contenir au moins 10 caractères';
    return '';
  }

  get typeError(): string {
    const control = this.ticketForm.get('type');
    if (control?.hasError('required')) return 'Le type d\'incident est requis';
    return '';
  }

  get userIdError(): string {
    const control = this.ticketForm.get('userId');
    if (control?.hasError('required')) return 'L\'employé concerné est requis';
    return '';
  }
}