import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-quick-actions',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './quick-actions.html',
  styleUrl: './quick-actions.css',
})
export class QuickActionsComponent {
  actions = [
    { icon: '➕', label: 'Deposit via M-Pesa', route: '/savings/deposit', primary: true },
    { icon: '💵', label: 'Apply for Loan',       route: '/loans/apply',     primary: false },
    { icon: '🏦', label: 'Withdraw (FOSA)',      route: '/savings/withdraw', primary: false },
    { icon: '📄', label: 'Download Statement',   route: '/statements',      primary: false },
  ];
}