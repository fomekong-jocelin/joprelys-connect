import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import '../../../core/security/local_session_data_purger.dart';

final labResultPdfTemporaryStoreProvider = Provider<LabResultPdfTemporaryStore>(
  (ref) => LabResultPdfTemporaryStore(),
);

/// Downloaded laboratory result PDFs, kept out of backed-up storage.
///
/// Files live in a dedicated folder of the OS temporary directory, which the
/// system may reclaim at any time, and the folder is erased at session end.
final class LabResultPdfTemporaryStore implements LocalSessionDataPurger {
  LabResultPdfTemporaryStore({Future<Directory> Function()? temporaryDirectory})
    : _temporaryDirectory = temporaryDirectory ?? getTemporaryDirectory;

  static const String folderName = 'joprelys-lab-results';

  final Future<Directory> Function() _temporaryDirectory;

  Future<File> save({
    required String resultId,
    required String resultNumber,
    required List<int> bytes,
  }) async {
    final folder = await _folder();
    await folder.create(recursive: true);
    final rawName = resultNumber.trim().isEmpty
        ? 'lab-result-$resultId'
        : resultNumber.trim();
    final safeName = rawName.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '-');
    final file = File('${folder.path}${Platform.pathSeparator}$safeName.pdf');
    return file.writeAsBytes(bytes, flush: true);
  }

  @override
  Future<void> purgeLocalSessionData() async {
    final folder = await _folder();
    if (await folder.exists()) {
      await folder.delete(recursive: true);
    }
  }

  Future<Directory> _folder() async {
    final root = await _temporaryDirectory();
    return Directory('${root.path}${Platform.pathSeparator}$folderName');
  }
}
