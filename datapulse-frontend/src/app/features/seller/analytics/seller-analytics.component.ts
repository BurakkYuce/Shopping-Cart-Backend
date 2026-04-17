import { Component, OnInit, inject, signal, computed, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  AnalyticsService,
  AnalyticsSales,
  AnalyticsProducts,
  StoreKpi,
  CustomerSegments,
  TopProduct,
} from '../../../core/services/analytics.service';
import { ProductService } from '../../../core/services/product.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

const COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#84cc16'];

@Component({
  selector: 'app-seller-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, SpinnerComponent],
  template: `
    <section class="p-8">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-xs font-bold uppercase tracking-wider text-primary">Deeper insight</p>
          <h1 class="mt-2 text-display-md text-text-primary">Analytics</h1>
        </div>
        <div class="flex items-center gap-3">
          <button (click)="exportToExcel()" class="btn-secondary h-10 gap-2 text-sm">
            <span class="material-symbols-outlined" style="font-size: 18px">download</span>
            Export Excel
          </button>
          <select [(ngModel)]="groupBy" (ngModelChange)="onGroupByChange($event)"
                  class="input-base h-10 w-40 bg-background-raised text-sm">
            <option value="day">Daily</option>
            <option value="week">Weekly</option>
            <option value="month">Monthly</option>
          </select>
        </div>
      </div>

      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()">
        <!-- KPI Cards -->
        <div class="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-5">
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary" style="font-size: 20px">payments</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Revenue</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">\${{ kpis()?.totalRevenue ?? sales()?.totalRevenue ?? 0 | number:'1.2-2' }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-success" style="font-size: 20px">receipt_long</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Orders</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ kpis()?.orderCount ?? sales()?.orderCount ?? 0 | number }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-amber-500" style="font-size: 20px">group</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Customers</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ kpis()?.customerCount ?? 0 | number }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-cyan-500" style="font-size: 20px">shopping_cart</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Avg Order Value</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">\${{ kpis()?.averageOrderValue ?? sales()?.averageOrderValue ?? 0 | number:'1.2-2' }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-purple-500" style="font-size: 20px">star</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Avg Rating</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ avgRating() | number:'1.1-1' }}</p>
          </div>
        </div>

        <!-- Revenue Trend -->
        <div class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
          <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Revenue Trend</h3>
          <p class="mt-1 text-xs text-text-tertiary">{{ groupByLabel }} revenue over time</p>
          <div id="seller-revenue-chart" class="mt-4" style="height: 300px"></div>
        </div>

        <!-- Two-column: Rating Distribution + Top Products -->
        <div class="mt-8 grid gap-8 lg:grid-cols-2">
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Rating Distribution</h3>
            <div id="seller-rating-chart" class="mt-4" style="height: 280px"></div>
          </div>
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Top Products</h3>
            <div id="seller-products-chart" class="mt-4" style="height: 280px"></div>
          </div>
        </div>

        <!-- Two-column: Customer Segments + Category Rating -->
        <div class="mt-8 grid gap-8 lg:grid-cols-2">
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Customer Segments</h3>
            <div id="seller-segments-chart" class="mt-4" style="height: 280px"></div>
          </div>
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Avg Rating by Category</h3>
            <div id="seller-category-rating-chart" class="mt-4" style="height: 280px"></div>
          </div>
        </div>

        <!-- Ask assistant -->
        <div class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
          <h3 class="text-xs font-bold uppercase tracking-wider text-text-primary">Ask the assistant</h3>
          <p class="mt-3 text-sm text-text-secondary">For deeper, conversational insight, open the DataPulse Assistant in the bottom-right corner. Ask things like "which category sold best last month?" or "how does my store compare to similar curators?"</p>
        </div>
      </div>
    </section>
  `,
})
export class SellerAnalyticsComponent implements OnInit, AfterViewChecked {
  private readonly analytics = inject(AnalyticsService);
  private readonly productService = inject(ProductService);

  readonly sales = signal<AnalyticsSales | null>(null);
  readonly products = signal<AnalyticsProducts | null>(null);
  readonly kpis = signal<StoreKpi | null>(null);
  readonly segments = signal<CustomerSegments | null>(null);
  readonly loading = signal<boolean>(true);

  groupBy = 'day';
  get groupByLabel(): string {
    return this.groupBy === 'day' ? 'Daily' : this.groupBy === 'week' ? 'Weekly' : 'Monthly';
  }

  readonly avgRating = computed(() => {
    const dist = this.kpis()?.ratingDistribution ?? {};
    let total = 0, count = 0;
    for (const [star, cnt] of Object.entries(dist)) {
      total += Number(star) * Number(cnt);
      count += Number(cnt);
    }
    return count > 0 ? total / count : 0;
  });

  private chartsRendered = false;
  private Plotly: any = null;
  private storeId: string | null = null;

  ngOnInit(): void {
    this.productService.listStores().subscribe({
      next: (stores) => {
        if (stores.length > 0) {
          this.storeId = stores[0].id;
          this.loadStoreData(this.storeId);
        }
      },
    });
    this.loadData();
  }

