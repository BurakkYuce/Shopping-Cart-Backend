import { Component, OnInit, inject, signal, computed, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  AnalyticsService,
  AnalyticsSales,
  AnalyticsCustomers,
  AnalyticsProducts,
  PlatformOverview,
  CustomerSegments,
  TopProduct,
} from '../../../core/services/analytics.service';
import { SpinnerComponent } from '../../../shared/components/spinner/spinner.component';

const COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#84cc16'];

@Component({
  selector: 'app-admin-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, SpinnerComponent],
  template: `
    <header class="sticky top-0 z-40 flex items-center justify-between border-b border-outline/40 bg-background/90 px-12 py-6 backdrop-blur-xl">
      <div class="flex items-center gap-8">
        <span class="text-2xl font-bold tracking-tight text-text-primary">DataPulse</span>
        <h2 class="border-l border-outline/40 pl-8 text-xl font-semibold text-text-primary">Platform Analytics</h2>
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
    </header>

    <section class="px-12 py-10">
      <app-spinner *ngIf="loading()"></app-spinner>

      <div *ngIf="!loading()">
        <!-- KPI Cards -->
        <div class="grid gap-5 sm:grid-cols-2 lg:grid-cols-5">
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary" style="font-size: 20px">payments</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Total GMV</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">\${{ overview()?.totalGmv ?? 0 | number:'1.2-2' }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-success" style="font-size: 20px">receipt_long</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Total Orders</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ overview()?.totalOrders ?? 0 | number }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-amber-500" style="font-size: 20px">group</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Total Users</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ overview()?.totalUsers ?? 0 | number }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-cyan-500" style="font-size: 20px">storefront</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Total Stores</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">{{ overview()?.totalStores ?? 0 | number }}</p>
          </div>
          <div class="rounded-2xl bg-background-raised p-6 shadow-atelier-sm">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-purple-500" style="font-size: 20px">shopping_cart</span>
              <p class="text-xs font-bold uppercase tracking-wider text-text-tertiary">Avg Order Value</p>
            </div>
            <p class="mt-3 text-3xl font-bold text-text-primary">\${{ sales()?.averageOrderValue ?? 0 | number:'1.2-2' }}</p>
          </div>
        </div>

        <!-- Revenue Trend -->
        <div class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
          <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Revenue Trend</h3>
          <p class="mt-1 text-xs text-text-tertiary">{{ groupByLabel }} revenue over time</p>
          <div id="revenue-trend-chart" class="mt-4" style="height: 320px"></div>
        </div>

        <!-- Two-column: Top Stores + Customer Segments -->
        <div class="mt-8 grid gap-8 lg:grid-cols-2">
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Top Stores by Revenue</h3>
            <div id="top-stores-chart" class="mt-4" style="height: 300px"></div>
          </div>
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Customer Segments</h3>
            <div id="segments-chart" class="mt-4" style="height: 300px"></div>
          </div>
        </div>

        <!-- Two-column: Category Revenue + Satisfaction Distribution -->
        <div class="mt-8 grid gap-8 lg:grid-cols-2">
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Revenue by Category</h3>
            <div id="category-chart" class="mt-4" style="height: 300px"></div>
          </div>
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Customer Satisfaction</h3>
            <div id="satisfaction-chart" class="mt-4" style="height: 300px"></div>
          </div>
        </div>

        <!-- Two-column: Top Cities + Spend by Membership -->
        <div class="mt-8 grid gap-8 lg:grid-cols-2">
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Top Cities</h3>
            <div id="cities-chart" class="mt-4" style="height: 300px"></div>
          </div>
          <div class="rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
            <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Spend by Membership</h3>
            <div id="membership-chart" class="mt-4" style="height: 300px"></div>
          </div>
        </div>

        <!-- Top Products Table -->
        <div class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
          <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Top Selling Products</h3>
          <div class="mt-4 overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-outline/40 text-left text-xs font-bold uppercase tracking-wider text-text-tertiary">
                  <th class="pb-3 pr-4">#</th>
                  <th class="pb-3 pr-4">Product</th>
                  <th class="pb-3 pr-4 text-right">Qty Sold</th>
                  <th class="pb-3 text-right">Revenue</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let p of topProducts(); let i = index"
                    class="border-b border-outline/20">
                  <td class="py-3 pr-4 text-text-tertiary">{{ i + 1 }}</td>
                  <td class="py-3 pr-4 font-semibold text-text-primary">{{ p.productName }}</td>
                  <td class="py-3 pr-4 text-right text-text-secondary">{{ p.totalQuantity | number }}</td>
                  <td class="py-3 text-right font-semibold text-text-primary">\${{ p.totalRevenue | number:'1.2-2' }}</td>
                </tr>
                <tr *ngIf="topProducts().length === 0">
                  <td colspan="4" class="py-8 text-center text-text-tertiary">No product data yet.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Ask assistant -->
        <div class="mt-8 rounded-3xl bg-background-raised p-8 shadow-atelier-sm">
          <h3 class="text-xs font-bold uppercase tracking-widest text-text-primary">Ask the assistant</h3>
          <p class="mt-3 text-sm text-text-secondary">For deeper, conversational insights across the entire platform, open the DataPulse Assistant. Ask things like "what were the top 5 performing curators last quarter?" or "which categories have the highest return rate?"</p>
        </div>
      </div>
    </section>
  `,
})
export class AdminAnalyticsComponent implements OnInit, AfterViewChecked {
  private readonly analytics = inject(AnalyticsService);

