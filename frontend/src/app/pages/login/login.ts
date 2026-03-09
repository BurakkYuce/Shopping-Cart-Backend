import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
  standalone: true
})
export class Login {
  email: string = '';
  password: string = '';

  constructor(private router: Router, private auth: Auth) {}

  onLogin(){
    const isSuccessful = this.auth.login(this.email, this.password);

    if(isSuccessful){
      this.router.navigate(['/products']);
    } else {
      alert('Please enter both email and password.');
    }
  }
}
