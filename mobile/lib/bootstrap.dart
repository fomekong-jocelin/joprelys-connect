import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/app.dart';
import 'core/network/api_client_providers.dart';
import 'features/auth/application/auth_providers.dart';

Future<void> bootstrap() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Future.wait([
    initializeDateFormatting('fr_FR'),
    initializeDateFormatting('en_GB'),
  ]);
  runApp(
    ProviderScope(
      overrides: [
        apiSessionAccessProvider.overrideWith(
          (ref) => ref.watch(authSessionManagerProvider),
        ),
      ],
      child: const JoprelysApp(),
    ),
  );
}
