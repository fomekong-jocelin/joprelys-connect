abstract final class AppRouteName {
  static const String foundation = 'foundation';
  static const String authLoading = 'auth-loading';
  static const String login = 'login';
  static const String otp = 'otp';
  static const String unlock = 'unlock';
  static const String recovery = 'recovery';
}

abstract final class AppRoutePath {
  static const String foundation = '/';
  static const String authLoading = '/auth/loading';
  static const String login = '/auth/login';
  static const String otp = '/auth/otp';
  static const String unlock = '/auth/unlock';
  static const String recovery = '/auth/recovery';

  static bool isAuthPath(String path) => path.startsWith('/auth/');
}
