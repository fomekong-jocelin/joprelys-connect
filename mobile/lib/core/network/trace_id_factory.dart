import 'dart:math';

abstract interface class TraceIdFactory {
  String create();
}

final class SecureTraceIdFactory implements TraceIdFactory {
  SecureTraceIdFactory({Random? random}) : _random = random ?? Random.secure();

  final Random _random;

  @override
  String create() {
    final buffer = StringBuffer('trc_m_');
    for (var index = 0; index < 20; index++) {
      buffer.write(_random.nextInt(16).toRadixString(16));
    }
    return buffer.toString();
  }
}
