import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/i18n/locale_controller.dart';
import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../../../core/network/api_request_interceptor.dart';
import '../../../core/network/api_session.dart';
import '../../../core/security/secure_storage.dart';
import '../data/auth_api.dart';
import '../data/auth_session_store.dart';
import '../data/secure_auth_session_store.dart';
import 'auth_session_manager.dart';
import 'biometric_authenticator.dart';

final authSessionStoreProvider = Provider<AuthSessionStore>((ref) {
  return SecureAuthSessionStore(ref.watch(secureStorageProvider));
});

final storedAuthSessionAccessProvider = Provider<ApiSessionAccess>((ref) {
  return StoredApiSessionAccess(ref.watch(authSessionStoreProvider));
});

final authApiDioProvider = Provider<Dio>((ref) {
  final config = ref.watch(apiNetworkConfigProvider);
  final dio = Dio(
    BaseOptions(
      baseUrl: config.baseUri.toString(),
      connectTimeout: config.connectTimeout,
      sendTimeout: config.sendTimeout,
      receiveTimeout: config.receiveTimeout,
      responseType: ResponseType.json,
    ),
  );

  dio.interceptors.add(CookieManager(ref.watch(apiCookieJarProvider)));
  dio.interceptors.add(
    ApiRequestInterceptor(
      sessionAccess: ref.watch(storedAuthSessionAccessProvider),
      localeReader: () => ref.read(appLocaleProvider),
      traceIdFactory: ref.watch(traceIdFactoryProvider),
    ),
  );

  ref.onDispose(() => dio.close(force: true));
  return dio;
});

final authApiProvider = Provider<AuthApi>((ref) {
  return AuthApi(
    ApiClient(
      dio: ref.watch(authApiDioProvider),
      exceptionMapper: ref.watch(apiExceptionMapperProvider),
    ),
  );
});

final authSessionManagerProvider = Provider<AuthSessionManager>((ref) {
  return AuthSessionManager(
    store: ref.watch(authSessionStoreProvider),
    authApi: ref.watch(authApiProvider),
    cookieJar: ref.watch(apiCookieJarProvider),
  );
});

final biometricAuthenticatorProvider = Provider<BiometricAuthenticator>((ref) {
  return LocalBiometricAuthenticator();
});
