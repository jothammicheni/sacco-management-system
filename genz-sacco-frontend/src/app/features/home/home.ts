
import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { AuthTokenResponse } from '../../core/models/auth.models';
import { LoginService } from './login.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
  ],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {

  private fb = inject(FormBuilder);
  private router = inject(Router);

  readonly login = inject(LoginService);

  readonly submitting = signal(false);

  form = this.fb.nonNullable.group({

    phoneNumber: [
      '',
      [
        Validators.required,
        Validators.pattern(/^254[17]\d{8}$/),
      ],
    ],

    password: [
      '',
      Validators.required,
    ],

  });


  submit(): void {

    console.log('LOGIN FORM SUBMITTED');

    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const { phoneNumber, password } = this.form.getRawValue();

    this.login.login(phoneNumber, password)
      .pipe(
        finalize(() => {
          this.submitting.set(false);
        })
      )
      .subscribe({

        next: (res: AuthTokenResponse) => {

          console.log('Login response:', res);

          if (res.nextStep === 'FULL_ACCESS' && res.accessToken) {
            this.router.navigate(['/dashboard']);
          }

        },

        error: (error) => {

          console.error('Login failed:', error);

          // Error message is managed by LoginService

        },

      });

  }

}
