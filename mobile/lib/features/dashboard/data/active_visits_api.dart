import '../../../core/network/api_client.dart';
import '../domain/active_visit.dart';

final class ActiveVisitsApi {
  const ActiveVisitsApi(this._client);

  final ApiClient _client;

  Future<List<ActiveVisit>> getActiveVisits() async {
    final response = await _client.get<dynamic>('/api/visits/active');
    final data = response.data;
    if (data is! List) {
      throw const FormatException('Invalid active visits response');
    }

    return data.map((item) {
      if (item is! Map) {
        throw const FormatException('Invalid active visit item');
      }
      return ActiveVisit.fromJson(Map<String, dynamic>.from(item));
    }).toList(growable: false);
  }
}
