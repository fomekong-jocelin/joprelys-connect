import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/api_request_policy.dart';
import '../domain/professional_session.dart';

abstract interface class AuthGateway {
  Future<AuthExchangeResult> login({
    required String email,
    required String password,
  });

  Future<AuthExchangeResult> verifyOtp({
    required String email,
    required String otpCode,
  });

  Future<ProfessionalSession> refresh();

  Future<void> logout();
}

final class AuthApi implements AuthGateway {
  const AuthApi(this._client);

  final ApiClient _client;

  @override
  Future<AuthExchangeResult> login({
    required String email,
    required String password,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/auth/login',
      data: {'email': email.trim(), 'password': password},
      policy: const ApiRequestPolicy.publicRequest(),
    );
    return _parseExchange(response.data, fallbackEmail: email);
  }

  @override
  Future<AuthExchangeResult> verifyOtp({
    required String email,
    required String otpCode,
  }) async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/auth/verify-otp',
      data: {'email': email.trim(), 'otpCode': otpCode.trim()},
      policy: const ApiRequestPolicy.publicRequest(),
    );
    return _parseExchange(response.data, fallbackEmail: email);
  }

  @override
  Future<ProfessionalSession> refresh() async {
    final response = await _client.post<Map<String, dynamic>>(
      '/api/auth/refresh',
      data: const <String, dynamic>{},
      policy: const ApiRequestPolicy.publicRequest(),
    );
    final result = _parseExchange(response.data);
    final session = result.session;
    if (result.requiresOtp || session == null) {
      throw const ApiException(
        kind: ApiFailureKind.malformedResponse,
        code: 'AUTH_REFRESH_INVALID_RESPONSE',
        message: 'AUTH_REFRESH_INVALID_RESPONSE',
      );
    }
    return session;
  }

  @override
  Future<void> logout() async {
    await _client.post<void>(
      '/api/auth/logout',
      data: const <String, dynamic>{},
      policy: const ApiRequestPolicy(
        authenticationMode: ApiAuthenticationMode.required,
        retryMode: ApiRetryMode.never,
        recoverProfessionalSession: false,
      ),
    );
  }

  AuthExchangeResult _parseExchange(
    Object? payload, {
    String? fallbackEmail,
  }) {
    final data = _asMap(payload);
    final requiresOtp = data['requiresOtp'] == true;
    final email = _optionalString(data['email']) ?? fallbackEmail?.trim();

    if (requiresOtp) {
      if (email == null || email.isEmpty) {
        throw _malformed('AUTH_OTP_EMAIL_MISSING');
      }
      return AuthExchangeResult.otpRequired(email);
    }

    final accessToken = _requiredString(data, 'accessToken');
    final expiresAt = _requiredDate(data, 'expiresAt');
    final resolvedEmail = _requiredString(data, 'email');
    final name = _requiredString(data, 'name');
    final role = _requiredString(data, 'role');
    final tokenType = _requiredString(data, 'tokenType');
    if (tokenType.toLowerCase() != 'bearer') {
      throw _malformed('AUTH_TOKEN_TYPE_INVALID');
    }

    return AuthExchangeResult.authenticated(
      ProfessionalSession(
        accessToken: accessToken,
        expiresAt: expiresAt,
        sessionExpiresAt: _optionalDate(data['sessionExpiresAt']),
        sessionId: _optionalString(data['sessionId']),
        email: resolvedEmail,
        name: name,
        role: role,
      ),
    );
  }

  Map<String, dynamic> _asMap(Object? value) {
    if (value is Map<String, dynamic>) {
      return value;
    }
    if (value is Map) {
      return value.map((key, item) => MapEntry(key.toString(), item));
    }
    throw _malformed('AUTH_RESPONSE_NOT_OBJECT');
  }

  String _requiredString(Map<String, dynamic> data, String key) {
    final value = _optionalString(data[key]);
    if (value == null) {
      throw _malformed('AUTH_${key.toUpperCase()}_MISSING');
    }
    return value;
  }

  String? _optionalString(Object? value) {
    if (value == null) {
      return null;
    }
    if (value is! String || value.trim().isEmpty) {
      return null;
    }
    return value.trim();
  }

  DateTime _requiredDate(Map<String, dynamic> data, String key) {
    final value = _requiredString(data, key);
    final parsed = DateTime.tryParse(value);
    if (parsed == null) {
      throw _malformed('AUTH_${key.toUpperCase()}_INVALID');
    }
    return parsed.toUtc();
  }

  DateTime? _optionalDate(Object? value) {
    final text = _optionalString(value);
    if (text == null) {
      return null;
    }
    final parsed = DateTime.tryParse(text);
    if (parsed == null) {
      throw _malformed('AUTH_OPTIONAL_DATE_INVALID');
    }
    return parsed.toUtc();
  }

  ApiException _malformed(String code) {
    return ApiException(
      kind: ApiFailureKind.malformedResponse,
      code: code,
      message: code,
    );
  }
}
