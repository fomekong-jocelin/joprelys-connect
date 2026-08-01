final class PatientDirectoryItem {
  const PatientDirectoryItem({
    required this.id,
    required this.dpu,
    required this.fullName,
    this.gender,
    this.birthDate,
    this.phone,
    this.bloodGroup,
    this.allergies,
    this.medicalHistory,
  });

  final String id;
  final String dpu;
  final String fullName;
  final String? gender;
  final DateTime? birthDate;
  final String? phone;
  final String? bloodGroup;
  final String? allergies;
  final String? medicalHistory;

  factory PatientDirectoryItem.fromJson(Map<String, dynamic> json) {
    final dpu =
        (json['localPatientNumber'] ??
                json['globalPatientNumber'] ??
                json['temporaryPatientNumber'] ??
                '')
            .toString()
            .trim();
    final name = (json['displayName'] ?? json['fullName'] ?? '')
        .toString()
        .trim();

    return PatientDirectoryItem(
      id: (json['id'] ?? '').toString(),
      dpu: dpu.isEmpty ? 'DPU-INCONNU' : dpu,
      fullName: name.isEmpty ? 'Patient sans nom' : name,
      gender: json['gender'] as String?,
      birthDate: json['birthDate'] != null
          ? DateTime.tryParse(json['birthDate'].toString())
          : null,
      phone: json['phone'] as String?,
      bloodGroup: json['bloodGroup'] as String?,
      allergies: json['allergies'] as String?,
      medicalHistory: json['medicalHistory'] as String?,
    );
  }
}
