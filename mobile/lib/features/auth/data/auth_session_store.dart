import '../domain/professional_session.dart';

abstract interface class AuthSessionStore {
  Future<ProfessionalSession?> read();

  Future<void> write(ProfessionalSession session);

  Future<void> clear();
}
