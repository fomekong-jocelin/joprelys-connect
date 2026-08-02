import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/lifecycle/app_activity_registry.dart';

void main() {
  test('clinical voice activity preserves and releases the locked route', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);
    final controller = container.read(appForegroundActivityProvider.notifier);

    expect(
      container.read(appForegroundActivityProvider).preservesRouteOnLock,
      isFalse,
    );

    controller.activate(AppForegroundActivity.clinicalVoiceAssistant);
    expect(
      container.read(appForegroundActivityProvider).preservesRouteOnLock,
      isTrue,
    );

    controller.deactivate(AppForegroundActivity.clinicalVoiceAssistant);
    expect(
      container.read(appForegroundActivityProvider).preservesRouteOnLock,
      isFalse,
    );
  });
}
