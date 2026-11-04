import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/security/local_session_data_purger.dart';
import 'clinical_transcript_draft_store.dart';
import 'lab_result_pdf_temporary_store.dart';

/// Clinical data the dashboard keeps on the device, erased at session end.
final dashboardLocalSessionDataPurgersProvider =
    Provider<List<LocalSessionDataPurger>>((ref) {
      return <LocalSessionDataPurger>[
        // Same storage configuration as the dictation services use.
        const SecureClinicalTranscriptDraftStore(),
        ref.watch(labResultPdfTemporaryStoreProvider),
      ];
    });
