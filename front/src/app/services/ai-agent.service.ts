import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AiAgentService {
  private readonly baseUrl = `${environment.apiUrl}/myaiagent`;

  constructor(private readonly http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token') || localStorage.getItem('access_token');
    return new HttpHeaders({
      'Accept': 'text/markdown',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  analyseTicket(ticketId: number): Observable<string> {
    return this.http.get(`${this.baseUrl}/${ticketId}`, {
      headers: this.getHeaders(),
      responseType: 'text'
    });
  }
}