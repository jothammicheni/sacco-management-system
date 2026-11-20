
import { Injectable, signal, inject } from "@angular/core";
import { AuthTokenResponse } from "../../core/models/auth.models";
import { AuthApiService } from "../../core/api/auth-api.service";
import { tap, catchError, finalize, throwError } from "rxjs";

@Injectable({ providedIn: "root" })
export class LoginService {
  readonly phoneNumber = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly step = signal<"idle" | "done" | "error">("idle");

  private api = inject(AuthApiService);

  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  login(phone: string, password: string) {
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.step.set("idle");

    return this.api.login({
      phoneNumber: phone,
      password,
    }).pipe(
      tap((res: AuthTokenResponse) => {
        if (res.nextStep === "FULL_ACCESS" && res.accessToken) {
          this.accessToken = res.accessToken;
          this.refreshToken = res.refreshToken ?? null;
          this.phoneNumber.set(phone);
          this.step.set("done");
        } else {
          this.fail("Login failed. Please check your credentials.");
        }
      }),

      catchError((err) => {
        this.fail(
          err?.error?.message ?? "Invalid phone number or password."
        );

        return throwError(() => err);
      }),

      finalize(() => {
        this.submitting.set(false);
      })
    );
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  getRefreshToken(): string | null {
    return this.refreshToken;
  }

  private fail(message: string): void {
    this.errorMessage.set(message);
    this.step.set("error");
  }

  reset(): void {
    this.step.set("idle");
    this.submitting.set(false);
    this.errorMessage.set(null);
    this.accessToken = null;
    this.refreshToken = null;
    this.phoneNumber.set(null);
  }
}
