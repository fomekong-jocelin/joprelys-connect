import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/config/app_environment.dart';

void main() {
  test('accepts an HTTP development endpoint and normalizes trailing slash', () {
    final config = AppRuntimeConfig.fromValues(
      environmentName: 'dev',
      apiBaseUrl: 'http://10.0.2.2:8080/',
    );

    expect(config.environment, AppEnvironment.dev);
    expect(config.apiBaseUri.toString(), 'http://10.0.2.2:8080');
  });

  test('requires HTTPS for recette and production', () {
    expect(
      () => AppRuntimeConfig.fromValues(
        environmentName: 'recette',
        apiBaseUrl: 'http://recette.joprelys.com',
      ),
      throwsArgumentError,
    );

    final production = AppRuntimeConfig.fromValues(
      environmentName: 'prod',
      apiBaseUrl: 'https://joprelys.com/api-root/',
    );
    expect(production.apiBaseUri.toString(), 'https://joprelys.com/api-root');
  });

  test('rejects unknown environments and unsafe URL components', () {
    expect(
      () => AppRuntimeConfig.fromValues(
        environmentName: 'staging',
        apiBaseUrl: 'https://example.test',
      ),
      throwsArgumentError,
    );
    expect(
      () => AppRuntimeConfig.fromValues(
        environmentName: 'prod',
        apiBaseUrl: 'https://user:secret@example.test?token=unsafe',
      ),
      throwsArgumentError,
    );
  });
}
