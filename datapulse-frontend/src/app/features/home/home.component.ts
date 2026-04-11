import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { forkJoin } from 'rxjs';
import { ProductService } from '../../core/services/product.service';
import { Product, Category, Store } from '../../core/models/product.models';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { SpinnerComponent } from '../../shared/components/spinner/spinner.component';

interface HeroSlide {
  kicker: string;
  title: string;
  subtitle: string;
  cta: string;
  ctaLink: string;
  image: string;
}

interface Countdown {
  hours: string;
  minutes: string;
  seconds: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent, SpinnerComponent],
  templateUrl: './home.component.html',
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly productService = inject(ProductService);

  readonly categories = signal<Category[]>([]);
  readonly trending = signal<Product[]>([]);
  readonly flashMarket = signal<Product[]>([]);
  readonly stores = signal<Store[]>([]);
  readonly loading = signal<boolean>(true);

  readonly heroIndex = signal<number>(0);
  readonly heroSlides: HeroSlide[] = [
    {
      kicker: 'Atelier Edition',
      title: 'Pieces born of quiet craft.',
      subtitle: 'Discover independent makers shaping the next season of mindful design.',
      cta: 'Explore the Edit',
      ctaLink: '/products',
      image: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?q=80&w=1920&auto=format&fit=crop',
    },
    {
      kicker: 'Flash Market',
      title: 'The details, only for a moment.',
      subtitle: 'Limited releases from our most-loved curators — gone when the hour runs out.',
      cta: 'Shop the Drop',
      ctaLink: '/products',
      image: 'https://images.unsplash.com/photo-1472851294608-062f824d29cc?q=80&w=1920&auto=format&fit=crop',
    },
    {
      kicker: 'Curators',
      title: 'Meet the hands behind the work.',
      subtitle: 'A rotating collection of ateliers we believe in, all in one place.',
      cta: 'Meet the Curators',
      ctaLink: '/products',
      image: 'https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?q=80&w=1920&auto=format&fit=crop',
    },
  ];

  readonly countdown = signal<Countdown>({ hours: '00', minutes: '00', seconds: '00' });
  private heroTimer?: number;
  private countdownTimer?: number;

  ngOnInit(): void {
    this.loadData();
    this.startHeroRotation();
    this.startCountdown();
  }

  ngOnDestroy(): void {
    if (this.heroTimer) clearInterval(this.heroTimer);
    if (this.countdownTimer) clearInterval(this.countdownTimer);
  }

  goToHero(i: number): void {
    this.heroIndex.set(i);
  }

  private loadData(): void {
    this.loading.set(true);
    forkJoin({
      cats: this.productService.listCategories(),
      trending: this.productService.list({ size: 40, sort: 'rating,desc' }),
      flash: this.productService.list({ size: 40, sort: 'unitPrice,asc' }),
      stores: this.productService.listStores(),
    }).subscribe({
      next: ({ cats, trending, flash, stores }) => {
        this.categories.set(cats.slice(0, 8));
        this.trending.set(trending.content.slice(0, 8));
        this.flashMarket.set(flash.content.slice(0, 4));
        this.stores.set(stores.slice(0, 6));
        this.loading.set(false);
      },
      error: () => {
        this.categories.set([]);
        this.trending.set([]);
        this.flashMarket.set([]);
        this.stores.set([]);
        this.loading.set(false);
      },
    });
  }

  private startHeroRotation(): void {
    this.heroTimer = window.setInterval(() => {
      this.heroIndex.update((i) => (i + 1) % this.heroSlides.length);
    }, 6000);
  }

  private startCountdown(): void {
    const end = Date.now() + 1000 * 60 * 60 * 4; // 4 hours from load
    const tick = () => {
      const diff = Math.max(0, end - Date.now());
      const h = Math.floor(diff / 3600000);
      const m = Math.floor((diff % 3600000) / 60000);
      const s = Math.floor((diff % 60000) / 1000);
      this.countdown.set({
        hours: h.toString().padStart(2, '0'),
        minutes: m.toString().padStart(2, '0'),
        seconds: s.toString().padStart(2, '0'),
      });
    };
    tick();
    this.countdownTimer = window.setInterval(tick, 1000);
  }
}
