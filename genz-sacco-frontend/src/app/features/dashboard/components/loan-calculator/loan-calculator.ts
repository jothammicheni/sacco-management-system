import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DashboardApiService } from '../../services/dashboard-api.service';
import { LoanEstimate } from '../../models/dashboard.models';

@Component({
  selector: 'app-loan-calculator',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './loan-calculator.html',
  styleUrl: './loan-calculator.css',
})
export class LoanCalculatorComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(DashboardApiService);

  estimate = signal<LoanEstimate | null>(null);

  form = this.fb.group({
    amount: [100_000, [Validators.required, Validators.min(1_000), Validators.max(1_000_000)]],
    tenure: [12, [Validators.required, Validators.min(1), Validators.max(60)]],
  });

  ngOnInit(): void {
    this.recalculate();
  }

  recalculate(): void {
    const { amount, tenure } = this.form.value;
    if (!amount || !tenure) return;

    this.api.calculateLoan(amount, tenure).subscribe(e => this.estimate.set(e));
  }

  formatKES(amount: number): string {
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES',
      maximumFractionDigits: 0,
    }).format(amount);
  }
}