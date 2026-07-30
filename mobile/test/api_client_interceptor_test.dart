import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/core/network/api_headers.dart';
import 'package:joprelys_mobile/core/network/api_recovery_interceptor.dart';
import 'package:joprelys_mobile/core/network/api_request_interceptor.dart';
import 'package:joprelys_mobile/core/network/api_request_policy.dart';
import 'package:joprelys_mobile/core/network/api_session.dart';
import 'package:joprelys_mobile/core/network/session_refresh_coordinator.dart';
import 'package:joprelys_mobile/core/network/trace_id_factory.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  test('adds locale, correlation and bearer headers', () async {
    final session = FakeSessionAccess.professional('access-1');
    final adapter = QueueHttpClientAdapter((options, callIndex) {
      return jsonResponse(200, '{"ok":true}');
    });
    final client = buildClient(adapter: adapter, sessionAccess: session);

    await client.get<Map<String, dynamic>>('/api/patients');

    final request = adapter.requests.single;
    expect(request.headers[ApiHeaders.accept], ApiHeaders.json);
    expect(request.headers[ApiHeaders.acceptLanguage], 'fr');
    expect(request.headers[ApiHeaders.traceId], 'trc_m_test');
    expect(request.headers[ApiHeaders.authorization], 'Bearer access-1');
  });

  test(
    'refreshes a professional session once and retries the request',
    () async {
      final session = FakeSessionAccess.professional('rejected-token')
        ..refreshedToken = 'fresh-token';
      final adapter = QueueHttpClientAdapter((options, callIndex) {
        final authorization = options.headers[ApiHeaders.authorization];
        return authorization == 'Bearer fresh-token'
            ? jsonResponse(200, '{"ok":true}')
            : jsonResponse(401, '{"detail":"AUTH_SESSION_INVALID"}');
      });
      final client = buildClient(adapter: adapter, sessionAccess: session);

      final response = await client.get<Map<String, dynamic>>('/api/visits');

      expect(response.statusCode, 200);
      expect(session.refreshCalls, 1);
      expect(adapter.requests, hasLength(2));
      expect(
        adapter.requests.last.headers[ApiHeaders.authorization],
        'Bearer fresh-token',
      );
    },
  );

  test('coordinates one refresh across concurrent 401 responses', () async {
    final session = FakeSessionAccess.professional('old-token')
      ..refreshedToken = 'new-token'
      ..refreshDelay = const Duration(milliseconds: 20);
    final adapter = QueueHttpClientAdapter((options, callIndex) {
      final authorization = options.headers[ApiHeaders.authorization];
      return authorization == 'Bearer new-token'
          ? jsonResponse(200, '{"ok":true}')
          : jsonResponse(401, '{"detail":"AUTH_SESSION_INVALID"}');
    });
    final client = buildClient(adapter: adapter, sessionAccess: session);

    await Future.wait([
      client.get<Map<String, dynamic>>('/api/patients'),
      client.get<Map<String, dynamic>>('/api/visits'),
    ]);

    expect(session.refreshCalls, 1);
    expect(adapter.requests, hasLength(4));
  });

  test('never refreshes on 403 authorization refusal', () async {
    final session = FakeSessionAccess.professional('access-1');
    final adapter = QueueHttpClientAdapter((options, callIndex) {
      return jsonResponse(
        403,
        '{"error":{"code":"ACCESS_DENIED","message":"Forbidden"}}',
      );
    });
    final client = buildClient(adapter: adapter, sessionAccess: session);

    await expectLater(
      client.get<Map<String, dynamic>>('/api/admin'),
      throwsA(
        isA<ApiException>().having(
          (error) => error.kind,
          'kind',
          ApiFailureKind.forbidden,
        ),
      ),
    );
    expect(session.refreshCalls, 0);
    expect(session.expireCalls, 0);
  });

  test(
    'expires a patient session on 401 without professional refresh',
    () async {
      final session = FakeSessionAccess.patient('patient-token');
      final adapter = QueueHttpClientAdapter((options, callIndex) {
        return jsonResponse(401, '{"detail":"Authentication is required"}');
      });
      final client = buildClient(adapter: adapter, sessionAccess: session);

      await expectLater(
        client.get<Map<String, dynamic>>('/api/patient/me'),
        throwsA(isA<ApiException>()),
      );
      expect(session.refreshCalls, 0);
      expect(session.expireCalls, 1);
      expect(adapter.requests, hasLength(1));
    },
  );

  test('retries one safe GET after a transient 503 response', () async {
    final session = FakeSessionAccess.professional('access-1');
    final adapter = QueueHttpClientAdapter((options, callIndex) {
      return callIndex == 0
          ? jsonResponse(503, '{"detail":"Temporary unavailable"}')
          : jsonResponse(200, '{"ok":true}');
    });
    final client = buildClient(adapter: adapter, sessionAccess: session);

    final response = await client.get<Map<String, dynamic>>('/api/queue');

    expect(response.statusCode, 200);
    expect(adapter.requests, hasLength(2));
    expect(
      adapter.requests.first.headers[ApiHeaders.traceId],
      adapter.requests.last.headers[ApiHeaders.traceId],
    );
  });

  test('does not retry a POST without an idempotency key', () async {
    final session = FakeSessionAccess.professional('access-1');
    final adapter = QueueHttpClientAdapter((options, callIndex) {
      return jsonResponse(503, '{"detail":"Temporary unavailable"}');
    });
    final client = buildClient(adapter: adapter, sessionAccess: session);

    await expectLater(
      client.post<Map<String, dynamic>>(
        '/api/visits',
        data: {'patientId': 'patient-1'},
      ),
      throwsA(isA<ApiException>()),
    );
    expect(adapter.requests, hasLength(1));
  });

  test(
    'adds idempotency header and retries an explicitly safe write',
    () async {
      final session = FakeSessionAccess.professional('access-1');
      final adapter = QueueHttpClientAdapter((options, callIndex) {
        return callIndex == 0
            ? jsonResponse(503, '{"detail":"Temporary unavailable"}')
            : jsonResponse(200, '{"ok":true}');
      });
      final client = buildClient(adapter: adapter, sessionAccess: session);

      await client.post<Map<String, dynamic>>(
        '/api/idempotent-operation',
        data: {'value': 1},
        policy: const ApiRequestPolicy.protectedWrite(
          idempotencyKey: 'idem-123',
        ),
      );

      expect(adapter.requests, hasLength(2));
      expect(
        adapter.requests.first.headers[ApiHeaders.idempotencyKey],
        'idem-123',
      );
    },
  );
}

