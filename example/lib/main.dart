import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const MethodChannel _platform = MethodChannel('print_channel');

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
// Define a function to show dialogs in Flutter
  Future<void> showProgressDialog(BuildContext context, String message) async {
    return showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text("Printing Progress"),
          content: Text(message),
        );
      },
    );
  }

  _print() async {
    printWithUsb('Dagocloud');
  }

  Future<void> printWithUsb(String contentToPrint) async {
    try {
      await _platform.invokeMethod('printUsb', contentToPrint);
    } catch (e) {
      print('Error: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    // Listen for method channel events
    _platform.setMethodCallHandler((call) async {
      if (call.method == 'printProgress') {
        showProgressDialog(context, call.arguments);
      } else if (call.method == 'printResult') {
// Handle and display the result message
        final result = call.arguments as Map<String, String>;
        showDialog(
          context: context,
          builder: (context) {
            return AlertDialog(
              title: Text(result['status'] ?? ''),
              content: Text(result['message'] ?? ''),
            );
          },
        );
      }
    });
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              'Press the button to print',
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _print,
        tooltip: 'Print',
        child: const Icon(Icons.print),
      ),
    );
  }
}
