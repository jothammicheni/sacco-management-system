import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { AuthApiService } from '../../../../core/api/auth-api.service';
import { ActivationService } from '../activation.service';

@Component({
  selector: 'app-otp-step',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './otp-step.html',
  styleUrl: './otp-step.css',
})
export class OtpStepComponent implements OnInit {
  /** When true, show the phone input (no magic link token present) */
  @Input() manualPhoneEntry = false;

  private fb = inject(FormBuilder);
  private api = inject(AuthApiService);
  readonly state = inject(ActivationService);

  /** Controls whether the "Send code" button has been clicked in manual mode */
  codeSent = false;

  form = this.fb.group({
    phoneNumber: ['', [
      Validators.required,
      Validators.pattern(/^254[17]\d{8}$/),
    ]],
    code: ['', [
      Validators.required,
      Validators.pattern(/^[0-9]{6}$/),
    ]],
  });

  ngOnInit(): void {
    if (this.manualPhoneEntry) {
      // Manual mode: phone field is used, code field disabled until code sent
      this.form.controls.code.disable();
    } else {
      // Magic-link mode: phone comes from the service, no phone input
      this.form.controls.phoneNumber.clearValidators();
      this.form.controls.phoneNumber.updateValueAndValidity();
    }
  }

  /** Manual mode only — request an OTP via SMS/email */
  sendCode(): void {
    const phone = this.form.controls.phoneNumber.value ?? '';
    if (this.form.controls.phoneNumber.invalid) {
      this.form.controls.phoneNumber.markAsTouched();
      return;
    }

    this.state.submitting.set(true);
    this.state.errorMessage.set(null);

    this.api.requestOtp('activation', { phoneNumber: phone }).subscribe({
      next: () => {
        this.codeSent = true;
        this.form.controls.code.enable();
        this.state.phoneNumber.set(phone);
        this.state.submitting.set(false);
      },
      error: err => {
        this.state.submitting.set(false);
        this.state.errorMessage.set(
          err?.error?.message ?? 'Could not send code. Try again.',
        );
      },
    });
  }

  /** Both modes — submit the code to /otp/verify/activation */
  submitCode(): void {
    if (this.form.controls.code.invalid) {
      this.form.controls.code.markAsTouched();
      return;
    }

    const phone = this.state.phoneNumber();
    const code = this.form.controls.code.value ?? '';

    if (!phone) {
      this.state.fail('Missing phone number. Please reload the page.');
      return;
    }

    this.state.submitting.set(true);
    this.state.errorMessage.set(null);

    this.api.verifyOtp('activation', { phoneNumber: phone, code }).subscribe({
      next: res => {
        this.state.submitting.set(false);
        this.state.handleOtpVerified(res);
      },
      error: err => {
        this.state.submitting.set(false);
        this.state.errorMessage.set(
          err?.error?.message ?? 'Invalid or expired code.',
        );
      },
    });
  }

  /** Auto-submit when the 6th digit is typed (quality-of-life touch) */
  onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 6);
    input.value = digits;
    this.form.controls.code.setValue(digits);

    if (digits.length === 6) {
      this.submitCode();
    }
  }
}