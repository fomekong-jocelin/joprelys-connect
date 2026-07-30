import 'package:dio/dio.dart';
import 'package:flutter/widgets.dart';

import 'api_headers.dart';
import 'api_request_policy.dart';
import 'api_session.dart';
import 'trace_id_factory.dart';

final class ApiRequestInterceptor extends Interceptor {
  ApiRequestInterceptor({
    required ApiSessionAccess sessionAccess,
    required Locale Function() localeReader,
    required TraceIdFactory traceIdFactory,
    DateTime Function()? clock,
  }) : _sessionAccess = sessionAccess,
       _localeReader = localeReader,
       _traceIdFactory = traceIdFactory,
       _clock = clock ?? DateTime.now;

  final ApiSessionAccess _sessionAccess;
  final Locale Function() _localeReader;
  final TraceIdFactory _traceIdFactory;
  final DateTime Function() _clock;

  @override
  void onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final policy = _policy(options);
    options.headers.putIfAbsent(ApiHeaders.accept, () => ApiHeaders.json);
    options.headers.putIfAbsent(
      ApiHeaders.acceptLanguage,
      () => _localeReader().languageCode,
    );
    options.headers.putIfAbsent(ApiHeaders.traceId, _traceIdFactory.create);

    if (options.data != null) {
      options.headers.putIfAbsent(
        ApiHeaders.contentType,
        () => ApiHeaders.json,
      );
    }

    final idempotencyKey = policy.idempotencyKey?.trim();
    if (idempotencyKey != null && idempotencyKey.isNotEmpty) {
      options.headers.putIfAbsent(
        ApiHeaders.idempotencyKey,
        () => idempotencyKey,
      );
    }

    if (policy.authenticationMode == ApiAuthenticationMode.none) {
      options.headers.remove(ApiHeaders.authorization);
      handler.next(options);
      return;
    }

    final session = await _sessionAccess.current();
    if (session?.hasUsableAccessToken(_clock()) == true) {
      options.headers[ApiHeaders.authorization] =
          'Bearer ${session!.accessToken!.trim()}';
    }
    handler.next(options);
  }

  ApiRequestPolicy _policy(RequestOptions options) {
    final value = options.extra[ApiRequestExtraKeys.policy];
    return value is ApiRequestPolicy
        ? value
        : const ApiRequestPolicy.publicRequest();
  }
}
