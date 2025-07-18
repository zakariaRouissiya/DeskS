import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login';
import { TicketComponent } from './components/ticket/ticket';
import { AdminGuard } from './guards/admin.guard';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard';
import { Utilisateurs } from './components/utilisateurs/utilisateurs';
import { Departments } from './components/departments/departments';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'tickets', component: TicketComponent },
  { path: 'admin', component: AdminDashboardComponent, canActivate: [AdminGuard] },
  { path: 'admin/utilisateurs', component: Utilisateurs, canActivate: [AdminGuard] },
  { path: 'admin/departments', component: Departments, canActivate: [AdminGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];