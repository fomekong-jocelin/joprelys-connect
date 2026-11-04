import 'dart:io';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/features/dashboard/data/clinical_transcript_draft_store.dart';
import 'package:joprelys_mobile/features/dashboard/data/lab_result_pdf_temporary_store.dart';

void main() {
  group('LabResultPdfTemporaryStore', () {
    late Directory temporaryRoot;
    late LabResultPdfTemporaryStore store;

    setUp(() async {
      temporaryRoot = await Directory.systemTemp.createTemp('joprelys-test-');
      store = LabResultPdfTemporaryStore(
        temporaryDirectory: () async => temporaryRoot,
      );
    });

    tearDown(() async {
      if (await temporaryRoot.exists()) {
        await temporaryRoot.delete(recursive: true);
      }
    });

    test('writes into a dedicated temporary folder with a safe name', () async {
      final file = await store.save(
        resultId: 'result-1',
        resultNumber: 'RES/2026 ../01',
        bytes: const [37, 80, 68, 70],
      );

      expect(file.existsSync(), isTrue);
      expect(file.parent.path, endsWith(LabResultPdfTemporaryStore.folderName));
      expect(file.parent.parent.path, temporaryRoot.path);
      expect(file.uri.pathSegments.last, 'RES-2026-..-01.pdf');
    });

    test('purge erases every downloaded result', () async {
      final file = await store.save(
        resultId: 'result-1',
        resultNumber: '',
        bytes: const [37, 80, 68, 70],
      );

      await store.purgeLocalSessionData();

      expect(file.existsSync(), isFalse);
      expect(file.parent.existsSync(), isFalse);
      await store.purgeLocalSessionData();
    });
  });

  test('draft purge erases every dictation draft and nothing else', () async {
    FlutterSecureStorage.setMockInitialValues(<String, String>{
      'joprelys.clinical-voice.draft.v3.visit-1': '{"version":3}',
      'joprelys.clinical-voice.draft.v3.visit-2': '{"version":3}',
      'joprelys.clinical-voice.draft.v2.visit-3': '{"version":2}',
      'joprelys.ui.theme': 'dark',
    });
    const storage = FlutterSecureStorage();

    await const SecureClinicalTranscriptDraftStore().purgeLocalSessionData();

    expect(await storage.readAll(), {'joprelys.ui.theme': 'dark'});
  });
}