ApiClient buildClient({
  required QueueHttpClientAdapter adapter,
  required ApiSessionAccess sessionAccess,
}) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  dio.interceptors.add(
    ApiRequestInterceptor(
      sessionAccess: sessionAccess,
      localeReader: () => const Locale('fr'),
      traceIdFactory: const FixedTraceIdFactory(),
      clock: () => DateTime.utc(2026, 7, 30),
    ),
  );
  dio.interceptors.add(
    ApiRecoveryInterceptor(
      dio: dio,
      sessionAccess: sessionAccess,
      refreshCoordinator: SessionRefreshCoordinator(),
      clock: () => DateTime.utc(2026, 7, 30),
      transientRetryDelay: Duration.zero,
    ),
  );
  return ApiClient(dio: dio, exceptionMapper: const ApiExceptionMapper());
}

ResponseBody jsonResponse(int statusCode, String body) {
  return ResponseBody.fromString(
    body,
    statusCode,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
      ApiHeaders.traceId: ['trc_backend_test'],
    },
  );
}

final class FixedTraceIdFactory implements TraceIdFactory {
  const FixedTraceIdFactory();

  @override
  String create() => 'trc_m_test';
}

final class FakeSessionAccess implements ApiSessionAccess {
  FakeSessionAccess._(this.snapshot);

  factory FakeSessionAccess.professional(String accessToken) {
    return FakeSessionAccess._(
      ApiSessionSnapshot(
        kind: ApiSessionKind.professional,
        accessToken: accessToken,
        accessTokenExpiresAt: DateTime.utc(2026, 7, 31),
      ),
    );
  }

  factory FakeSessionAccess.patient(String accessToken) {
    return FakeSessionAccess._(
      ApiSessionSnapshot(
        kind: ApiSessionKind.patient,
        accessToken: accessToken,
        accessTokenExpiresAt: DateTime.utc(2026, 7, 31),
      ),
    );
  }

  ApiSessionSnapshot? snapshot;
  String? refreshedToken;
  Duration refreshDelay = Duration.zero;
  int refreshCalls = 0;
  int expireCalls = 0;

  @override
  Future<ApiSessionSnapshot?> current() async => snapshot;

  @override
  Future<void> expire() async {
    expireCalls += 1;
    snapshot = null;
  }

  @override
  Future<ApiSessionSnapshot?> refresh({String? rejectedAccessToken}) async {
    refreshCalls += 1;
    if (refreshDelay > Duration.zero) {
      await Future<void>.delayed(refreshDelay);
    }
    final token = refreshedToken;
    if (token == null) {
      return null;
    }
    snapshot = ApiSessionSnapshot(
      kind: ApiSessionKind.professional,
      accessToken: token,
      accessTokenExpiresAt: DateTime.utc(2026, 7, 31),
    );
    return snapshot;
  }
}
