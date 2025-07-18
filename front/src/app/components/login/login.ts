import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { AuthentificationService } from "../../services/authentification";
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

interface Particle {
  x: number;
  y: number;
  delay: number;
}

@Component({
  selector: "app-login",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule
  ],
  templateUrl: "./login.html",
  styleUrls: ["./login.css"],
})
export class LoginComponent implements OnInit {
  email = "";
  password = "";
  error = "";
  isLoading = false;
  showPassword = false;
  rememberMe = false;
  particles: Particle[] = [];

  constructor(
    private readonly authService: AuthentificationService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.generateParticles();
    this.loadSavedCredentials();
  }

  generateParticles() {
    for (let i = 0; i < 50; i++) {
      this.particles.push({
        x: Math.random() * 100,
        y: Math.random() * 100,
        delay: Math.random() * 8
      });
    }
  }

  loadSavedCredentials() {
    const savedEmail = localStorage.getItem('saved_email');
    const rememberMeStatus = localStorage.getItem('remember_me') === 'true';
    if (rememberMeStatus && savedEmail) {
      this.email = savedEmail;
      this.rememberMe = true;
    }
  }

  login() {
    if (!this.email || !this.password) {
      this.error = "Veuillez remplir tous les champs";
      this.shakeCard();
      return;
    }

    this.isLoading = true;
    this.error = "";

    this.authService.login(this.email, this.password).subscribe({
      next: () => {
        if (this.rememberMe) {
          localStorage.setItem('saved_email', this.email);
          localStorage.setItem('remember_me', 'true');
        } else {
          localStorage.removeItem('saved_email');
          localStorage.removeItem('remember_me');
        }

        this.error = "";
        this.isLoading = false;

        if (this.authService.isAdmin()) {
          this.router.navigate(["/admin"]);
        } else {
          this.router.navigate(["/tickets"]);
        }
      },
      error: (error) => {
        console.error('Erreur de connexion:', error);
        this.error = "Email ou mot de passe incorrect";
        this.isLoading = false;
        this.shakeCard();
      },
    });
  }

  onForgotPassword() {
    if (!this.email) {
      this.error = "Veuillez saisir votre adresse email pour réinitialiser votre mot de passe";
      this.shakeCard();
      return;
    }

    this.isLoading = true;
    this.error = "";

    setTimeout(() => {
      this.isLoading = false;
      this.error = "";
      alert(`Un email de réinitialisation a été envoyé à ${this.email}`);
    }, 800);
  }

  private shakeCard() {
    const card = document.querySelector('.login-card');
    if (card) {
      card.classList.add('animate__shakeX');
      setTimeout(() => {
        card.classList.remove('animate__shakeX');
      }, 1000);
    }
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  onRememberMeChange() {
    if (!this.rememberMe) {
      localStorage.removeItem('saved_email');
      localStorage.removeItem('remember_me');
    }
  }

  onInputChange() {
    if (this.error) {
      this.error = "";
    }
  }
}
