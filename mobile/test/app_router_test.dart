import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/app/router/app_router.dart';
import 'package:joprelys_mobile/app/router/route_names.dart';

void main() {
  test('starts on the foundation route', () {
    final router = createAppRouter();
    addTearDown(router.dispose);

    expect(
      router.routeInformationProvider.value.uri.path,
      AppRoutePath.foundation,
    );
  });

  test('foundation route constants stay stable', () {
    expect(AppRouteName.foundation, 'foundation');
    expect(AppRoutePath.foundation, '/');
  });
}
