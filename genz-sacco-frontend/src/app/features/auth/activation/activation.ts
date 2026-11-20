import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AuthApiService } from '../../../core/api/auth-api.service';
import { ActivationService } from './activation.service';
import { OtpStepComponent } from './steps/otp-step';
import { CredentialsStepComponent } from './steps/credentials-step';

@Component({
  selector: 'app-activation',
  standalone: true,
  imports: [CommonModule, OtpStepComponent, CredentialsStepComponent],
  templateUrl: './activation.html',
  styleUrl: './activation.css',
})
export class Activation implements OnInit {
  private route = inject(ActivatedRoute);
  private api = inject(AuthApiService);
  readonly state = inject(ActivationService);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.state.step.set('manual-phone');
      return;
    }

    this.api.verifyLink(token).subscribe({
      next: res => {
        if (res.valid) {
          this.state.phoneNumber.set(res.phoneNumber ?? null);
          this.state.maskedEmail.set(res.maskedEmail ?? null);
          this.state.step.set('otp');
        } else {
          this.state.fail(this.humanizeReason(res.reason));
        }
      },
      error: () => {
        this.state.fail(
          'We could not verify this link. It may have expired.',
        );
      },
    });
  }

  private humanizeReason(reason?: string): string {
    switch (reason) {
      case 'LINK_EXPIRED':
        return 'This link has expired. Request a new code.';
      case 'ALREADY_ACTIVATED':
        return 'Your account is already activated. Please log in.';
      case 'USER_NOT_FOUND':
        return 'We could not find an account for this link.';
      case 'LINK_NOT_FOUND':
        return 'This link is invalid.';
      default:
        return 'Something went wrong. Please try again.';
    }
  }
}