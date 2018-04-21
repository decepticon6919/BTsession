package com.example.sukanya.btsession;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import java.util.ArrayList;

/**
 * Created by Sukanya on 21-04-2018.
 */

public class BTdevice {
    Context _context;
    String _btname;
    String _btaddress;
    BluetoothDevice _btdevice;

    public BTdevice(BluetoothDevice btdev,Context context) {
        _context = context;
        _btdevice = btdev;
        _btname = btdev.getName();
        _btaddress = btdev.getAddress();
    }

    public String getName(){
        return _btname;
    }

    public String getAddress(){
        return _btaddress;
    }

}
