import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app/app.dart';
import 'core/network/api_client_providers.dart';
import 'core/security/local_session_data_purger.dart';
import 'features/auth/application/auth_providers.dart';
import 'features/dashboard/data/dashboard_local_session_data.dart';

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
        localSessionDataPurgersProvider.overrideWith(
          (ref) => ref.watch(dashboardLocalSessionDataPurgersProvider),
        ),
      ],
      child: const JoprelysApp(),
    ),
  );
}
