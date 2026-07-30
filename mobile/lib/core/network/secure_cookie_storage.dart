import 'dart:convert';

import 'package:cookie_jar/cookie_jar.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final class SecureCookieStorage extends Storage {
  SecureCookieStorage(this._storage, {this.namespace = 'joprelys.cookie'});

  final FlutterSecureStorage _storage;
  final String namespace;
  Future<void> _serial = Future<void>.value();

  String get _indexKey => '$namespace.__index';

  @override
  Future<void> init(bool persistSession, bool ignoreExpires) async {}

  @override
  Future<String?> read(String key) async {
    await _serial;
    return _storage.read(key: _storageKey(key));
  }

  @override
  Future<void> write(String key, String value) {
    return _enqueue(() async {
      final storageKey = _storageKey(key);
      final tracked = await _readTrackedKeys();
      await _storage.write(key: storageKey, value: value);
      if (tracked.add(storageKey)) {
        await _writeTrackedKeys(tracked);
      }
    });
  }

  @override
  Future<void> delete(String key) {
    return _enqueue(() async {
      final storageKey = _storageKey(key);
      final tracked = await _readTrackedKeys();
      await _storage.delete(key: storageKey);
      if (tracked.remove(storageKey)) {
        await _writeTrackedKeys(tracked);
      }
    });
  }

  @override
  Future<void> deleteAll(List<String> keys) {
    return _enqueue(() async {
      final tracked = await _readTrackedKeys();
      if (tracked.isEmpty) {
        tracked.addAll(keys.map(_storageKey));
      }
      await Future.wait(tracked.map((key) => _storage.delete(key: key)));
      await _storage.delete(key: _indexKey);
    });
  }

  Future<void> _enqueue(Future<void> Function() operation) {
    final result = _serial.then((_) => operation());
    _serial = result.catchError((Object _, StackTrace _) {});
    return result;
  }

  Future<Set<String>> _readTrackedKeys() async {
    final encoded = await _storage.read(key: _indexKey);
    if (encoded == null || encoded.isEmpty) {
      return <String>{};
    }
    try {
      final decoded = jsonDecode(encoded);
      if (decoded is! List) {
        return <String>{};
      }
      return decoded.whereType<String>().toSet();
    } on FormatException {
      return <String>{};
    }
  }

  Future<void> _writeTrackedKeys(Set<String> keys) {
    if (keys.isEmpty) {
      return _storage.delete(key: _indexKey);
    }
    final ordered = keys.toList()..sort();
    return _storage.write(key: _indexKey, value: jsonEncode(ordered));
  }

  String _storageKey(String key) {
    final encoded = base64Url.encode(utf8.encode(key)).replaceAll('=', '');
    return '$namespace.$encoded';
  }
}
