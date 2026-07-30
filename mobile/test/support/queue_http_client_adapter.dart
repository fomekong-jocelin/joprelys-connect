import 'dart:async';
import 'dart:typed_data';

import 'package:dio/dio.dart';

typedef QueueResponseHandler =
    FutureOr<ResponseBody> Function(RequestOptions options, int callIndex);

final class QueueHttpClientAdapter implements HttpClientAdapter {
  QueueHttpClientAdapter(this._handler);

  final QueueResponseHandler _handler;
  final List<RequestOptions> requests = [];
  bool closed = false;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    return _handler(options, requests.length - 1);
  }

  @override
  void close({bool force = false}) {
    closed = true;
  }
}