  readonly overview = signal<PlatformOverview | null>(null);
  readonly sales = signal<AnalyticsSales | null>(null);
  readonly categorySales = signal<AnalyticsSales | null>(null);
  readonly customers = signal<AnalyticsCustomers | null>(null);
  readonly products = signal<AnalyticsProducts | null>(null);
  readonly segments = signal<CustomerSegments | null>(null);
  readonly loading = signal<boolean>(true);

  groupBy = 'day';
  get groupByLabel(): string {
    return this.groupBy === 'day' ? 'Daily' : this.groupBy === 'week' ? 'Weekly' : 'Monthly';
  }

  readonly topProducts = computed<TopProduct[]>(
    () => this.products()?.topSellingProducts?.slice(0, 10) ?? [],
  );

  private chartsRendered = false;
  private Plotly: any = null;

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);
    this.chartsRendered = false;
    let loaded = 0;
    const checkDone = () => { if (++loaded >= 5) this.loading.set(false); };

    this.analytics.getPlatformOverview().subscribe({
      next: (d) => { this.overview.set(d); checkDone(); },
      error: () => { this.overview.set({ totalGmv: 0, totalOrders: 0, totalUsers: 0, totalStores: 0, topStores: [] }); checkDone(); },
    });
    this.analytics.getSales({ groupBy: this.groupBy as any }).subscribe({
      next: (d) => { this.sales.set(d); checkDone(); },
      error: () => { this.sales.set({ totalRevenue: 0, orderCount: 0, averageOrderValue: 0, revenueByDay: {} }); checkDone(); },
    });
    this.analytics.getSales({ groupBy: 'category' }).subscribe({
      next: (d) => { this.categorySales.set(d); checkDone(); },
      error: () => { this.categorySales.set({ totalRevenue: 0, orderCount: 0, averageOrderValue: 0, revenueByDay: {} }); checkDone(); },
    });
    this.analytics.getCustomers().subscribe({
      next: (d) => { this.customers.set(d); checkDone(); },
      error: () => { this.customers.set({ averageAge: 0, spendByMembership: {}, satisfactionDistribution: {}, topCities: {} }); checkDone(); },
    });
    this.analytics.getProducts().subscribe({
      next: (d) => { this.products.set(d); checkDone(); },
      error: () => { this.products.set({ topSellingProducts: [], avgRatingByCategory: {} }); checkDone(); },
    });
    this.analytics.getCustomerSegments().subscribe({
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
    this.renderTopStoresChart();
    this.renderSegmentsChart();
    this.renderCategoryChart();
    this.renderSatisfactionChart();
    this.renderCitiesChart();
    this.renderMembershipChart();
  }

  private renderRevenueChart(): void {
    const P = this.Plotly;
    if (!P) return;
    const s = this.sales();
    if (!s) return;

    let data: Record<string, number> = {};
    if (this.groupBy === 'week' && s.revenueByWeek) data = s.revenueByWeek;
    else if (this.groupBy === 'month' && s.revenueByMonth) data = s.revenueByMonth;
    else data = s.revenueByDay;

    // Daily data spans the full dataset (~2+ years) and renders as a flat line
    // against a few large spikes. Trim to the trailing window for readability.
    const windowSize = this.groupBy === 'day' ? 30 : this.groupBy === 'week' ? 26 : 24;
    const sorted = Object.entries(data).sort(([a], [b]) => a.localeCompare(b)).slice(-windowSize);
    const x = sorted.map(([k]) => k);
    const y = sorted.map(([, v]) => v);

    P.newPlot('revenue-trend-chart', [{
      x, y,
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

  private renderTopStoresChart(): void {
    const P = this.Plotly;
    const stores = this.overview()?.topStores ?? [];
    if (!P || stores.length === 0) return;

    const top = stores.slice(0, 8);
    P.newPlot('top-stores-chart', [{
      y: top.map(s => s.storeName),
      x: top.map(s => s.revenue),
      type: 'bar',
      orientation: 'h',
      marker: { color: top.map((_, i) => COLORS[i % COLORS.length]), borderradius: 6 },
      text: top.map(s => '$' + s.revenue.toLocaleString(undefined, { minimumFractionDigits: 2 })),
      textposition: 'outside',
      textfont: { size: 11, color: '#a1a1aa' },
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 10, r: 80, b: 30, l: 120 },
      xaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickprefix: '$' },
      yaxis: { autorange: 'reversed' },
    }, { responsive: true, displayModeBar: false });
  }

  private renderSegmentsChart(): void {
    const P = this.Plotly;
    const segs = this.segments()?.segments ?? [];
    if (!P || segs.length === 0) return;

    P.newPlot('segments-chart', [{
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

  private renderCategoryChart(): void {
    const P = this.Plotly;
    const catData = this.categorySales()?.revenueByCategory ?? {};
    if (!P || Object.keys(catData).length === 0) return;

    const sorted = Object.entries(catData).sort(([, a], [, b]) => b - a).slice(0, 10);
    P.newPlot('category-chart', [{
      x: sorted.map(([k]) => k),
      y: sorted.map(([, v]) => v),
      type: 'bar',
      marker: { color: sorted.map((_, i) => COLORS[i % COLORS.length]), borderradius: 6 },
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 10, r: 20, b: 80, l: 60 },
      xaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickangle: -45 },
      yaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickprefix: '$' },
    }, { responsive: true, displayModeBar: false });
  }

  private renderSatisfactionChart(): void {
    const P = this.Plotly;
    const sat = this.customers()?.satisfactionDistribution ?? {};
    if (!P || Object.keys(sat).length === 0) return;

    const sorted = Object.entries(sat).sort(([a], [b]) => a.localeCompare(b));
    P.newPlot('satisfaction-chart', [{
      labels: sorted.map(([k]) => k),
      values: sorted.map(([, v]) => v),
      type: 'pie',
      marker: { colors: ['#ef4444', '#f59e0b', '#22c55e', '#06b6d4', '#6366f1'] },
      textinfo: 'label+percent',
      textfont: { size: 11 },
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 10, r: 10, b: 10, l: 10 },
      showlegend: false,
    }, { responsive: true, displayModeBar: false });
  }

  private renderCitiesChart(): void {
    const P = this.Plotly;
    const cities = this.customers()?.topCities ?? {};
    if (!P || Object.keys(cities).length === 0) return;

    const sorted = Object.entries(cities).sort(([, a], [, b]) => b - a).slice(0, 10);
    P.newPlot('cities-chart', [{
      x: sorted.map(([k]) => k),
      y: sorted.map(([, v]) => v),
      type: 'bar',
      marker: { color: '#06b6d4', borderradius: 6 },
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 10, r: 20, b: 80, l: 50 },
      xaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickangle: -45 },
      yaxis: { gridcolor: 'rgba(161,161,170,0.1)' },
    }, { responsive: true, displayModeBar: false });
  }

  private renderMembershipChart(): void {
    const P = this.Plotly;
    const mem = this.customers()?.spendByMembership ?? {};
    if (!P || Object.keys(mem).length === 0) return;

    const sorted = Object.entries(mem).sort(([, a], [, b]) => b - a);
    P.newPlot('membership-chart', [{
      x: sorted.map(([k]) => k),
      y: sorted.map(([, v]) => v),
      type: 'bar',
      marker: { color: sorted.map((_, i) => COLORS[i % COLORS.length]), borderradius: 6 },
      text: sorted.map(([, v]) => '$' + v.toFixed(2)),
      textposition: 'outside',
      textfont: { size: 11, color: '#a1a1aa' },
    }], {
      paper_bgcolor: 'transparent', plot_bgcolor: 'transparent',
      font: { color: '#a1a1aa', size: 11 },
      margin: { t: 30, r: 20, b: 60, l: 60 },
      xaxis: { gridcolor: 'rgba(161,161,170,0.1)' },
      yaxis: { gridcolor: 'rgba(161,161,170,0.1)', tickprefix: '$' },
    }, { responsive: true, displayModeBar: false });
  }

  exportToExcel(): void {
    const sheets: { name: string; rows: string[][] }[] = [];

    // KPI Summary
    const ov = this.overview();
    const s = this.sales();
    sheets.push({
      name: 'KPI Summary',
      rows: [
        ['Metric', 'Value'],
        ['Total GMV', String(ov?.totalGmv ?? 0)],
        ['Total Orders', String(ov?.totalOrders ?? 0)],
        ['Total Users', String(ov?.totalUsers ?? 0)],
        ['Total Stores', String(ov?.totalStores ?? 0)],
        ['Avg Order Value', String(s?.averageOrderValue ?? 0)],
      ],
    });

    // Revenue by period
    if (s) {
      let data: Record<string, number> = {};
      if (this.groupBy === 'week' && s.revenueByWeek) data = s.revenueByWeek;
      else if (this.groupBy === 'month' && s.revenueByMonth) data = s.revenueByMonth;
      else data = s.revenueByDay;
      const sorted = Object.entries(data).sort(([a], [b]) => a.localeCompare(b));
      sheets.push({
        name: 'Revenue Trend',
        rows: [['Period', 'Revenue'], ...sorted.map(([k, v]) => [k, v.toFixed(2)])],
      });
    }

    // Top Stores
    const stores = ov?.topStores ?? [];
    if (stores.length) {
      sheets.push({
        name: 'Top Stores',
        rows: [['Store', 'Revenue'], ...stores.map(st => [st.storeName, st.revenue.toFixed(2)])],
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

    // Revenue by Category
    const cats = this.categorySales()?.revenueByCategory ?? {};
    if (Object.keys(cats).length) {
      sheets.push({
        name: 'Revenue by Category',
        rows: [['Category', 'Revenue'], ...Object.entries(cats).sort(([, a], [, b]) => b - a).map(([k, v]) => [k, v.toFixed(2)])],
      });
    }

    // Top Products
    const prods = this.products()?.topSellingProducts ?? [];
    if (prods.length) {
      sheets.push({
        name: 'Top Products',
        rows: [['Product', 'Qty Sold', 'Revenue'], ...prods.map(p => [p.productName, String(p.totalQuantity), p.totalRevenue.toFixed(2)])],
      });
    }

    // Top Cities
    const cities = this.customers()?.topCities ?? {};
    if (Object.keys(cities).length) {
      sheets.push({
        name: 'Top Cities',
        rows: [['City', 'Customers'], ...Object.entries(cities).sort(([, a], [, b]) => b - a).map(([k, v]) => [k, String(v)])],
      });
    }

    // Spend by Membership
    const mem = this.customers()?.spendByMembership ?? {};
    if (Object.keys(mem).length) {
      sheets.push({
        name: 'Spend by Membership',
        rows: [['Membership', 'Avg Spend'], ...Object.entries(mem).sort(([, a], [, b]) => b - a).map(([k, v]) => [k, v.toFixed(2)])],
      });
    }

    this.downloadMultiSheetCsv(sheets, 'datapulse-platform-analytics');
  }

  private downloadMultiSheetCsv(sheets: { name: string; rows: string[][] }[], filename: string): void {
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
    a.download = `${filename}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
