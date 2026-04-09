import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';

@Component({
  selector: 'app-auth-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <div class="min-h-screen bg-background">
      <div class="mx-auto flex min-h-screen max-w-7xl flex-col px-4 py-8 sm:px-6 lg:px-8">
        <a routerLink="/" class="inline-flex items-center gap-2 self-start">
          <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-primary shadow-atelier-sm">
            <span class="material-symbols-outlined text-white" style="font-size: 22px">insights</span>
          </div>
          <span class="text-xl font-bold tracking-tight text-text-primary">DataPulse</span>
        </a>
        <div class="flex flex-1 items-center justify-center">
          <router-outlet />
        </div>
      </div>
    </div>
  `,
})
export class AuthShellComponent {}
