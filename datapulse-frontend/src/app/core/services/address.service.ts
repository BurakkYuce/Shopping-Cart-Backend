import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Address, CreateAddressRequest } from '../models/user.models';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/addresses`;

  list(): Observable<Address[]> {
    return this.http.get<Address[]>(this.baseUrl);
  }

  create(body: CreateAddressRequest): Observable<Address> {
    return this.http.post<Address>(this.baseUrl, body);
  }

  update(id: string, body: Partial<CreateAddressRequest>): Observable<Address> {
    return this.http.put<Address>(`${this.baseUrl}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
