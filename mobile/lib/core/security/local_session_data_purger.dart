import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Local data owned by a feature that must never outlive the professional
/// session that produced it (clinical drafts, downloaded results…).
abstract interface class LocalSessionDataPurger {
  Future<void> purgeLocalSessionData();
}

/// Purgers run by the session manager whenever the local session is cleared.
///
/// The composition root (`bootstrap.dart`) overrides this provider with the
/// feature purgers so that `core` and `auth` never depend on feature code.
final localSessionDataPurgersProvider = Provider<List<LocalSessionDataPurger>>((
  ref,
) {
  return const <LocalSessionDataPurger>[];
});
