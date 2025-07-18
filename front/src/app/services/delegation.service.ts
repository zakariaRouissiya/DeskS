import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface DelegationResponse {
  id: number;
  ticketId: number;
  ticketTitre: string;
  justification: string;
  statut: string;
  dateDemande: string;
  dateReponse?: string;
  commentaireReponse?: string;
  fromTechnician: {
    id: number;
    nom: string;
    prenom: string;
    email: string;
    departmentNom: string;
  };
  toTechnician: {
    id: number;
    nom: string;
    prenom: string;
    email: string;
    departmentNom: string;
  };
  approvedBy?: {
    id: number;
    nom: string;
    prenom: string;
    email: string;
    departmentNom: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class DelegationService {
  private readonly apiUrl = `${environment.apiUrl}/delegations`;

  constructor(private readonly http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    });
  }

  getPendingDelegations(): Observable<DelegationResponse[]> {
    return this.http.get<DelegationResponse[]>(`${this.apiUrl}/pending`, { 
      headers: this.getHeaders() 
    }).pipe(
      map((response: any) => response || []),
      catchError(error => {
        console.error('Erreur lors de la récupération des délégations:', error);
        return of([]);
      })
    );
  }

  getAllDelegations(): Observable<DelegationResponse[]> {
    return this.http.get<DelegationResponse[]>(`${this.apiUrl}/all`, { 
      headers: this.getHeaders() 
    }).pipe(
      map((response: any) => response || []),
      catchError(error => {
        console.error('Erreur lors de la récupération de l\'historique:', error);
        return of([]);
      })
    );
  }

  approveDelegation(delegationId: number, commentaire: string = ''): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${delegationId}/approve`, { 
      commentaire 
    }, { 
      headers: this.getHeaders() 
    }).pipe(
      map((response: any) => response),
      catchError(error => {
        console.error('Erreur lors de l\'approbation de la délégation:', error);
        throw error;
      })
    );
  }

  rejectDelegation(delegationId: number, commentaire: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${delegationId}/reject`, { 
      commentaire 
    }, { 
      headers: this.getHeaders() 
    }).pipe(
      map((response: any) => response),
      catchError(error => {
        console.error('Erreur lors du refus de la délégation:', error);
        throw error;
      })
    );
  }

  createDelegation(ticketId: number, toTechnicianId: number, justification: string): Observable<any> {
    const body = {
      ticketId,
      toTechnicianId,
      justification
    };

    return this.http.post<any>(`${this.apiUrl}/request`, body, { 
      headers: this.getHeaders() 
    }).pipe(
      map((response: any) => response),
      catchError(error => {
        console.error('Erreur lors de la création de la délégation:', error);
        throw error;
      })
    );
  }

  getDelegationsByTicket(ticketId: number): Observable<DelegationResponse[]> {
    return this.http.get<DelegationResponse[]>(`${this.apiUrl}/ticket/${ticketId}`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des délégations du ticket:', error);
        return of([]);
      })
    );
  }

  getMyDelegationRequests(): Observable<DelegationResponse[]> {
    return this.http.get<DelegationResponse[]>(`${this.apiUrl}/my-requests`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération de mes demandes:', error);
        return of([]);
      })
    );
  }

  getReceivedDelegations(): Observable<DelegationResponse[]> {
    return this.http.get<DelegationResponse[]>(`${this.apiUrl}/received`, { 
      headers: this.getHeaders() 
    }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des délégations reçues:', error);
        return of([]);
      })
    );
  }
}