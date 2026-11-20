import { Injectable, signal } from '@angular/core';
import { AuthTokenResponse } from '../../../../app/core/models/auth.models';

export type ActivationStep =
  | 'loading'
  | 'manual-phone'
  | 'otp'
  | 'credentials'
  | 'done'
  | 'error';

@Injectable({ providedIn: 'root' })
export class ActivationService {
  readonly step = signal<ActivationStep>('loading');
  readonly phoneNumber = signal<string | null>(null);
  readonly maskedEmail = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  private passwordSetupToken: string | null = null;
  private pinSetupToken: string | null = null;

  handleOtpVerified(response: AuthTokenResponse): void {
    switch (response.nextStep) {
      case 'SETUP_PASSWORD':
        if (!response.pinSetupToken) {
          this.fail('Server did not return a setup token.');
          return;
        }
        this.passwordSetupToken = response.pinSetupToken;
        this.setStep('credentials');
        break;

      case 'SETUP_PIN':
        // PIN-reset flow — not used in activation. Kept for future flows.
        if (!response.pinSetupToken) {
          this.fail('Server did not return a setup token.');
          return;
        }
        this.pinSetupToken = response.pinSetupToken;
        this.setStep('credentials');
        break;

      case 'FULL_ACCESS':
        this.setStep('done');
        break;

      default:
        this.fail('Unexpected response from server.');
    }
  }

  handlePasswordSet(response: AuthTokenResponse): void {
    // No longer used in the merged flow — the credentials step
    // chains password + PIN calls itself. Kept for future use.
    switch (response.nextStep) {
      case 'SETUP_PIN':
        if (!response.pinSetupToken) {
          this.fail('Server did not return a setup token.');
          return;
        }
        this.pinSetupToken = response.pinSetupToken;
        this.setStep('credentials');
        break;
      case 'FULL_ACCESS':
        this.setStep('done');
        break;
      default:
        this.fail('Unexpected response from server.');
    }
  }

  handlePinSet(response: AuthTokenResponse): void {
    if (response.nextStep === 'FULL_ACCESS') {
      this.setStep('done');
    } else {
      this.fail('Unexpected response from server.');
    }
  }

  getPasswordSetupToken(): string | null {
    return this.passwordSetupToken;
  }

  getPinSetupToken(): string | null {
    return this.pinSetupToken;
  }

  // ---------- Helpers ----------

  setStep(step: ActivationStep): void {
    this.step.set(step);
  }

  fail(message: string): void {
    this.errorMessage.set(message);
    this.step.set('error');
  }

  reset(): void {
    this.setStep('loading');
    this.phoneNumber.set(null);
    this.maskedEmail.set(null);
    this.submitting.set(false);
    this.errorMessage.set(null);
    this.passwordSetupToken = null;
    this.pinSetupToken = null;
  }
}