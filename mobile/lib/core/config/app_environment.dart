enum AppEnvironment {
  dev,
  recette,
  prod;

  static AppEnvironment parse(String value) {
    return switch (value.trim().toLowerCase()) {
      'dev' => AppEnvironment.dev,
      'recette' => AppEnvironment.recette,
      'prod' => AppEnvironment.prod,
      _ => throw ArgumentError.value(
        value,
        'APP_ENV',
        'Valeur attendue : dev, recette ou prod.',
      ),
    };
  }

  bool get requiresHttps => this != AppEnvironment.dev;
}

final class AppRuntimeConfig {
  const AppRuntimeConfig({required this.environment, required this.apiBaseUri});

  final AppEnvironment environment;
  final Uri apiBaseUri;

  factory AppRuntimeConfig.fromValues({
    required String environmentName,
    required String apiBaseUrl,
  }) {
    final environment = AppEnvironment.parse(environmentName);
    final uri = _normalizeApiBaseUri(apiBaseUrl);

    if (environment.requiresHttps && uri.scheme != 'https') {
      throw ArgumentError.value(
        apiBaseUrl,
        'API_BASE_URL',
        'HTTPS est obligatoire en recette et en production.',
      );
    }

    return AppRuntimeConfig(environment: environment, apiBaseUri: uri);
  }

  static Uri _normalizeApiBaseUri(String value) {
    final trimmed = value.trim();
    final uri = Uri.tryParse(trimmed);
    if (uri == null || !uri.hasScheme || uri.host.isEmpty) {
      throw ArgumentError.value(
        value,
        'API_BASE_URL',
        'Une URL API absolue est obligatoire.',
      );
    }
    if (uri.scheme != 'http' && uri.scheme != 'https') {
      throw ArgumentError.value(
        value,
        'API_BASE_URL',
        'Seuls HTTP et HTTPS sont acceptés.',
      );
    }
    if (uri.hasQuery || uri.hasFragment || uri.userInfo.isNotEmpty) {
      throw ArgumentError.value(
        value,
        'API_BASE_URL',
        'La base URL ne doit contenir ni identifiants, ni query, ni fragment.',
      );
    }

    final normalizedPath = uri.path == '/'
        ? ''
        : uri.path.replaceFirst(RegExp(r'/+$'), '');
    return uri.replace(path: normalizedPath);
  }
}
