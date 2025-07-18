import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AdminService } from '../../../services/admin';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-department-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="modern-dialog-container">
      <!-- Header -->
      <div class="dialog-header">
        <div class="header-content">
          <div class="header-left">
            <div class="icon-wrapper">
              <mat-icon class="header-icon">
                {{ data.mode === 'create' ? 'add_business' : 'edit_business' }}
              </mat-icon>
            </div>
            <div class="header-text">
              <h2 class="dialog-title">
                {{ data.mode === 'create' ? 'Nouveau Département' : 'Modifier le Département' }}
              </h2>
              <p class="dialog-subtitle">
                {{ data.mode === 'create' ? 'Créez un nouveau département pour votre organisation' : 'Mettez à jour les informations du département' }}
              </p>
            </div>
          </div>
          <button mat-icon-button 
                  (click)="onCancel()"
                  class="close-button"
                  matTooltip="Fermer">
            <mat-icon>close</mat-icon>
          </button>
        </div>
        <mat-progress-bar *ngIf="loading" mode="indeterminate" class="progress-bar"></mat-progress-bar>
      </div>

      <!-- Content -->
      <mat-dialog-content class="dialog-content">
        <form [formGroup]="departmentForm" class="modern-form">
          <div class="form-step">
            <div class="step-header">
              <div class="step-info">
                <h3 class="step-title">
                  <mat-icon>info</mat-icon>
                  Informations du département
                </h3>
                <p class="step-description">Renseignez le nom et la description</p>
              </div>
            </div>
            
            <div class="step-content">
              <mat-form-field appearance="fill" class="modern-field">
                <mat-label>
                  <mat-icon class="field-icon">label</mat-icon>
                  Nom du département
                </mat-label>
                <input matInput formControlName="nom" placeholder="Ex: Support Technique" required>
                <mat-error *ngIf="departmentForm.get('nom')?.hasError('required')">Le nom est requis.</mat-error>
              </mat-form-field>

              <mat-form-field appearance="fill" class="modern-field">
                <mat-label>
                  <mat-icon class="field-icon">description</mat-icon>
                  Description
                </mat-label>
                <textarea matInput formControlName="description" rows="4" placeholder="Ex: Gère les incidents et les demandes des utilisateurs..."></textarea>
              </mat-form-field>
            </div>
          </div>
        </form>
      </mat-dialog-content>

      <!-- Actions -->
      <mat-dialog-actions class="dialog-actions">
        <div class="actions-container">
          <button mat-raised-button
                  color="primary"
                  (click)="onSave()"
                  [disabled]="departmentForm.invalid || loading"
                  class="submit-button">
            <mat-icon class="submit-icon">
              {{ data.mode === 'create' ? 'add' : 'save' }}
            </mat-icon>
            <span class="submit-text">
              {{ data.mode === 'create' ? 'Créer le département' : 'Enregistrer' }}
            </span>
            <div class="button-overlay" *ngIf="loading">
              <mat-spinner diameter="20"></mat-spinner>
            </div>
          </button>
        </div>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      font-family: 'Roboto', sans-serif;
    }

    .modern-dialog-container {
      width: 100%;
      max-width: 900px;
      overflow: hidden;
    }

    /* Header Styles */
    .dialog-header {
      background: #2563eb;
      color: white;
      margin: -15px -35px 5 20px;
      position: relative;
    }
    .dialog-header::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0; bottom: 0;
      background: linear-gradient(45deg, rgba(255,255,255,0.1) 25%, transparent 25%), 
                  linear-gradient(-45deg, rgba(255,255,255,0.1) 25%, transparent 25%);
      background-size: 20px 20px;
      opacity: 0.3;
    }
    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 24px;
      position: relative;
      z-index: 1;
    }
    .header-left { display: flex; align-items: center; gap: 16px; }
    .icon-wrapper {
      background: rgba(255, 255, 255, 0.2);
      border-radius: 12px;
      padding: 12px;
      display: flex;
    }
    .header-icon { font-size: 2rem; width: 2rem; height: 2rem; }
    .dialog-title { font-size: 1.5rem; font-weight: 700; margin: 0 0 2px 0; }
    .dialog-subtitle { font-size: 0.9rem; margin: 0; opacity: 0.9; font-weight: 300; }
    .close-button {
      background: rgba(255, 255, 255, 0.1);
      color: white;
      width: 50px;
      height: 50px;
    }
    .progress-bar { position: absolute; bottom: 0; left: 0; right: 0; height: 3px; z-index: 2; }

    /* Content Styles */
    .dialog-content { padding: 24px; background: #f8faff; }
    .modern-form { display: flex; flex-direction: column; gap: 24px; }

    /* Step Styles */
    .form-step {
      background: white;
      border-radius: 16px;
      box-shadow: 0 4px 20px rgba(37, 99, 235, 0.08);
      border: 1px solid #e6f2ff;
    }
    .step-header {
      background: #f8faff;
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 12px;
      border-bottom: 1px solid #e6f2ff;
    }
    .step-number {
      background: #2563eb;
      color: white;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 1rem;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
    }
    .step-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1.1rem;
      font-weight: 600;
      color: #1e40af;
      margin: 0;
    }
    .step-description { color: #64748b; margin: 0; font-size: 0.85rem; }
    .step-content { padding: 20px; }

    /* Form Field Styles */
    .modern-field { width: 100%; }
    .modern-field:not(:last-child) { margin-bottom: 16px; }
    .field-icon { margin-right: 8px; color: #2563eb; }
    ::ng-deep .mat-form-field-appearance-fill .mat-form-field-flex {
        background-color: #fdfdff;
    }

    /* Actions */
    .dialog-actions {
      background: #f8faff;
      margin: 0 -24px -24px -24px;
      padding: 16px 24px;
      border-top: 1px solid #e6f2ff;
    }
    .actions-container { display: flex; justify-content: flex-end; gap: 12px; }
    .cancel-button { border-radius: 8px; padding: 8px 16px; }
    .submit-button {
      border-radius: 8px;
      padding: 8px 20px;
      font-weight: 600;
      margin-bottom: 15px;
      margin-left: 8px;
      position: relative;
    }
    .submit-icon { margin-right: 8px; }
    .button-overlay {
      position: absolute;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(255, 255, 255, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
    }
  `]
})
export class DepartmentDialog implements OnInit {
  departmentForm: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    public dialogRef: MatDialogRef<DepartmentDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { mode: 'create' | 'edit', department?: any }
  ) {
    this.departmentForm = this.fb.group({
      nom: ['', Validators.required],
      description: ['']
    });
  }

  ngOnInit(): void {
    if (this.data.mode === 'edit' && this.data.department) {
      this.departmentForm.patchValue(this.data.department);
    }
  }

  onSave(): void {
    if (this.departmentForm.invalid || this.loading) return;

    this.loading = true;
    const action = this.data.mode === 'create'
      ? this.adminService.createDepartment(this.departmentForm.value)
      : this.adminService.updateDepartment(this.data.department.id, this.departmentForm.value);

    action.subscribe({
      next: () => {
        this.loading = false;
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.loading = false;
        console.error('Erreur lors de la sauvegarde du département', err);
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}