  private loadData(): void {
    let loaded = 0;
    const checkDone = () => { if (++loaded >= 2) this.loading.set(false); };

    this.analytics.getSales({ groupBy: this.groupBy as any }).subscribe({
      next: (d) => { this.sales.set(d); checkDone(); },
      error: () => { this.sales.set({ totalRevenue: 0, orderCount: 0, averageOrderValue: 0, revenueByDay: {} }); checkDone(); },
    });
    this.analytics.getProducts().subscribe({
      next: (d) => { this.products.set(d); checkDone(); },
      error: () => { this.products.set({ topSellingProducts: [], avgRatingByCategory: {} }); checkDone(); },
    });
  }

  private loadStoreData(storeId: string): void {
    this.analytics.getStoreKpis(storeId).subscribe({
      next: (d) => this.kpis.set(d),
      error: () => {},
    });
    this.analytics.getCustomerSegments(storeId).subscribe({
      next: (d) => this.segments.set(d),
      error: () => this.segments.set({ segments: [], totalCustomers: 0 }),
    });
  }

  onGroupByChange(value: string): void {
    this.groupBy = value;
    this.analytics.getSales({ groupBy: value as any }).subscribe({
      next: (d) => {
        this.sales.set(d);
        this.renderRevenueChart();
      },
    });
  }

  ngAfterViewChecked(): void {
    if (!this.loading() && !this.chartsRendered) {
      this.chartsRendered = true;
      this.renderAllCharts();
    }
  }

  private async renderAllCharts(): Promise<void> {
    if (!this.Plotly) {
      this.Plotly = await import('plotly.js-dist-min');
    }
    this.renderRevenueChart();
    this.renderRatingChart();
    this.renderProductsChart();
    this.renderSegmentsChart();
    this.renderCategoryRatingChart();
  }

