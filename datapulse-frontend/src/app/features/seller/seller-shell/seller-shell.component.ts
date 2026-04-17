import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { ToastComponent } from '../../../shared/components/toast/toast.component';
import { ChatBubbleComponent } from '../../../shared/components/chat-bubble/chat-bubble.component';

@Component({
  selector: 'app-seller-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, ToastComponent, ChatBubbleComponent],
  templateUrl: './seller-shell.component.html',
})
export class SellerShellComponent {
  readonly auth = inject(AuthService);

  readonly nav = [
    { path: '/seller', icon: 'dashboard', label: 'Overview', exact: true },
    { path: '/seller/products', icon: 'inventory_2', label: 'Products' },
    { path: '/seller/orders', icon: 'receipt_long', label: 'Orders' },
    { path: '/seller/reviews', icon: 'rate_review', label: 'Reviews' },
    { path: '/seller/analytics', icon: 'insights', label: 'Analytics' },
    { path: '/seller/settings', icon: 'tune', label: 'Settings' },
  ];

  logout(): void {
    this.auth.logout();
  }
}
