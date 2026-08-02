import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_connect/features/auth/domain/effective_access.dart';

void main() {
  test('normalizes and checks effective permissions', () {
    final access = EffectiveAccess.fromJson({
      'userId': 'user-1',
      'roles': ['doctor'],
      'permissions': ['patient_read', 'CLINICAL_READ'],
    });

    expect(access.userId, 'user-1');
    expect(access.roles, contains('DOCTOR'));
    expect(access.hasPermission('PATIENT_READ'), isTrue);
    expect(access.hasPermission('clinical_read'), isTrue);
    expect(access.hasPermission('AUDIT_READ'), isFalse);
  });

  test('handles missing permission arrays safely', () {
    final access = EffectiveAccess.fromJson({'userId': 'user-2'});

    expect(access.roles, isEmpty);
    expect(access.permissions, isEmpty);
    expect(access.hasAnyPermission(['PATIENT_READ', 'VISIT_READ']), isFalse);
  });
}
