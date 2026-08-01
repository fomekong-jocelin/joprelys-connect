import 'package:flutter/widgets.dart';

int applyAcceptedVitalValue(TextEditingController controller, num? value) {
  if (value == null) return 0;
  final next = _formatVitalValue(value);
  if (controller.text.trim() == next) return 0;
  controller.text = next;
  return 1;
}

String _formatVitalValue(num value) {
  final numeric = value.toDouble();
  return numeric == numeric.roundToDouble()
      ? numeric.toInt().toString()
      : numeric.toString();
}
