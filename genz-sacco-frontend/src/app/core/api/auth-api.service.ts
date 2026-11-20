import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActivationContext,
  AuthTokenResponse,
  LoginRequest,
  OtpRequestPayload,
  OtpVerifyRequest,
  PasswordSetRequest,
  PinSetRequest,
} from '../models/auth.models';

/**
 * Purpose suffix that appears in the URL: /otp/request/{purpose}
 * and /otp/verify/{purpose}. Matches the backend's controller paths.
 */
export type OtpPurposePath = 'activation' | 'password-reset' | 'pin-reset';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/auth';

  // ==========================================================
  // Magic link
  // ==========================================================

  /**
   * GET /api/v1/auth/verify?token=...
   *
   * Called on page load when the URL contains ?token=...
   * Returns { valid, phoneNumber, maskedEmail, expiresAt, reason? }
   */
  verifyLink(token: string): Observable<ActivationContext> {
    const params = new HttpParams().set('token', token);
    return this.http.get<ActivationContext>(`${this.base}/verify`, { params });
  }

  // ==========================================================
  // OTP
  // ==========================================================

  /**
   * POST /api/v1/auth/otp/request/{purpose}
   *
   * Body: { phoneNumber }
   * Returns 202 with an empty body.
   */
  requestOtp(
    purpose: OtpPurposePath,
    payload: OtpRequestPayload,
  ): Observable<void> {
    return this.http.post<void>(`${this.base}/otp/request/${purpose}`, payload);
  }

  /**
   * POST /api/v1/auth/otp/verify/{purpose}
   *
   * Body: { phoneNumber, code }
   * Returns: { nextStep: 'SETUP_PASSWORD', setupToken } on success
   *          { nextStep: 'SETUP_PIN', setupToken }      for PIN reset
   *          { nextStep: 'FULL_ACCESS', ... }           if already has PIN
   */
  verifyOtp(
    purpose: OtpPurposePath,
    payload: OtpVerifyRequest,
  ): Observable<AuthTokenResponse> {
    return this.http.post<AuthTokenResponse>(
      `${this.base}/otp/verify/${purpose}`,
      payload,
    );
  }

  // ==========================================================
  // Credential setup
  // ==========================================================

  /**
   * POST /api/v1/auth/password/set
   *
   * Body: { setupToken, password }
   * Returns: { nextStep: 'SETUP_PIN', setupToken } or
   *          { nextStep: 'FULL_ACCESS', ... }
   */
  setPassword(payload: PasswordSetRequest): Observable<AuthTokenResponse> {
    return this.http.post<AuthTokenResponse>(
      `${this.base}/password/set`,
      payload,
    );
  }

  /**
   * POST /api/v1/auth/pin/set
   *
   * Body: { setupToken, pin }
   * Returns: { nextStep: 'FULL_ACCESS', accessToken, refreshToken }
   */
  setPin(payload: PinSetRequest): Observable<AuthTokenResponse> {
    return this.http.post<AuthTokenResponse>(`${this.base}/pin/set`, payload);
  }

  login(payload: LoginRequest): Observable<AuthTokenResponse>{
   return this.http.post<AuthTokenResponse>(`${this.base}/login`, payload)
  }
}