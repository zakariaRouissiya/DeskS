import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export enum Statut {
  OUVERT = 'OUVERT',
  EN_COURS = 'EN_COURS',
  RESOLU = 'RESOLU',
  VALIDE = 'VALIDE',
  FERME = 'FERME'
}

export enum Priorite {
  FAIBLE = 'FAIBLE',
  MOYENNE = 'MOYENNE',
  HAUTE = 'HAUTE',
  CRITIQUE = 'CRITIQUE'
}

export interface Departement {
  id: number;
  nom: string;
  description?: string;
}

export interface Utilisateur {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role?: string;
  department?: Departement;
  dateInscription?: string;
}

export interface PieceJointe {
  id?: number;
  nomDuFichier: string;
  typeDuFichier: string;
  url: string;
  taille: number;
  downloadUrl?: string;
  previewUrl?: string;
}

export interface Commentaire {
  id?: number;
  commentaire: string;
  dateCreation?: Date | string;
  auteur: Utilisateur;
}

export interface Ticket {
  id?: number;
  titre: string;
  description: string;
  statut: Statut | string;
  priorite: Priorite | string;
  dateCreation?: Date | string;
  dateResolution?: Date | string;
  dateFermeture?: Date | string;     
  dateReouverture?: Date | string;    
  tempsResolution?: string;
  type: string;
  user?: Utilisateur;
  department?: Departement;
  commentaires?: Commentaire[];
  pieceJointe?: PieceJointe;
  assignedTo?: Utilisateur;
  delegatedTo?: Utilisateur;
  closedBy?: Utilisateur;             
  reopenedBy?: Utilisateur;           
}

export interface TicketResponseDTO {
  id?: number;
  titre: string;
  description: string;
  statut: Statut | string;
  priorite: Priorite | string;
  dateCreation?: string;
  dateResolution?: string;
  dateFermeture?: string;      
  dateReouverture?: string;    
  type: string;
  user?: {
    id: number;
    nom: string;
    prenom: string;
    email: string;
    department?: {
      id: number;
      nom: string;
    };
  };
  assignedTo?: {
    id: number;
    nom: string;
    prenom: string;
    email: string;
    department?: {
      id: number;
      nom: string;
    };
  };
  closedBy?: Utilisateur;
  reopenedBy?: Utilisateur;
  pieceJointe?: {
    id: number;
    nomDuFichier: string;
    typeDuFichier: string;
    url: string;
    taille: number;
    downloadUrl?: string;
    previewUrl?: string;
  };
}

export interface TicketFilters {
  statut?: Statut;
  priorite?: Priorite;
  dateDebut?: Date;
  dateFin?: Date;
  search?: string;
  userId?: number;
  departmentId?: number;
  assignedToId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private readonly baseUrl = `${environment.apiUrl}/tickets`;
  private readonly fileUrl = `${environment.apiUrl}/files`;

