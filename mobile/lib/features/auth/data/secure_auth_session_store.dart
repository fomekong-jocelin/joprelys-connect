import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../domain/professional_session.dart';
import 'auth_session_store.dart';

final class SecureAuthSessionStore implements AuthSessionStore {
  const SecureAuthSessionStore(
    this._storage, {
    this.storageKey = 'joprelys.auth.professional_session.v1',
  });

  final FlutterSecureStorage _storage;
  final String storageKey;

  @override
  Future<ProfessionalSession?> read() async {
    final encoded = await _storage.read(key: storageKey);
    if (encoded == null || encoded.trim().isEmpty) {
      return null;
    }

    try {
      final decoded = jsonDecode(encoded);
      if (decoded is! Map) {
        throw const FormatException('Invalid professional session payload');
      }
      return ProfessionalSession.fromJson(
        decoded.map((key, value) => MapEntry(key.toString(), value)),
      );
    } on FormatException {
      await clear();
      return null;
    }
  }

  @override
  Future<void> write(ProfessionalSession session) {
    return _storage.write(key: storageKey, value: jsonEncode(session.toJson()));
  }

  @override
  Future<void> clear() {
    return _storage.delete(key: storageKey);
  }
}
