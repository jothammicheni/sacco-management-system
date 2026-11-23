import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardApiService } from '../../services/dashboard-api.service';
import { FinancialSummary } from '../../models/dashboard.models';

@Component({
  selector: 'app-financial-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './financial-summary.html',
  styleUrl: './financial-summary.css',
})
export class FinancialSummaryComponent implements OnInit {
  private api = inject(DashboardApiService);
  summary = signal<FinancialSummary | null>(null);

  ngOnInit(): void {
    this.api.getFinancialSummary().subscribe(s => this.summary.set(s));
  }

  formatKES(amount: number): string {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}