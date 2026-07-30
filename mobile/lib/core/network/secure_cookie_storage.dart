import 'dart:convert';

import 'package:cookie_jar/cookie_jar.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final class SecureCookieStorage extends Storage {
  const SecureCookieStorage(
    this._storage, {
    this.namespace = 'joprelys.cookie',
  });

  final FlutterSecureStorage _storage;
  final String namespace;

  @override
  Future<void> init(bool persistSession, bool ignoreExpires) async {}

  @override
  Future<String?> read(String key) {
    return _storage.read(key: _storageKey(key));
  }

  @override
  Future<void> write(String key, String value) {
    return _storage.write(key: _storageKey(key), value: value);
  }

  @override
  Future<void> delete(String key) {
    return _storage.delete(key: _storageKey(key));
  }

  @override
  Future<void> deleteAll(List<String> keys) async {
    await Future.wait(keys.map(delete));
  }

  String _storageKey(String key) {
    final encoded = base64Url.encode(utf8.encode(key)).replaceAll('=', '');
    return '$namespace.$encoded';
  }
}
