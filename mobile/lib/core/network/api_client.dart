import 'package:dio/dio.dart';

import 'api_exception.dart';
import 'api_exception_mapper.dart';
import 'api_request_policy.dart';

final class ApiClient {
  ApiClient({required Dio dio, required ApiExceptionMapper exceptionMapper})
    : _dio = dio,
      _exceptionMapper = exceptionMapper;

  final Dio _dio;
  final ApiExceptionMapper _exceptionMapper;

  Future<Response<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? headers,
    ApiRequestPolicy policy = const ApiRequestPolicy.protectedRead(),
    CancelToken? cancelToken,
  }) {
    return request<T>(
      path,
      method: 'GET',
      queryParameters: queryParameters,
      headers: headers,
      policy: policy,
      cancelToken: cancelToken,
    );
  }

  Future<Response<T>> post<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? headers,
    ApiRequestPolicy policy = const ApiRequestPolicy.protectedWrite(),
    CancelToken? cancelToken,
  }) {
    return request<T>(
      path,
      method: 'POST',
      data: data,
      queryParameters: queryParameters,
      headers: headers,
      policy: policy,
      cancelToken: cancelToken,
    );
  }

  Future<Response<T>> put<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? headers,
    ApiRequestPolicy policy = const ApiRequestPolicy.protectedWrite(),
    CancelToken? cancelToken,
  }) {
    return request<T>(
      path,
      method: 'PUT',
      data: data,
      queryParameters: queryParameters,
      headers: headers,
      policy: policy,
      cancelToken: cancelToken,
    );
  }

  Future<Response<T>> patch<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? headers,
    ApiRequestPolicy policy = const ApiRequestPolicy.protectedWrite(),
    CancelToken? cancelToken,
  }) {
    return request<T>(
      path,
      method: 'PATCH',
      data: data,
      queryParameters: queryParameters,
      headers: headers,
      policy: policy,
      cancelToken: cancelToken,
    );
  }

  Future<Response<T>> delete<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? headers,
    ApiRequestPolicy policy = const ApiRequestPolicy.protectedWrite(),
    CancelToken? cancelToken,
  }) {
    return request<T>(
      path,
      method: 'DELETE',
      data: data,
      queryParameters: queryParameters,
      headers: headers,
      policy: policy,
      cancelToken: cancelToken,
    );
  }

  Future<Response<T>> request<T>(
    String path, {
    required String method,
    required ApiRequestPolicy policy,
    Object? data,
    Map<String, dynamic>? queryParameters,
    Map<String, dynamic>? headers,
    CancelToken? cancelToken,
    ResponseType? responseType,
  }) async {
    try {
      return await _dio.request<T>(
        path,
        data: data,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(
          method: method,
          headers: headers,
          responseType: responseType,
          extra: {ApiRequestExtraKeys.policy: policy},
        ),
      );
    } on DioException catch (error) {
      final mapped = error.error;
      if (mapped is ApiException) {
        throw mapped;
      }
      throw _exceptionMapper.map(error);
    }
  }
}
