export interface LoginRequest {
  readonly email: string;
  readonly password: string;
}

export interface LoginResponse {
  readonly accessToken: string;
  readonly tokenType: 'Bearer';
  readonly expiresAt: string;
  readonly email: string;
  readonly name: string;
  readonly role: string;
}

export interface AuthSession {
  readonly accessToken: string;
  readonly expiresAt: string;
  readonly email: string;
  readonly name: string;
  readonly role: string;
}
