import 'dart:io';
import 'package:bascat/bascat.dart' as bascat;

void main(List<String> arguments) async {
  if (arguments.isEmpty) {
    stderr.writeln('Usage: bascat <filename>');
    exit(1);
  }
  var srcData = await File(arguments.first).readAsBytes();
  bascat.decodeGwBas(srcData).forEach(print);
}
