import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class Auth {

    private readonly STORAGE_KEY = "loggenInUserEmail";

    private readonly storedUser = {
      email: "ceren@gmail.com",
      password:"1234"
    }

    login(email: string, password: string): boolean {
      const ok = email === this.storedUser.email && password === this.storedUser.password;


      if(ok){
        localStorage.setItem(this.STORAGE_KEY, email);
      }
      return ok;
    }

    logout(): void {
      localStorage.removeItem(this.STORAGE_KEY);
    }

    isLoggedIn(): boolean {
      return !!localStorage.getItem(this.STORAGE_KEY);
    }

    getLoggedInEmail(): string | null {
      return localStorage.getItem(this.STORAGE_KEY);
    }

}
