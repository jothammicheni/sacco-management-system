import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
} from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from '../../../../core/api/auth-api.service';
import { ActivationService } from '../activation.service';

@Component({
  selector: 'app-credentials-step',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './credentials-step.html',
  styleUrl: './credentials-step.css',
})
export class CredentialsStepComponent {
  private fb = inject(FormBuilder);
  private api = inject(AuthApiService);
  readonly state = inject(ActivationService);

  form = this.fb.group(
    {
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/),
      ]],
      confirmPassword: ['', [Validators.required]],
      pin: ['', [
        Validators.required,
        Validators.pattern(/^\d{6}$/),
      ]],
      confirmPin: ['', [Validators.required]],
    },
    {
      validators: [
        this.matchFields('password', 'confirmPassword'),
        this.matchFields('pin', 'confirmPin'),
      ],
    },
  );

  /** Custom validator: two fields must match */
  private matchFields(a: string, b: string) {
    return (group: AbstractControl) => {
      const v1 = group.get(a)?.value;
      const v2 = group.get(b)?.value;
      if (v1 && v2 && v1 !== v2) {
        group.get(b)?.setErrors({ mismatch: true });
      } else {
        const errors = group.get(b)?.errors;
        if (errors?.['mismatch']) {
          const { mismatch, ...rest } = errors;
          group.get(b)?.setErrors(Object.keys(rest).length ? rest : null);
        }
      }
      return null;
    };
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const pwdToken = this.state.getPasswordSetupToken();
    if (!pwdToken) {
      this.state.fail('Your session expired. Please start over.');
      return;
    }

    this.state.submitting.set(true);
    this.state.errorMessage.set(null);

    try {
      // Step 1 — set password
      const pwdRes = await firstValueFrom(
        this.api.setPassword({
          setupToken: pwdToken,
          password: this.form.value.password!,
        }),
      );

      if (pwdRes.nextStep === 'FULL_ACCESS') {
        this.state.submitting.set(false);
        this.state.setStep('done');
        return;
      }

      if (!pwdRes.pinSetupToken) {
        this.state.fail('Server did not return a PIN setup token.');
        this.state.submitting.set(false);
        return;
      }

      // Step 2 — set PIN (using the fresh token from step 1)
      const pinRes = await firstValueFrom(
        this.api.setPin({
          setupToken: pwdRes.pinSetupToken,
          pin: this.form.value.pin!,
        }),
      );

      this.state.submitting.set(false);
      this.state.handlePinSet(pinRes);
    } catch (err: any) {
      this.state.submitting.set(false);
      const message =
        err?.error?.message ??
        err?.error?.error ??
        'Something went wrong. Please try again.';
      this.state.errorMessage.set(message);
    }
  }
}