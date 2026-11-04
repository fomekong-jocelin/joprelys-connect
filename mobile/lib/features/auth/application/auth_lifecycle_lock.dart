import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/lifecycle/app_activity_registry.dart';
import '../presentation/pages/unlock_page.dart';
import 'auth_controller.dart';

class AuthLifecycleLock extends ConsumerStatefulWidget {
  const AuthLifecycleLock({required this.child, super.key});

  final Widget child;

  @override
  ConsumerState<AuthLifecycleLock> createState() => _AuthLifecycleLockState();
}

class _AuthLifecycleLockState extends ConsumerState<AuthLifecycleLock>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached) {
      ref.read(authControllerProvider.notifier).lock();
    }
  }

  @override
  Widget build(BuildContext context) {
    final authStatus = ref.watch(authControllerProvider).value?.status;
    final activities = ref.watch(appForegroundActivityProvider);
    final preserveClinicalCapture =
        authStatus == AuthStatus.locked && activities.preservesRouteOnLock;

    // The Stack is always present so that locking never re-parents the router
    // subtree: the clinical capture underneath keeps its state.
    return Stack(
      fit: StackFit.expand,
      children: [
        widget.child,
        if (preserveClinicalCapture)
          const Positioned.fill(child: BlockSemantics(child: _LockSurface())),
      ],
    );
  }
}

/// Unlock screen drawn above the router. It owns a [Navigator] (and thus an
/// [Overlay]) because tooltips and menus of the auth shell need one, and the
/// app builder sits above the router's navigator.
class _LockSurface extends StatelessWidget {
  const _LockSurface();

  @override
  Widget build(BuildContext context) {
    return HeroControllerScope.none(
      child: Navigator(
        onGenerateRoute: (settings) => PageRouteBuilder<void>(
          settings: settings,
          pageBuilder: (context, animation, secondaryAnimation) => Material(
            color: Theme.of(context).colorScheme.surface,
            child: const UnlockPage(),
          ),
        ),
      ),
    );
  }
}
