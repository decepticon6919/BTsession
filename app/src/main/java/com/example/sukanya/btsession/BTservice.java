package com.example.sukanya.btsession;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Sukanya on 21-04-2018.
 */

public class BTservice {

    private static final String _appName = "BluetoothSession";
    private static final UUID _appUuid = UUID.fromString("44e128a5-ac7a-4c9a-be4c-224b6bf81b20");
    private final BluetoothAdapter _btAdapter;
    private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Context context;

    public BTservice(Context applicationContext) {
        context = applicationContext;
        _btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized void start(){
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread();
            mSecureAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        if(mConnectThread == null) {
            mConnectThread = new ConnectThread(device);
            mConnectThread.start();
        }
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice remoteDevice) {
        if(mConnectedThread == null) {
            mConnectedThread = new ConnectedThread(socket, remoteDevice);
            mConnectedThread.start();
        }
    }

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = _btAdapter.listenUsingRfcommWithServiceRecord(_appName, _appUuid);

            } catch (Exception e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }
        public void run() {
            BluetoothSocket socket = null;
            while(true) {
                try {
                    if (mmServerSocket != null) {
                        socket = mmServerSocket.accept();
                        if (socket != null) {
                            connected(socket, socket.getRemoteDevice());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(_appUuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }
        public void run() {
            _btAdapter.cancelDiscovery();
            for(int i=0;i<3;i++) {
                try {
                    mmSocket.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            synchronized (BTservice.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }
        public void cancel() {
            try {
                mmSocket.close();
            }
            catch (Exception e) {
               e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket, BluetoothDevice remoteDevice) {
            mmSocket = socket;
            mmDevice = remoteDevice;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (Exception e) {
               e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String s= new String(buffer,"UTF-8");
                    Log.i("bt_read",s.substring(0,bytes));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                String s= new String(buffer,"UTF-8");
                Log.i("bt_write",s);
                mmOutStream.flush();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
