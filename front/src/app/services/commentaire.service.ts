import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface CommentaireResponse {
  id: number;
  commentaire: string;
  dateCreation: string;
  dateModification?: string;
  modifie: boolean;
  auteur: {
    id: number;
    nom: string;
    prenom: string;
    email: string;
    role: string;
    department?: {
      id: number;
      nom: string;
    };
  };
}

export interface CreateCommentaireRequest {
  auteurId: number;
  contenu: string;
}

export interface UpdateCommentaireRequest {
  auteurId: number;
  contenu: string;
}

@Injectable({
  providedIn: 'root'
})
export class CommentaireService {
  private readonly baseUrl = `${environment.apiUrl}/commentaires`;

  constructor(private readonly http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token') || localStorage.getItem('access_token');
    return new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    });
  }

  createCommentaire(ticketId: number, request: CreateCommentaireRequest): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/ticket/${ticketId}`, request, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  getCommentairesByTicket(ticketId: number): Observable<CommentaireResponse[]> {
    return this.http.get<CommentaireResponse[]>(`${this.baseUrl}/ticket/${ticketId}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateCommentaire(commentaireId: number, request: UpdateCommentaireRequest): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/${commentaireId}`, request, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  deleteCommentaire(commentaireId: number, auteurId: number): Observable<any> {
    return this.http.delete<any>(`${this.baseUrl}/${commentaireId}?auteurId=${auteurId}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  canUserModifyComment(commentaireId: number, userId: number): Observable<{ canModify: boolean }> {
    return this.http.get<{ canModify: boolean }>(`${this.baseUrl}/${commentaireId}/can-modify?userId=${userId}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  canUserDeleteComment(commentaireId: number, userId: number): Observable<{ canDelete: boolean }> {
    return this.http.get<{ canDelete: boolean }>(`${this.baseUrl}/${commentaireId}/can-delete?userId=${userId}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  countCommentairesByTicket(ticketId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.baseUrl}/ticket/${ticketId}/count`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: any): Observable<never> {
    const errorMessage = error.error?.error || error.message || 'Une erreur inattendue est survenue';
    console.error('Erreur CommentaireService:', error);
    return throwError(() => new Error(errorMessage));
  }
}