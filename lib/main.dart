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
  bool isModal = false;

  @override
  void initState() {
    super.initState();

    _platform.setMethodCallHandler((call) async {
      if (call.method == 'dismissDialog') {
        Navigator.of(context).pop();
      } else if (call.method == 'printProgress') {
        showProgressDialog(context, call.arguments);
      } else if (call.method == 'printResult') {
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
  }

  Future<void> showProgressDialog(BuildContext context, String message) async {
    return showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text("Printing Progress"),
          content: Text(message),
        );
      },
    );
  }

  _print() async {
    printWithUsb(
      """
        <font size='big'>COMMANDE N 045</font>
        ------------------------------------------
        <b>PACK BOMBA</b>[R]20 000 MGA
        <b>Qty:</b> 2
        ------------------------------------------
        <b>Coca 33cl</b>[R]3 000 MGA
        <b>Qty:</b> 4
        ------------------------------------------
        <b><font size='big'>TOTAL :[R] 42 000 MGA</font></b>
        ------------------------------------------
        Merci de votre visite!
      """,
    );
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
