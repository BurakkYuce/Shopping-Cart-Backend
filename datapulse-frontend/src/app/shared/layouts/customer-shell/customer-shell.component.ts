import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ChatBubbleComponent } from '../../components/chat-bubble/chat-bubble.component';
import { ToastComponent } from '../../components/toast/toast.component';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-customer-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, FooterComponent, ChatBubbleComponent, ToastComponent],
  template: `
    <div class="flex min-h-screen flex-col bg-background">
      <app-navbar />
      <main class="flex-1">
        <router-outlet />
      </main>
      <app-footer />
      <app-chat-bubble />
      <app-toast />
    </div>
  `,
})
export class CustomerShellComponent implements OnInit {
  private readonly cart = inject(CartService);
  private readonly wishlist = inject(WishlistService);
  private readonly auth = inject(AuthService);

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.cart.reload();
      this.wishlist.list().subscribe({ error: () => {} });
    }
  }
}
