package com.example.woosim_flutter;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String WOOSIM_FLUTTER_MOBILE_PRINTER = "com.woosim_flutter.mobile_printer";

    // Message types sent from the BluetoothPrintService Handler
    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_READ = 3;
    public static final String DEVICE_NAME = "device_name";

    private BluetoothAdapter bluetoothAdapter = null;
    private WoosimBTPrintService woosimBTPrintService = null;
    private WoosimService woosimService = null;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), WOOSIM_FLUTTER_MOBILE_PRINTER)
                .setMethodCallHandler((call, result) -> {
                    if(call.method.equals("initWoosimBluetoothService")){
                        System.out.println("INITIALIZING WOOSIM PRINTER");
                        if(bluetoothAdapter == null){
                            result.error("UNAVAILABLE", "Bluetooth not available", null);
                        }

                        if(!bluetoothAdapter.isEnabled()){
                            result.error("NOT_ENABLED", "Bluetooth not enabled", null);
                        }

                        if(woosimBTPrintService == null) InitializeWoosimBluetoothService();
                    }else if(call.method.equals("scanWoosimDevice")){
                        System.out.println("SCANNING FORM NATIVE");
                        List<Map<String, String>> devices = DoDiscovery();

                        result.success(devices);
                    }else if(call.method.equals("textPrint")){

                        final Map<String, Object> arguments = call.arguments();

                        try {
                            assert arguments != null;
                            String data = (String) arguments.get("data");

                            assert data != null;
                            PrintText(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else if(call.method.equals("connectToPrinter")){
                        final Map<String, Object> arguments = call.arguments();

                        assert arguments != null;
                        connectToPrinter(Objects.requireNonNull(arguments.get("address")).toString());
                    }
                });
    }


    private void InitializeWoosimBluetoothService(){
        woosimBTPrintService = new WoosimBTPrintService();
        woosimService = new WoosimService(mHandler);
    }

    private List<Map<String, String>> DoDiscovery(){
        System.out.println("DO DISCOVERY");
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        List<Map<String, String>> deviceList = new ArrayList<>();

        //if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            System.out.println("PERMISSION GRANTED");
            if(bluetoothAdapter.isDiscovering()){
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();

            Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();

            if(pairedDevice.size() > 0){
                for(BluetoothDevice device : pairedDevice){
                    Map<String, String> deviceMap = new HashMap<>();

                    deviceMap.put("name", device.getName());
                    deviceMap.put("address", device.getAddress());

                    deviceList.add(deviceMap);

                    BluetoothDevice sample = bluetoothAdapter.getRemoteDevice("00:15:0E:E8:CD:3A");
                    //woosimBTPrintService.connect(sample, false);
                    System.out.println("BT DEVICE: " + sample.getName().toUpperCase(Locale.ROOT) + " | ADDRESS: " + sample.getAddress());
                }
            }else{
                System.out.println("NO DEVICES FOUND");
            }
        //}
        return deviceList;
    }

    private void connectToPrinter(String address){
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        woosimBTPrintService.connect(device, false);
    }

    private void PrintText(String text) throws IOException {
        byte[] textByte = null;

        textByte = text.getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(WoosimCmd.setTextStyle(false, false, false, 1, 1));
        byteStream.write(textByte);
        byteStream.write(WoosimCmd.printData());

        sendData(WoosimCmd.initPrinter());
        sendData(byteStream.toByteArray());
    }

    private void sendData(byte[] data) {
        // Check that we're actually connected before trying printing
        if (woosimBTPrintService.getState() != WoosimBTPrintService.STATE_CONNECTED) {
            System.out.println("You are not connected to a device");
            return;
        }
        // Check that there's actually something to send
        if (data.length > 0) woosimBTPrintService.write(data);
    }

    // The Handler that gets information back from the BluetoothPrintService
    private final MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
               // Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                //Toast.makeText(getApplicationContext(), msg.getData().getInt(TOAST), Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_READ:
                woosimService.processRcvData((byte[])msg.obj, msg.arg1);
                break;
            case WoosimService.MESSAGE_PRINTER:
                if (msg.arg1 == WoosimService.MSR) {
                    if (msg.arg2 == 0) {
                        Toast.makeText(getApplicationContext(), "MSR reading failure", Toast.LENGTH_SHORT).show();
                    } else {
                        byte[][] track = (byte[][]) msg.obj;
                        if (track[0] != null) {
                            String str = new String(track[0]);
                            //mTrack1View.setText(str);
                        }
                        if (track[1] != null) {
                            String str = new String(track[1]);
                            //mTrack2View.setText(str);
                        }
                        if (track[2] != null) {
                            String str = new String(track[2]);
                            //mTrack3View.setText(str);
                        }
                    }
                }
                break;
        }
    }
}
