import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardApiService } from '../../services/dashboard-api.service';
import { ActiveLoan } from '../../models/dashboard.models';

@Component({
  selector: 'app-active-loan',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './active-loan.html',
  styleUrl: './active-loan.css',
})
export class ActiveLoanComponent implements OnInit {
  private api = inject(DashboardApiService);
  loan = signal<ActiveLoan | null>(null);

  ngOnInit(): void {
    this.api.getActiveLoans().subscribe(loans => {
      this.loan.set(loans[0] ?? null);
    });
  }

  formatKES(amount: number): string {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES',
      maximumFractionDigits: 0,
    }).format(amount);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  }
}