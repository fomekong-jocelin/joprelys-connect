final class ApiNetworkConfig {
  const ApiNetworkConfig({
    required this.baseUri,
    this.connectTimeout = const Duration(seconds: 15),
    this.sendTimeout = const Duration(seconds: 30),
    this.receiveTimeout = const Duration(seconds: 30),
  });

  final Uri baseUri;
  final Duration connectTimeout;
  final Duration sendTimeout;
  final Duration receiveTimeout;
}
