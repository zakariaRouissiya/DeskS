import { UserService } from './../../services/user.service';
import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { AuthentificationService } from '../../services/authentification';

@Component({
  selector: 'app-user-profile-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTabsModule,
    MatSelectModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './user-profile-dialog.html',
  styleUrls: ['./user-profile-dialog.css']
})
export class UserProfileDialogComponent implements OnInit {
  profileForm!: FormGroup;
  passwordForm!: FormGroup;
  currentUser: any = null;
  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;
  loading = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly userService: UserService,
    private readonly snackBar: MatSnackBar,
    private readonly router: Router,
    private readonly authService: AuthentificationService,
    public dialogRef: MatDialogRef<UserProfileDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  ngOnInit(): void {
    this.loadUserProfile();
    this.initPasswordForm();
  }

  loadUserProfile(): void {
    this.loading = true;
    this.userService.getUserProfile().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.initProfileForm();
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement du profil utilisateur', err);
        this.snackBar.open('Impossible de charger vos informations', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  initProfileForm(): void {
    this.profileForm = this.fb.group({
      nom: [this.currentUser?.nom || '', [Validators.required, Validators.minLength(2)]],
      prenom: [this.currentUser?.prenom || '', [Validators.required, Validators.minLength(2)]],
      email: [
        this.currentUser?.email || '', 
        [Validators.required, Validators.email, Validators.pattern('^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}$')]
      ]
    });
  }

  initPasswordForm(): void {
    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [
        Validators.required, 
        Validators.minLength(8),
        Validators.pattern('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$')
      ]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.checkPasswords });
  }

  checkPasswords(group: FormGroup) {
    const password = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { notMatching: true };
  }

  hasUppercase(value: string): boolean {
    return /[A-Z]/.test(value || '');
  }

  hasNumber(value: string): boolean {
    return /\d/.test(value);
  }

  hasSpecialChar(value: string): boolean {
    if (!value) return false;
    return /[^A-Za-z0-9]/.test(value);
  }

  updateProfile(): void {
    if (this.profileForm.invalid) {
      this.markFormGroupTouched(this.profileForm);
      return;
    }

    this.loading = true;
    
    const userData = {
      nom: this.profileForm.value.nom?.trim(),
      prenom: this.profileForm.value.prenom?.trim()
    };

    this.userService.updateUserProfile(userData).subscribe({
      next: (response) => {
        this.snackBar.open('Profil mis à jour avec succès', 'Fermer', { 
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        if (response) {
          this.currentUser = {
            ...this.currentUser,
            nom: response.nom || userData.nom,
            prenom: response.prenom || userData.prenom
          };
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du profil:', error);
        this.loading = false;
        let errorMessage = 'Erreur lors de la mise à jour du profil';
        if (error.message) {
          errorMessage = error.message;
        } else if (error.error && typeof error.error === 'string') {
          errorMessage = error.error;
        } else if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }
        this.snackBar.open(errorMessage, 'Fermer', { 
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  updatePassword(): void {
    if (this.passwordForm.invalid) {
      this.markFormGroupTouched(this.passwordForm);
      return;
    }

    this.loading = true;
    
    const passwordData = {
      currentPassword: this.passwordForm.value.currentPassword,
      newPassword: this.passwordForm.value.newPassword
    };

    this.userService.updatePassword(passwordData).subscribe({
      next: (response) => {
        if (response && response.success === true) {
          this.snackBar.open(
            'Mot de passe mis à jour avec succès. Vous allez être déconnecté pour vous reconnecter.', 
            'Fermer', 
            { duration: 5000 }
          );
          this.passwordForm.reset();
          this.loading = false;
          this.dialogRef.close();
          setTimeout(() => {
            this.authService.logout();
            this.router.navigate(['/login']);
          }, 2000);
        } else {
          this.loading = false;
          this.snackBar.open(
            'Réponse inattendue du serveur. Veuillez vérifier votre mot de passe.', 
            'Fermer', 
            { duration: 5000 }
          );
        }
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du mot de passe:', error);
        this.loading = false;
        let errorMessage = 'Erreur lors de la mise à jour du mot de passe';
        if (error.message) {
          errorMessage = error.message;
        } else if (error.status === 401) {
          errorMessage = 'Mot de passe actuel incorrect';
          this.passwordForm.get('currentPassword')?.reset();
          this.passwordForm.get('currentPassword')?.setErrors({'incorrect': true});
        } else if (error.status === 400) {
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          } else if (error.error && typeof error.error === 'string') {
            errorMessage = error.error;
          } else {
            errorMessage = 'Données invalides';
          }
        } else if (error.status === 404) {
          errorMessage = 'Service non trouvé. Vérifiez la configuration.';
        }
        this.snackBar.open(errorMessage, 'Fermer', { duration: 5000 });
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  closeDialog(): void {
    this.dialogRef.close();
  }
}