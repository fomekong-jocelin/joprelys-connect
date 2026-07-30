import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/app_config.dart';
import '../i18n/locale_controller.dart';
import 'api_client.dart';
import 'api_exception_mapper.dart';
import 'api_network_config.dart';
import 'api_recovery_interceptor.dart';
import 'api_request_interceptor.dart';
import 'api_session.dart';
import 'session_refresh_coordinator.dart';
import 'trace_id_factory.dart';

final apiNetworkConfigProvider = Provider<ApiNetworkConfig>((ref) {
  return ApiNetworkConfig(baseUri: AppConfig.runtime.apiBaseUri);
});

final apiSessionAccessProvider = Provider<ApiSessionAccess>((ref) {
  return const AnonymousApiSessionAccess();
});

final apiCookieJarProvider = Provider<CookieJar>((ref) {
  // Le refresh professionnel actuel est un cookie HttpOnly. MOB-2804 le garde
  // uniquement en mémoire ; MOB-2805 fournira la persistance sécurisée.
  return CookieJar(ignoreExpires: false);
});

final apiExceptionMapperProvider = Provider<ApiExceptionMapper>((ref) {
  return const ApiExceptionMapper();
});

final traceIdFactoryProvider = Provider<TraceIdFactory>((ref) {
  return SecureTraceIdFactory();
});

final sessionRefreshCoordinatorProvider = Provider<SessionRefreshCoordinator>((
  ref,
) {
  return SessionRefreshCoordinator();
});

final apiDioProvider = Provider<Dio>((ref) {
  final config = ref.watch(apiNetworkConfigProvider);
  final sessionAccess = ref.watch(apiSessionAccessProvider);
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
      sessionAccess: sessionAccess,
      localeReader: () => ref.read(appLocaleProvider),
      traceIdFactory: ref.watch(traceIdFactoryProvider),
    ),
  );
  dio.interceptors.add(
    ApiRecoveryInterceptor(
      dio: dio,
      sessionAccess: sessionAccess,
      refreshCoordinator: ref.watch(sessionRefreshCoordinatorProvider),
    ),
  );

  ref.onDispose(() => dio.close(force: true));
  return dio;
});

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(
    dio: ref.watch(apiDioProvider),
    exceptionMapper: ref.watch(apiExceptionMapperProvider),
  );
});
