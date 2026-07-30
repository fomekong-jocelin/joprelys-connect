import '../../../core/network/api_session.dart';

final class ProfessionalSession {
  const ProfessionalSession({
    required this.accessToken,
    required this.expiresAt,
    required this.email,
    required this.name,
    required this.role,
    this.sessionExpiresAt,
    this.sessionId,
    this.biometricEnabled = false,
  });

  final String accessToken;
  final DateTime expiresAt;
  final DateTime? sessionExpiresAt;
  final String? sessionId;
  final String email;
  final String name;
  final String role;
  final bool biometricEnabled;

  bool hasUsableAccessToken(DateTime now) {
    return accessToken.trim().isNotEmpty && expiresAt.isAfter(now);
  }

  ApiSessionSnapshot toApiSnapshot() {
    return ApiSessionSnapshot(
      kind: ApiSessionKind.professional,
      accessToken: accessToken,
      accessTokenExpiresAt: expiresAt,
    );
  }

  ProfessionalSession copyWith({
    String? accessToken,
    DateTime? expiresAt,
    DateTime? sessionExpiresAt,
    String? sessionId,
    String? email,
    String? name,
    String? role,
    bool? biometricEnabled,
  }) {
    return ProfessionalSession(
      accessToken: accessToken ?? this.accessToken,
      expiresAt: expiresAt ?? this.expiresAt,
      sessionExpiresAt: sessionExpiresAt ?? this.sessionExpiresAt,
      sessionId: sessionId ?? this.sessionId,
      email: email ?? this.email,
      name: name ?? this.name,
      role: role ?? this.role,
      biometricEnabled: biometricEnabled ?? this.biometricEnabled,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'accessToken': accessToken,
      'expiresAt': expiresAt.toUtc().toIso8601String(),
      'sessionExpiresAt': sessionExpiresAt?.toUtc().toIso8601String(),
      'sessionId': sessionId,
      'email': email,
      'name': name,
      'role': role,
      'biometricEnabled': biometricEnabled,
    };
  }

  factory ProfessionalSession.fromJson(Map<String, dynamic> json) {
    return ProfessionalSession(
      accessToken: _requiredString(json, 'accessToken'),
      expiresAt: _requiredDate(json, 'expiresAt'),
      sessionExpiresAt: _optionalDate(json, 'sessionExpiresAt'),
      sessionId: _optionalString(json, 'sessionId'),
      email: _requiredString(json, 'email'),
      name: _requiredString(json, 'name'),
      role: _requiredString(json, 'role'),
      biometricEnabled: json['biometricEnabled'] == true,
    );
  }

  static String _requiredString(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value is! String || value.trim().isEmpty) {
      throw FormatException('Invalid professional session field: $key');
    }
    return value.trim();
  }

  static String? _optionalString(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value == null) {
      return null;
    }
    if (value is! String || value.trim().isEmpty) {
      throw FormatException('Invalid professional session field: $key');
    }
    return value.trim();
  }

  static DateTime _requiredDate(Map<String, dynamic> json, String key) {
    final value = _requiredString(json, key);
    final parsed = DateTime.tryParse(value);
    if (parsed == null) {
      throw FormatException('Invalid professional session date: $key');
    }
    return parsed.toUtc();
  }

  static DateTime? _optionalDate(Map<String, dynamic> json, String key) {
    final value = json[key];
    if (value == null) {
      return null;
    }
    if (value is! String) {
      throw FormatException('Invalid professional session date: $key');
    }
    final parsed = DateTime.tryParse(value);
    if (parsed == null) {
      throw FormatException('Invalid professional session date: $key');
    }
    return parsed.toUtc();
  }

  @override
  String toString() {
    return 'ProfessionalSession(email: $email, role: $role, '
        'expiresAt: $expiresAt, biometricEnabled: $biometricEnabled)';
  }
}

final class AuthExchangeResult {
  const AuthExchangeResult._({
    required this.requiresOtp,
    required this.email,
    this.session,
  });

  factory AuthExchangeResult.otpRequired(String email) {
    return AuthExchangeResult._(requiresOtp: true, email: email.trim());
  }

  factory AuthExchangeResult.authenticated(ProfessionalSession session) {
    return AuthExchangeResult._(
      requiresOtp: false,
      email: session.email,
      session: session,
    );
  }

  final bool requiresOtp;
  final String email;
  final ProfessionalSession? session;
}
