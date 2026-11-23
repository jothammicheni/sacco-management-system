import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardApiService } from '../../services/dashboard-api.service';
import { RecentTransaction } from '../../models/dashboard.models';

@Component({
  selector: 'app-recent-transactions',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './recent-transactions.html',
  styleUrl: './recent-transactions.css',
})
export class RecentTransactionsComponent implements OnInit {
  private api = inject(DashboardApiService);
  transactions = signal<RecentTransaction[]>([]);

  ngOnInit(): void {
    this.api.getRecentTransactions().subscribe(t => this.transactions.set(t));
  }

  formatKES(amount: number): string {
    const abs = Math.abs(amount);
    const formatted = new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES',
      maximumFractionDigits: 0,
    }).format(abs);
    return amount >= 0 ? `+${formatted}` : `-${formatted}`;
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  }

  statusClass(status: RecentTransaction['status']): string {
    switch (status) {
      case 'Completed':
      case 'Credited':  return 'bg-green-100 text-green-800';
      case 'Pending':   return 'bg-amber-100 text-amber-800';
      case 'Failed':    return 'bg-red-100 text-red-800';
    }
  }
}