  constructor(private readonly http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token') || localStorage.getItem('access_token');
    return new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  createTicketWithAttachment(ticket: Ticket, userId: number, file?: File): Observable<any> {
    const formData = new FormData();
    
    formData.append('userId', userId.toString());
    formData.append('titre', ticket.titre);
    formData.append('description', ticket.description);
    formData.append('priorite', ticket.priorite.toString());
    formData.append('type', ticket.type);
    
    if (file) {
      formData.append('file', file);
    }

    return this.http.post<any>(`${this.baseUrl}/create`, formData, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  createTicket(ticket: Ticket, userId: number, file?: File): Observable<Ticket> {
    return this.createTicketWithAttachment(ticket, userId, file).pipe(
      map(response => response.ticket),
      catchError(this.handleError)
    );
  }

  addAttachmentToTicket(ticketId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<any>(`${this.baseUrl}/${ticketId}/attachment`, formData, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  downloadFile(pieceJointeId: number): Observable<Blob> {
    return this.http.get(`${this.fileUrl}/download/${pieceJointeId}`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    }).pipe(
      catchError(this.handleError)
    );
  }

  previewFile(pieceJointeId: number): Observable<Blob> {
    return this.http.get(`${this.fileUrl}/preview/${pieceJointeId}`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    }).pipe(
      catchError(this.handleError)
    );
  }

  getMyTickets(userId: number): Observable<TicketResponseDTO[]> {
    return this.http.get<TicketResponseDTO[]>(`${this.baseUrl}/my/${userId}`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  getTicketDetails(ticketId: number): Observable<TicketResponseDTO> {
    return this.http.get<TicketResponseDTO>(`${this.baseUrl}/${ticketId}`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateTicket(ticketId: number, ticket: Partial<Ticket>): Observable<Ticket> {
    return this.http.put<any>(`${this.baseUrl}/${ticketId}`, ticket, { 
      headers: this.getHeaders() 
    }).pipe(
      map(response => response.ticket),
      catchError(this.handleError)
    );
  }

  deleteTicketByEmployee(ticketId: number, userId: number): Observable<void> {
    return this.http.delete<any>(`${this.baseUrl}/employee/${ticketId}?userId=${userId}`, { 
      headers: this.getHeaders() 
    }).pipe(
      map(() => {}), 
      catchError(this.handleError)
    );
  }

  getDepartmentTickets(departmentId: number): Observable<TicketResponseDTO[]> {
    return this.http.get<TicketResponseDTO[]>(`${this.baseUrl}/department/${departmentId}`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  assignTicket(ticketId: number, technicianId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${ticketId}/assign`, { 
      technicianId 
    }, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  autoDispatchTickets(departmentId: number): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auto-dispatch`, { 
      departmentId 
    }, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  getDepartmentTechnicians(departmentId: number): Observable<Utilisateur[]> {
    const url = `${environment.apiUrl}/utilisateurs/departement/${departmentId}/role/TECHNICIEN_SUPPORT`;
    
    
    return this.http.get<Utilisateur[]>(url, { 
      headers: this.getHeaders() 
    }).pipe(
      map(response => {
        return response;
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des techniciens:', error);
        console.error('URL appelée:', url);
        console.error('Headers:', this.getHeaders());
        return throwError(() => error);
      })
    );
  }

  getDepartmentStatistics(departmentId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/department/${departmentId}/statistics`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  getAssignedTickets(technicianId: number): Observable<TicketResponseDTO[]> {
    return this.http.get<TicketResponseDTO[]>(`${this.baseUrl}/assigned/${technicianId}`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(this.handleError)
    );
  }

  getTechnicianWorkload(technicianId: number): Observable<number> {
    return this.http.get<any>(`${this.baseUrl}/technicians/${technicianId}/workload`, {
      headers: this.getHeaders()
    }).pipe(
      map(response => {
        return response.workload || 0;
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération de la charge de travail:', error);
        return of(0);
      })
    );
  }

  getTechnicianWorkloadDetails(technicianId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/technicians/${technicianId}/workload`, {
      headers: this.getHeaders()
    }).pipe(
      map(response => {
        return response.details || {};
      }),
      catchError(error => {
        console.error('Erreur lors de la récupération des détails de charge:', error);
        return of({});
      })
    );
  }

  getDepartmentTechniciansWorkload(departmentId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/department/${departmentId}/technicians/workload`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des charges de département:', error);
        return of([]);
      })
    );
  }

  convertDTOToTicket(dto: TicketResponseDTO): Ticket {
    return {
      id: dto.id,
      titre: dto.titre,
      description: dto.description,
      statut: dto.statut,
      priorite: dto.priorite,
      dateCreation: dto.dateCreation,
      dateResolution: dto.dateResolution,
      dateFermeture: dto.dateFermeture,       
      dateReouverture: dto.dateReouverture,
      type: dto.type,
      user: dto.user ? {
        id: dto.user.id,
        nom: dto.user.nom,
        prenom: dto.user.prenom,
        email: dto.user.email,
        department: dto.user.department
      } : undefined,
      assignedTo: dto.assignedTo ? {
        id: dto.assignedTo.id,
        nom: dto.assignedTo.nom,
        prenom: dto.assignedTo.prenom,
        email: dto.assignedTo.email,
        department: dto.assignedTo.department
      } : undefined,
      closedBy: dto.closedBy ? { ...dto.closedBy } : undefined,         
      reopenedBy: dto.reopenedBy ? { ...dto.reopenedBy } : undefined, 
      department: dto.user?.department,
      pieceJointe: dto.pieceJointe ? {
        id: dto.pieceJointe.id,
        nomDuFichier: dto.pieceJointe.nomDuFichier,
        typeDuFichier: dto.pieceJointe.typeDuFichier,
        url: dto.pieceJointe.url,
        taille: dto.pieceJointe.taille,
        downloadUrl: dto.pieceJointe.downloadUrl,
        previewUrl: dto.pieceJointe.previewUrl
      } : undefined
    };
  }

  private handleError(error: any): Observable<never> {
    const errorMessage = error.error?.error || error.message || 'Une erreur inattendue est survenue';
    console.error('Erreur TicketService:', error);
    return throwError(() => new Error(errorMessage));
  }

  updateStatut(ticketId: number, statut: string, userId: number): Observable<Ticket> {
    return this.http.put<any>(
      `${this.baseUrl}/${ticketId}/statut`,
      { statut, userId },
      { headers: this.getHeaders() }
    ).pipe(
      map(response => response.ticket),
      catchError(this.handleError)
    );
  }
}