  private renderRevenueChart(): void {
    const P = this.Plotly;
    const s = this.sales();
    if (!P || !s) return;

    let data: Record<string, number> = {};
    if (this.groupBy === 'week' && s.revenueByWeek) data = s.revenueByWeek;
    else if (this.groupBy === 'month' && s.revenueByMonth) data = s.revenueByMonth;
    else data = s.revenueByDay;

    // The daily bucket spans the seller's entire history (often 2+ years), which
    // squashes recent activity under a handful of high-volume spikes and makes
    // the chart look empty. Trim to the trailing 30 buckets for readability.
    const windowSize = this.groupBy === 'day' ? 30 : this.groupBy === 'week' ? 26 : 24;
    const sorted = Object.entries(data).sort(([a], [b]) => a.localeCompare(b)).slice(-windowSize);
    P.newPlot('seller-revenue-chart', [{
      x: sorted.map(([k]) => k),
      y: sorted.map(([, v]) => v),
      type: 'scatter',
      mode: 'lines+markers',
      fill: 'tozeroy',
      line: { color: '#6366f1', width: 2.5, shape: 'spline' },
      marker: { size: 5, color: '#6366f1' },
      fillcolor: 'rgba(99,102,241,0.1)',
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 10, r: 20, b: 40, l: 60 },
      xaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickangle: -45 },
      yaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickprefix: '$' },
    }, { responsive: true, displayModeBar: false });
  }

  private renderRatingChart(): void {
    const P = this.Plotly;
    const dist = this.kpis()?.ratingDistribution ?? {};
    if (!P || Object.keys(dist).length === 0) return;

    const stars = ['1', '2', '3', '4', '5'];
    const colors = ['#ef4444', '#f97316', '#f59e0b', '#84cc16', '#22c55e'];
    P.newPlot('seller-rating-chart', [{
      x: stars.map(s => s + ' Star'),
      y: stars.map(s => dist[Number(s)] ?? 0),
      type: 'bar',
      marker: { color: colors, borderradius: 6 },
      text: stars.map(s => String(dist[Number(s)] ?? 0)),
      textposition: 'outside',
      textfont: { size: 12, color: '#a1a1aa' },
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 30, r: 20, b: 40, l: 40 },
      xaxis: { gridcolor: 'rgba(161,161,170,0.1)' },
      yaxis: { gridcolor: 'rgba(161,161,170,0.1)' },
    }, { responsive: true, displayModeBar: false });
  }

  private renderProductsChart(): void {
    const P = this.Plotly;
    const prods = this.kpis()?.topProducts ?? this.products()?.topSellingProducts ?? [];
    if (!P || prods.length === 0) return;

    const top = prods.slice(0, 6);
    P.newPlot('seller-products-chart', [{
      y: top.map(p => p.productName.length > 20 ? p.productName.substring(0, 20) + '...' : p.productName),
      x: top.map(p => p.totalRevenue),
      type: 'bar',
      orientation: 'h',
      marker: { color: top.map((_, i) => COLORS[i % COLORS.length]), borderradius: 6 },
      text: top.map(p => '$' + p.totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 2 })),
      textposition: 'outside',
      textfont: { size: 11, color: '#a1a1aa' },
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 10, r: 80, b: 30, l: 130 },
      xaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickprefix: '$' },
      yaxis: { autorange: 'reversed' },
    }, { responsive: true, displayModeBar: false });
  }

  private renderSegmentsChart(): void {
    const P = this.Plotly;
    const segs = this.segments()?.segments ?? [];
    if (!P || segs.length === 0) return;

    P.newPlot('seller-segments-chart', [{
      labels: segs.map(s => s.name.replace('_', ' ')),
      values: segs.map(s => s.count),
      type: 'pie',
      hole: 0.45,
      marker: { colors: COLORS },
      textinfo: 'label+percent',
      textfont: { size: 11 },
      hovertemplate: '%{label}<br>Count: %{value}<br>Avg Spend: $%{customdata:.2f}<extra></extra>',
      customdata: segs.map(s => s.avgSpend),
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 10, r: 10, b: 10, l: 10 },
      showlegend: false,
    }, { responsive: true, displayModeBar: false });
  }

  private renderCategoryRatingChart(): void {
    const P = this.Plotly;
    const cats = this.products()?.avgRatingByCategory ?? {};
    if (!P || Object.keys(cats).length === 0) return;

    const sorted = Object.entries(cats).sort(([, a], [, b]) => b - a);
    P.newPlot('seller-category-rating-chart', [{
      x: sorted.map(([k]) => k),
      y: sorted.map(([, v]) => v),
      type: 'bar',
      marker: { color: sorted.map((_, i) => COLORS[i % COLORS.length]), borderradius: 6 },
      text: sorted.map(([, v]) => v.toFixed(1)),
      textposition: 'outside',
      textfont: { size: 11, color: '#a1a1aa' },
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 30, r: 20, b: 80, l: 40 },
      xaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickangle: -45 },
      yaxis: { gridcolor: 'rgba(161,161,170,0.1)', range: [0, 5.5] },
    }, { responsive: true, displayModeBar: false });
  }

  exportToExcel(): void {
    const sheets: { name: string; rows: string[][] }[] = [];
    const s = this.sales();
    const k = this.kpis();

    // KPI Summary
    sheets.push({
      name: 'KPI Summary',
      rows: [
        ['Metric', 'Value'],
        ['Revenue', String(k?.totalRevenue ?? s?.totalRevenue ?? 0)],
        ['Orders', String(k?.orderCount ?? s?.orderCount ?? 0)],
        ['Customers', String(k?.customerCount ?? 0)],
        ['Avg Order Value', String(k?.averageOrderValue ?? s?.averageOrderValue ?? 0)],
        ['Avg Rating', String(this.avgRating())],
      ],
    });

    // Revenue Trend
    if (s) {
      let data: Record<string, number> = {};
      if (this.groupBy === 'week' && s.revenueByWeek) data = s.revenueByWeek;
      else if (this.groupBy === 'month' && s.revenueByMonth) data = s.revenueByMonth;
      else data = s.revenueByDay;
      const sorted = Object.entries(data).sort(([a], [b]) => a.localeCompare(b));
      sheets.push({
        name: 'Revenue Trend',
        rows: [['Period', 'Revenue'], ...sorted.map(([period, val]) => [period, val.toFixed(2)])],
      });
    }

    // Top Products
    const prods = k?.topProducts ?? this.products()?.topSellingProducts ?? [];
    if (prods.length) {
      sheets.push({
        name: 'Top Products',
        rows: [['Product', 'Qty Sold', 'Revenue'], ...prods.map(p => [p.productName, String(p.totalQuantity), p.totalRevenue.toFixed(2)])],
      });
    }

    // Rating Distribution
    const dist = k?.ratingDistribution ?? {};
    if (Object.keys(dist).length) {
      sheets.push({
        name: 'Rating Distribution',
        rows: [['Stars', 'Count'], ...['1', '2', '3', '4', '5'].map(star => [star + ' Star', String(dist[Number(star)] ?? 0)])],
      });
    }

    // Customer Segments
    const segs = this.segments()?.segments ?? [];
    if (segs.length) {
      sheets.push({
        name: 'Customer Segments',
        rows: [['Segment', 'Count', 'Percentage', 'Avg Spend'], ...segs.map(sg => [sg.name, String(sg.count), sg.percentage.toFixed(2), sg.avgSpend.toFixed(2)])],
      });
    }

    // Category Ratings
    const cats = this.products()?.avgRatingByCategory ?? {};
    if (Object.keys(cats).length) {
      sheets.push({
        name: 'Category Ratings',
        rows: [['Category', 'Avg Rating'], ...Object.entries(cats).sort(([, a], [, b]) => b - a).map(([cat, val]) => [cat, val.toFixed(2)])],
      });
    }

    const allRows: string[][] = [];
    for (const sheet of sheets) {
      allRows.push([`--- ${sheet.name} ---`]);
      allRows.push(...sheet.rows);
      allRows.push([]);
    }
    const csv = allRows.map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\n');
    const BOM = '\uFEFF';
    const blob = new Blob([BOM + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'datapulse-store-analytics.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
