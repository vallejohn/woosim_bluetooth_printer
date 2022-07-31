import 'package:flutter/material.dart';
import 'package:woosim_flutter/woosim_bluetooth_service.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Woosim Mobile BT Printer Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {

  WoosimBluetoothService woosimBluetoothService = WoosimBluetoothService();

  String data = '';

  @override
  void initState() {

    initializePrinter();

    data = "\n"
        "Sales Receipt\n"
        "\n"
        "MERCHANT NAME                   woosim coffe\n"
        "MASTER                          Gil-dong Hon\n"
        "ADDRESS                  #501, Daerung Techn\n"
        "town 3rd 448,Gasan-don\n"
        "Gumcheon-gu Seoul Korea\n"
        "HELP DESK                    (+82-2)2107-3721\n"
        "\n"
        "---------------------------------------------\n"
        "Product       Sale                      Price\n"
        "---------------------------------------------\n"
        "Cafe mocha      2                        7.5\n"
        "Cafe latte      1                        7.0\n"
        "Cappuccino      1                        7.5\n"
        "---------------------------------------------\n"
        "Total                                   29.5\n"
        "---------------------------------------------\n"
        ""
        "                                  Thank you!\n"
        "\n"
        "\n"
        "\n";

    super.initState();
  }

  Future<void> initializePrinter()async {
    await woosimBluetoothService.init();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Woosim'),
        actions: [
          TextButton(onPressed: ()async => await woosimBluetoothService.scanWoosimPrinter(), child: const Text('SCAN', style: TextStyle(color: Colors.white),)),
          TextButton(onPressed: ()async => await woosimBluetoothService.print(data), child: const Text('PRINT', style: TextStyle(color: Colors.white),)),
        ],
      ),
      body: StreamBuilder<List<Map>>(
        stream: woosimBluetoothService.connectedDevices(),
        builder: (BuildContext context, AsyncSnapshot<List<Map>> snapshot) {
          if(snapshot.hasData){
            return ListView.builder(
              itemCount: snapshot.data!.length,
                itemBuilder: (context, index){
              return TextButton(
                onPressed: ()async => await woosimBluetoothService.connect(snapshot.data![index]['address'].toString()),
                child: ListTile(
                  title: snapshot.data![index]['name'].isNotEmpty? Text(snapshot.data![index]['name']) : Text('Unknown'),
                ),
              );
            });
          }
          return const Center(child: Text('Empty'),);
        },
      )
    );
  }
}
