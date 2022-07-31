import 'dart:developer';

import 'package:flutter/services.dart';
import 'package:rxdart/rxdart.dart';

class WoosimBluetoothService{
  static const platform = MethodChannel('com.woosim_flutter.mobile_printer');

  final _woosimDeviceStreamController = BehaviorSubject<List<Map>>.seeded([]);
  Stream<List<Map>> connectedDevices() => _woosimDeviceStreamController.asBroadcastStream();

  Future<void> init() async{
    try{
      final result = await platform.invokeMethod('initWoosimBluetoothService');
      log('CHANNEL RESULT: ${result.toString()}');
    }on PlatformException catch(e){
      log(e.message.toString());
    }
  }

  Future<void> scanWoosimPrinter()async {
    try{
      final result = await platform.invokeMethod('scanWoosimDevice');
      log('SCAN RESULT: ${result.toString()}');
      final devices = result as List<dynamic>;
      final finalData = devices.cast<Map>().map((e) => e).toList();
      _woosimDeviceStreamController.add(finalData);
    }on PlatformException catch(e){
      log(e.message.toString());
    }
  }

  Future<void> connect (String address)async {
    try{
      final result = await platform.invokeMethod('connectToPrinter', {'address': address});
    }on PlatformException catch(e){
      log(e.message.toString());
    }
  }

  Future<void> print(String data)async {
    try{
      final result = await platform.invokeMethod('textPrint', {'data': data});
      log('PRINT RESULT: ${result.toString()}');
    }on PlatformException catch(e){
      log(e.message.toString());
    }
  }
}