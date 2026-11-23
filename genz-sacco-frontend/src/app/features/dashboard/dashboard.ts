import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardApiService } from './services/dashboard-api.service';
import { MemberProfile } from './models/dashboard.models';
import { GuarantorAlertComponent } from './components/guarantor-alert/guarantor-alert';
import { FinancialSummaryComponent } from './components/financial-summary/financial-summary';
import { QuickActionsComponent } from './components/quick-actions/quick-actions';
import { ActiveLoanComponent } from './components/active-loan/active-loan';
import { LoanCalculatorComponent } from './components/loan-calculator/loan-calculator';
import { RecentTransactionsComponent } from './components/recent-transactions/recent-transactions';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    GuarantorAlertComponent,
    FinancialSummaryComponent,
    QuickActionsComponent,
    ActiveLoanComponent,
    LoanCalculatorComponent,
    RecentTransactionsComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private api = inject(DashboardApiService);

  profile = signal<MemberProfile | null>(null);
  balancesHidden = signal(false);

  ngOnInit(): void {
    this.api.getProfile().subscribe(p => this.profile.set(p));
  }

  toggleBalances(): void {
    this.balancesHidden.update(v => !v);
  }

  logout(): void {
    // wire to /api/v1/auth/logout later
    console.log('logout');
  }
}