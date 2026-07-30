import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

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
  Widget build(BuildContext context) => widget.child;
}
