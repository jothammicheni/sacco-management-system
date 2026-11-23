import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';
import {
  MemberProfile,
  FinancialSummary,
  GuarantorRequest,
  ActiveLoan,
  RecentTransaction,
  LoanEstimate,
} from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private http = inject(HttpClient);
  private base = '/api/v1/member';

  // ============================================================
  // DUMMY DATA — replace each method body with a real HTTP call
  // when the backend endpoint exists. Signature stays the same.
  // ============================================================

  getProfile(): Observable<MemberProfile> {
    return of({
      memberId: '7b6a1b2c-3d4e-5f60-7890-abcdef123456',
      memberNo: 'SC-04829',
      firstName: 'Kamau',
      lastName: 'Mwangi',
      phone: '254712345678',
      email: 'kamau.mwangi@example.com',
      avatarUrl: undefined,
      notificationsCount: 2,
    }).pipe(delay(300));
  }

  getFinancialSummary(): Observable<FinancialSummary> {
    return of({
      totalDeposits: 245_000,
      depositsChangeThisMonth: 5_000,
      shareCapital: 20_000,
      shareCapitalNote: 'Non-withdrawable',
      activeLoanBalance: 120_000,
      activeLoanCount: 2,
      loanLimit: 735_000,
      loanLimitMultiple: '3x Deposits',
    }).pipe(delay(300));
  }

  getGuarantorRequests(): Observable<GuarantorRequest[]> {
    return of([
      {
        requestId: 'g-001',
        requesterName: 'Jane Wanjiku',
        amount: 50_000,
        loanType: 'Development Loan',
        requestedAt: new Date(Date.now() - 3_600_000).toISOString(),
      },
    ]).pipe(delay(300));
  }

  getActiveLoans(): Observable<ActiveLoan[]> {
    return of([
      {
        loanId: 'ln-902',
        loanNo: 'LN-902',
        loanType: 'Emergency Loan',
        totalAmount: 300_000,
        paidAmount: 180_000,
        percentPaid: 60,
        nextInstallmentAmount: 15_000,
        nextInstallmentDue: '2026-12-05',
      },
    ]).pipe(delay(300));
  }

getRecentTransactions(): Observable<RecentTransaction[]> {
  return of<RecentTransaction[]>([
    { date: '2026-11-20', description: 'Monthly BOSA Deposit', reference: 'QK89L2M10P', status: 'Completed', amount: 5_000 },
    { date: '2026-11-05', description: 'Loan Repayment (#LN-902)', reference: 'QK44P9X21A', status: 'Completed', amount: -15_000 },
    { date: '2026-10-28', description: 'FOSA ATM/M-Pesa Withdrawal', reference: 'QJ91W0R44T', status: 'Completed', amount: -10_000 },
    { date: '2026-10-15', description: 'Dividend Payout (2025/26)', reference: 'SYS-DIV-2026', status: 'Credited', amount: 18_400 },
  ]).pipe(delay(300));
}


  calculateLoan(amount: number, tenureMonths: number): Observable<LoanEstimate> {
    // Simple flat-interest placeholder; replace with real backend calc
    const annualRate = 0.12;
    const totalInterest = amount * annualRate * (tenureMonths / 12);
    const processingFee = amount * 0.01;
    return of({
      monthlyInstalment: Math.round((amount + totalInterest) / tenureMonths),
      totalRepayable: Math.round(amount + totalInterest + processingFee),
      interestRate: annualRate * 100,
      processingFee: Math.round(processingFee),
    }).pipe(delay(300));
  }

  // ============================================================
  // REAL API EXAMPLES — uncomment when backend is ready
  // ============================================================
  //
  // getProfile() {
  //   return this.http.get<MemberProfile>(`${this.base}/profile`);
  // }
  // getFinancialSummary() {
  //   return this.http.get<FinancialSummary>(`${this.base}/summary`);
  // }
  // getGuarantorRequests() {
  //   return this.http.get<GuarantorRequest[]>(`${this.base}/guarantor-requests`);
  // }
  // getActiveLoans() {
  //   return this.http.get<ActiveLoan[]>(`${this.base}/loans/active`);
  // }
  // getRecentTransactions() {
  //   return this.http.get<RecentTransaction[]>(`${this.base}/transactions/recent?limit=5`);
  // }
  // calculateLoan(amount: number, tenureMonths: number) {
  //   return this.http.get<LoanEstimate>(`${this.base}/loans/estimate`, {
  //     params: { amount: amount.toString(), tenure: tenureMonths.toString() },
  //   });
  // }
}