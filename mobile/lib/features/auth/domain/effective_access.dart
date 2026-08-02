import 'package:flutter/foundation.dart';

@immutable
final class EffectiveAccess {
  const EffectiveAccess({
    required this.userId,
    this.roles = const <String>{},
    this.permissions = const <String>{},
  });

  final String userId;
  final Set<String> roles;
  final Set<String> permissions;

  bool hasPermission(String permission) {
    return permissions.contains(permission.trim().toUpperCase());
  }

  bool hasAnyPermission(Iterable<String> candidates) {
    return candidates.any(hasPermission);
  }

  factory EffectiveAccess.fromJson(Map<String, dynamic> json) {
    return EffectiveAccess(
      userId: (json['userId'] ?? '').toString().trim(),
      roles: _stringSet(json['roles']),
      permissions: _stringSet(json['permissions']),
    );
  }

  static Set<String> _stringSet(Object? value) {
    if (value is! List) {
      return const <String>{};
    }
    return Set<String>.unmodifiable(
      value
          .map((item) => item.toString().trim().toUpperCase())
          .where((item) => item.isNotEmpty),
    );
  }
}
