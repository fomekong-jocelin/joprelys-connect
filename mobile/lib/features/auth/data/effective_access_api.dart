import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../../../core/network/api_exception.dart';
import '../domain/effective_access.dart';

abstract interface class EffectiveAccessGateway {
  Future<EffectiveAccess> getMyAccess();
}

final effectiveAccessApiProvider = Provider<EffectiveAccessGateway>((ref) {
  return EffectiveAccessApi(ref.watch(apiClientProvider));
});

final class EffectiveAccessApi implements EffectiveAccessGateway {
  const EffectiveAccessApi(this._client);

  final ApiClient _client;

  @override
  Future<EffectiveAccess> getMyAccess() async {
    final response = await _client.get<dynamic>('/api/rbac/me');
    final payload = response.data;
    if (payload is Map<String, dynamic>) {
      return EffectiveAccess.fromJson(payload);
    }
    if (payload is Map) {
      return EffectiveAccess.fromJson(
        payload.map((key, value) => MapEntry(key.toString(), value)),
      );
    }
    throw const ApiException(
      kind: ApiFailureKind.malformedResponse,
      code: 'RBAC_EFFECTIVE_ACCESS_INVALID',
      message: 'RBAC_EFFECTIVE_ACCESS_INVALID',
    );
  }
}
