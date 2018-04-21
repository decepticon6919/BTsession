package com.example.sukanya.btsession;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Sukanya on 21-04-2018.
 */

public class MainActivity extends AppCompatActivity{

    ListView devicelist;
    ArrayList<BTdevice> devices;
    ArrayAdapter btDeviceAdapter;
    BluetoothAdapter _btAdapter;
    private final int REQUEST_ENABLE_BT = 2;

    private BTservice service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicelist=(ListView)findViewById(R.id.deviceList);
        devices = new ArrayList<>();
        service = new BTservice(getApplicationContext());
        btDeviceAdapter = new btDeviceArrayAdapter(this, android.R.layout.simple_list_item_1, devices);
        devicelist.setAdapter(btDeviceAdapter);

        _btAdapter=BluetoothAdapter.getDefaultAdapter();
        if(!_btAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        IntentFilter btEvents = new IntentFilter();
        btEvents.addAction(BluetoothDevice.ACTION_FOUND);
        btEvents.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        btEvents.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(_btReceiver, btEvents);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_ENABLE_BT && resultCode==RESULT_CANCELED)
            finish();
        if(requestCode==REQUEST_ENABLE_BT && resultCode==RESULT_OK){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Instructions To Begin Session:")
                    .setMessage("1. Ensure that the second device is Discoverable \n" +
                            "2. Start scanning for available devices in the first device \n" +
                            "3. Connect to the second device and proceed \n ")
                    .setNeutralButton("CLOSE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                Toast.makeText(getApplicationContext(),"Discovery Started",Toast.LENGTH_SHORT).show();
                startScan();
//                AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                builder.setView(R.layout.activity_main)
//                        .setTitle("Available Devices")
//                        .setAdapter(btDeviceAdapter, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                BluetoothDevice device = (BluetoothDevice)btDeviceAdapter.getItem(i);
//                                if(device!=null){
//                                    service.connect(device);
//                                    Log.i("device detected",device.getName());
//                                }
//                            }
//                        });
//                AlertDialog alert = builder.create();
//                alert.show();
                return true;
            case R.id.discoverable:
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
                service.start();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BroadcastReceiver _btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice btdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(getApplicationContext(),btdevice.getName(),Toast.LENGTH_SHORT).show();
                if (!devices.contains(btdevice)) {
                    devices.add(new BTdevice(btdevice,getApplicationContext()));
                    btDeviceAdapter.notifyDataSetChanged();
                }
            }else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON)
                    startScan();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(),"Discovery Finished",Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void startScan() {
        devices.clear();
        _btAdapter.startDiscovery();
        btDeviceAdapter.notifyDataSetChanged();
    }

    private class btDeviceArrayAdapter extends ArrayAdapter {
        private ArrayList<BTdevice> _devices;
        private ArrayList<BTdevice> _displayDevices;
        public btDeviceArrayAdapter(Context context, int resId, ArrayList<BTdevice> items) {
            super(context, resId);
            _devices = items;
            _displayDevices = new ArrayList<>(items);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
            }
            TextView devname = (TextView)convertView.findViewById(R.id.device_item_name);
            TextView devaddr = (TextView)convertView.findViewById(R.id.device_item_addr);
            BTdevice dev = _displayDevices.get(position);
            devname.setText(dev.getName());
            devaddr.setText(dev.getAddress());
            return convertView;
        }
        @Override
        public int getCount() {
            return _displayDevices.size();
        }
        @Override
        public long getItemId(int position) {
            return _displayDevices.get(position).hashCode();
        }
        @Override
        public BTdevice getItem(int position) {
            return _displayDevices.get(position);
        }
        @Override
        public void notifyDataSetChanged() {
            _displayDevices.clear();
            _displayDevices.addAll(_devices);
            super.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (_btReceiver !=null)
            unregisterReceiver(_btReceiver);
    }

}
