import 'package:flutter_riverpod/flutter_riverpod.dart';

enum AppForegroundActivity { clinicalVoiceAssistant }

final appForegroundActivityProvider =
    NotifierProvider<
      AppForegroundActivityController,
      Set<AppForegroundActivity>
    >(AppForegroundActivityController.new);

class AppForegroundActivityController
    extends Notifier<Set<AppForegroundActivity>> {
  @override
  Set<AppForegroundActivity> build() => const <AppForegroundActivity>{};

  void activate(AppForegroundActivity activity) {
    if (state.contains(activity)) return;
    state = <AppForegroundActivity>{...state, activity};
  }

  void deactivate(AppForegroundActivity activity) {
    if (!state.contains(activity)) return;
    state = <AppForegroundActivity>{
      for (final current in state)
        if (current != activity) current,
    };
  }
}

extension AppForegroundActivitySet on Set<AppForegroundActivity> {
  bool get preservesRouteOnLock =>
      contains(AppForegroundActivity.clinicalVoiceAssistant);
}
