import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportingService {
  private readonly apiUrl = `${environment.apiUrl}/reporting`;

  constructor(private readonly http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token') || localStorage.getItem('access_token');
    return new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  getGlobalStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/global-stats`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketsByStatus(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/tickets-by-status`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketsByPriority(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/tickets-by-priority`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketsByDepartment(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/tickets-by-department`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getUsersByRole(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/users-by-role`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketsByMonth(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/tickets-by-month`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getAverageResolutionTime(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/average-resolution-time`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketVolumeByDay(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/ticket-volume-by-day`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketVolumeByHour(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/ticket-volume-by-hour`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketVolumeByWeek(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/ticket-volume-by-week`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketVolumeByYear(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/ticket-volume-by-year`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }
  getTicketVolumeByTechnician(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/ticket-volume-by-technician`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: any) {
    let msg = 'Erreur inattendue';
    if (error.error instanceof ErrorEvent) {
      msg = error.error.message;
    } else if (error.error && typeof error.error === 'string') {
      msg = error.error;
    } else if (error.message) {
      msg = error.message;
    }
    console.error('ReportingService error:', error);
    return throwError(() => new Error(msg));
  }
}