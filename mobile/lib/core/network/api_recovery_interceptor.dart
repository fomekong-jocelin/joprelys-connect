import 'dart:async';

import 'package:dio/dio.dart';

import 'api_headers.dart';
import 'api_request_policy.dart';
import 'api_session.dart';
import 'session_refresh_coordinator.dart';

final class ApiRecoveryInterceptor extends Interceptor {
  ApiRecoveryInterceptor({
    required Dio dio,
    required ApiSessionAccess sessionAccess,
    required SessionRefreshCoordinator refreshCoordinator,
    DateTime Function()? clock,
    Duration transientRetryDelay = const Duration(milliseconds: 250),
  }) : _dio = dio,
       _sessionAccess = sessionAccess,
       _refreshCoordinator = refreshCoordinator,
       _clock = clock ?? DateTime.now,
       _transientRetryDelay = transientRetryDelay;

  final Dio _dio;
  final ApiSessionAccess _sessionAccess;
  final SessionRefreshCoordinator _refreshCoordinator;
  final DateTime Function() _clock;
  final Duration _transientRetryDelay;

  @override
  void onError(DioException error, ErrorInterceptorHandler handler) async {
    if (await _trySessionRecovery(error, handler)) {
      return;
    }
    if (await _tryTransientRetry(error, handler)) {
      return;
    }
    handler.next(error);
  }

  Future<bool> _trySessionRecovery(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final request = error.requestOptions;
    final policy = _policy(request);
    if (!_canRecoverSession(error, request, policy)) {
      return false;
    }

    final session = await _sessionAccess.current();
    if (session == null) {
      return false;
    }
    if (!session.canRefresh) {
      await _sessionAccess.expire();
      return false;
    }

    final rejectedToken = _bearerToken(request);
    if (_hasDifferentUsableToken(session, rejectedToken)) {
      await _resolveRetry(
        request,
        handler,
        accessToken: session.accessToken!,
        extraKey: ApiRequestExtraKeys.authRetryCount,
      );
      return true;
    }

    try {
      final refreshed = await _refreshCoordinator.refresh(
        _sessionAccess,
        rejectedAccessToken: rejectedToken,
      );
      if (refreshed?.hasUsableAccessToken(_clock()) != true) {
        await _sessionAccess.expire();
        return false;
      }
      await _resolveRetry(
        request,
        handler,
        accessToken: refreshed!.accessToken!,
        extraKey: ApiRequestExtraKeys.authRetryCount,
      );
      return true;
    } on DioException catch (refreshError) {
      if (_isAuthenticationRefusal(refreshError)) {
        await _sessionAccess.expire();
      }
      handler.next(refreshError);
      return true;
    } catch (failure) {
      await _sessionAccess.expire();
      handler.next(
        DioException(
          requestOptions: request,
          type: DioExceptionType.unknown,
          error: failure,
        ),
      );
      return true;
    }
  }

  Future<bool> _tryTransientRetry(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final request = error.requestOptions;
    final policy = _policy(request);
    final retryCount = _retryCount(
      request,
      ApiRequestExtraKeys.transientRetryCount,
    );
    if (retryCount >= 1 ||
        !policy.allowsTransientRetry(request.method) ||
        !_isReplayable(request.data) ||
        !_isTransient(error)) {
      return false;
    }

    await Future<void>.delayed(_transientRetryDelay);
    try {
      final response = await _dio.fetch<dynamic>(
        request.copyWith(
          extra: {
            ...request.extra,
            ApiRequestExtraKeys.transientRetryCount: retryCount + 1,
          },
        ),
      );
      handler.resolve(response);
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
    return true;
  }

  bool _canRecoverSession(
    DioException error,
    RequestOptions request,
    ApiRequestPolicy policy,
  ) {
    return error.response?.statusCode == 401 &&
        policy.authenticationMode != ApiAuthenticationMode.none &&
        policy.recoverProfessionalSession &&
        _retryCount(request, ApiRequestExtraKeys.authRetryCount) < 1 &&
        !request.path.startsWith('/api/auth/');
  }

  bool _hasDifferentUsableToken(
    ApiSessionSnapshot session,
    String? rejectedToken,
  ) {
    return session.hasUsableAccessToken(_clock()) &&
        session.accessToken != rejectedToken;
  }

  Future<void> _resolveRetry(
    RequestOptions request,
    ErrorInterceptorHandler handler, {
    required String accessToken,
    required String extraKey,
  }) async {
    final retryCount = _retryCount(request, extraKey);
    final headers = Map<String, dynamic>.from(request.headers)
      ..[ApiHeaders.authorization] = 'Bearer $accessToken';
    try {
      final response = await _dio.fetch<dynamic>(
        request.copyWith(
          headers: headers,
          extra: {...request.extra, extraKey: retryCount + 1},
        ),
      );
      handler.resolve(response);
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }

  ApiRequestPolicy _policy(RequestOptions request) {
    final value = request.extra[ApiRequestExtraKeys.policy];
    return value is ApiRequestPolicy
        ? value
        : const ApiRequestPolicy.publicRequest();
  }

  int _retryCount(RequestOptions request, String key) {
    final value = request.extra[key];
    return value is int ? value : 0;
  }

  String? _bearerToken(RequestOptions request) {
    final header = request.headers[ApiHeaders.authorization];
    if (header is! String || !header.startsWith('Bearer ')) {
      return null;
    }
    final token = header.substring('Bearer '.length).trim();
    return token.isEmpty ? null : token;
  }

  bool _isAuthenticationRefusal(DioException error) {
    final status = error.response?.statusCode;
    return status == 401 || status == 403;
  }

  bool _isTransient(DioException error) {
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.sendTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return true;
    }
    final status = error.response?.statusCode;
    return status == 408 || status == 502 || status == 503 || status == 504;
  }

  bool _isReplayable(Object? data) {
    return data is! Stream && data is! FormData;
  }
}
