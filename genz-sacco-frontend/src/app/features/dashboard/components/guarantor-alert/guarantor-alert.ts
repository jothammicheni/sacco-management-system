import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardApiService } from '../../services/dashboard-api.service';
import { GuarantorRequest } from '../../models/dashboard.models';

@Component({
  selector: 'app-guarantor-alert',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './guarantor-alert.html',
  styleUrl: './guarantor-alert.css',
})
export class GuarantorAlertComponent implements OnInit {
  private api = inject(DashboardApiService);
  requests = signal<GuarantorRequest[]>([]);

  ngOnInit(): void {
    this.api.getGuarantorRequests().subscribe(r => this.requests.set(r));
  }

  formatKES(amount: number): string {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}