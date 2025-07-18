import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { AdminService } from '../../../services/admin';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-user-dialog',
  templateUrl: './user-dialog.html',
  styleUrls: ['./user-dialog.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatDialogModule
  ]
})
export class UserDialogComponent {
  userForm: FormGroup;
  isLoading = false;
  isEditMode = false;

  userRoles = [
    {
      value: 'ADMINISTRATEUR',
      label: 'Administrateur',
      icon: 'admin_panel_settings',
      color: '#6366f1',
      description: 'Accès complet à la gestion et à la configuration du système.'
    },
    {
      value: 'TECHNICIEN_SUPPORT',
      label: 'Technicien',
      icon: 'engineering',
      color: '#06b6d4',
      description: 'Gestion technique et résolution des tickets de support.'
    },
    {
      value: 'EMPLOYEE',
      label: 'Employé',
      icon: 'person',
      color: '#64748b',
      description: 'Accès standard pour la création et le suivi des tickets.'
    },
    {
      value: 'MANAGER_IT',
      label: 'Manager IT',
      icon: 'supervisor_account',
      color: '#10b981',
      description: 'Supervision des équipes IT et des tickets.'
    }
  ];

  departements: any[] = [];

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<UserDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.isEditMode = data.mode === 'edit';
    this.userForm = this.fb.group({
      nom: [data.user?.nom || '', [Validators.required, Validators.maxLength(50)]],
      prenom: [data.user?.prenom || '', [Validators.required, Validators.maxLength(50)]],
      email: [data.user?.email || '', [Validators.required, Validators.email]],
      role: [data.user?.role || '', Validators.required],
      // Corriger ici : utiliser departementId (comme dans le backend)
      departementId: [data.user?.department?.id || '', Validators.required]
    });
    this.loadDepartements();
  }

  loadDepartements() {
    this.adminService.getDepartments().subscribe({
      next: (depts) => this.departements = depts.departements || depts,
      error: () => this.departements = []
    });
  }

  getDialogTitle() {
    return this.isEditMode ? 'Modifier un utilisateur' : 'Créer un utilisateur';
  }

  getSubmitButtonText() {
    return this.isEditMode ? 'Enregistrer' : 'Créer';
  }

  getFieldError(field: string): string {
    const control = this.userForm.get(field);
    if (!control || !control.errors) return '';
    if (control.errors['required']) return 'Ce champ est requis';
    if (control.errors['email']) return 'Adresse email invalide';
    if (control.errors['maxlength']) return `Maximum ${control.errors['maxlength'].requiredLength} caractères`;
    return '';
  }

  getRoleInfo(value: string) {
    return this.userRoles.find(r => r.value === value);
  }

  onSubmit() {
    if (this.userForm.valid) {
      const formData = this.userForm.value;
      
      // Debug : vérifier les données
      console.log('Form data:', formData);
      
      if (this.isEditMode) {
        // Pour la modification
        this.adminService.updateUser(this.data.user.id, formData).subscribe({
          next: (response) => {
            this.snackBar.open('Utilisateur modifié avec succès', 'Fermer', { duration: 3000 });
            this.dialogRef.close(true);
          },
          error: (error) => {
            this.snackBar.open('Erreur lors de la modification', 'Fermer', { duration: 3000 });
          }
        });
      } else {
        // Pour la création
        this.adminService.createUser(formData).subscribe({
          next: (response) => {
            this.snackBar.open('Utilisateur créé avec succès', 'Fermer', { duration: 3000 });
            this.dialogRef.close(true);
          },
          error: (error) => {
            this.snackBar.open('Erreur lors de la création', 'Fermer', { duration: 3000 });
          }
        });
      }
    }
  }

  onCancel() {
    this.dialogRef.close(false);
  }
}