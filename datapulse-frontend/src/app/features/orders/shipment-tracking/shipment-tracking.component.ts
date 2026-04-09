import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { OrderService } from '../../../core/services/order.service';
import { Order, Shipment } from '../../../core/models/order.models';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-shipment-tracking',
  standalone: true,
  imports: [CommonModule, RouterLink, SpinnerComponent, StatusBadgeComponent],
  templateUrl: './shipment-tracking.component.html',
})
export class ShipmentTrackingComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);

  readonly order = signal<Order | null>(null);
  readonly shipment = signal<Shipment | null>(null);
  readonly loading = signal<boolean>(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  load(id: string): void {
    this.loading.set(true);
    this.orderService.get(id).subscribe({
      next: (o) => {
        this.order.set(o);
        this.orderService.getShipmentByOrder(id).subscribe({
          next: (s) => {
            this.shipment.set(s);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
      },
      error: () => this.loading.set(false),
    });
  }
}
