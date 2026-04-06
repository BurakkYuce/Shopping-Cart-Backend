import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './navbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar></app-navbar>

    <main class="shell-content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .shell-content {
      max-width: 1400px;
      margin: 0 auto;
      padding: 1.5rem;
    }
  `]
})
export class ShellComponent {}
