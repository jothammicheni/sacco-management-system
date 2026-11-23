/**
 * All dashboard types. When you wire real APIs, these interfaces
 * describe what the backend should return. No code changes needed
 * here — only in dashboard-api.service.ts.
 */

export interface MemberProfile {
  memberId: string;
  memberNo: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  avatarUrl?: string;
  notificationsCount: number;
}

export interface FinancialSummary {
  totalDeposits: number;
  depositsChangeThisMonth: number;
  shareCapital: number;
  shareCapitalNote: string;
  activeLoanBalance: number;
  activeLoanCount: number;
  loanLimit: number;
  loanLimitMultiple: string;   // e.g. "3x Deposits"
}

export interface GuarantorRequest {
  requestId: string;
  requesterName: string;
  amount: number;
  loanType: string;             // e.g. "Development Loan"
  requestedAt: string;          // ISO string
}

export interface ActiveLoan {
  loanId: string;
  loanNo: string;              // e.g. "LN-902"
  loanType: string;            // e.g. "Emergency Loan"
  totalAmount: number;
  paidAmount: number;
  percentPaid: number;         // 0–100
  nextInstallmentAmount: number;
  nextInstallmentDue: string;  // ISO date
}

export interface RecentTransaction {
  date: string;                // ISO date
  description: string;
  reference: string;           // M-Pesa reference or "SYS-..."
  status: 'Completed' | 'Pending' | 'Failed' | 'Credited';
  amount: number;              // positive = credit, negative = debit
}

export interface LoanEstimate {
  monthlyInstalment: number;
  totalRepayable: number;
  interestRate: number;        // annual %
  processingFee: number;
}