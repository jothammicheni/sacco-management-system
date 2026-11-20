/**
 * TypeScript mirrors of the backend DTOs in saccoauthservice.
 * Field names must match the Java DTO field names exactly —
 * Jackson serializes them as-is unless @JsonProperty renames them.
 */

/** GET /api/v1/auth/verify response */
export interface ActivationContext {
  valid: boolean;
  userId?: string;
  maskedEmail?: string;
  phoneNumber?: string;
  expiresAt?: string;
  reason?: string;
}

/** POST /api/v1/auth/otp/verify/{purpose} request */
export interface OtpVerifyRequest {
  phoneNumber: string;
  code: string;
}

/** POST /api/v1/auth/otp/request/{purpose} request */
export interface OtpRequestPayload {
  phoneNumber: string;
}

/** Discriminator returned by every endpoint that yields tokens */
export type NextStep = 'FULL_ACCESS' | 'SETUP_PASSWORD' | 'SETUP_PIN';

/** Action string returned alongside nextStep (redundant but present) */
export type AuthAction = 'SET_PASSWORD' | 'SET_PIN' | 'FULL_ACCESS';

/**
 * Unified response from /otp/verify/*, /password/set, /pin/set.
 *
 * The backend uses `pinSetupToken` for BOTH the password-setup and the
 * PIN-setup token. That's confusing naming, but it's what the DTO declares.
 * `nextStep` tells you which step it's for.
 */
export interface AuthTokenResponse {
  nextStep: NextStep;
  action?: AuthAction;
  accessToken?: string | null;
  refreshToken?: string | null;
  expiresIn?: number | null;
  tokenType?: string | null;
  pinSetupToken?: string;
}

/** POST /api/v1/auth/password/set */
export interface PasswordSetRequest {
  setupToken: string;      // ← matches backend's PasswordSetDto.setupToken
  password: string;
}

/** POST /api/v1/auth/pin/set */
export interface PinSetRequest {
  setupToken: string;      // ← matches backend's PinSetDto.setupToken
  pin: string;
}

export interface LoginRequest{
  phoneNumber: string;
  password:  string;
}