/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.braulio.chairman;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.braulio.chairman.adapter.ImageAdapter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */

//DeviceScanActivity is main activity
public class DeviceScanActivity extends Activity /*ListActivity*/ {

    private final static String TAG = DeviceScanActivity.class.getSimpleName();
    //bluetooth
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private SwipeRefreshLayout swipeToScanBLE;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private static final long RECEIPT_DELAY = 3000;
    private byte[] endData = null;
    private ListView list_device;
    private boolean mIsBound;
    private AlertDialog mPayDialog;
    //menu grid view
    private GridView menuGridView;
    private static final String[] MENU_ITEMS = new String[] { "coffee", "donut", "tea", "croissant" };
    private double totalPrice;
    private Coffee coffee;
    private Donut donut;
    private Tea tea;
    private Croissant croissant;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         setActivityLogoTitle();

        setContentView(R.layout.order);

        menuGridView = (GridView) findViewById(R.id.menuGridView);
        menuGridView.setAdapter(new ImageAdapter(this,MENU_ITEMS));
        coffee = new Coffee();
        donut = new Donut();
        tea = new Tea();
        croissant = new Croissant();
        displayTotalPrice(false,false);

        // Retrieve the SwipeRefreshLayout and ListView instances
        swipeToScanBLE = (SwipeRefreshLayout) findViewById(R.id.swipeToScanBLE);


        mHandler = new Handler();
        checkBLE();
        list_device = (ListView) findViewById(R.id.listitem_device);
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    public Coffee getCoffee(){
        return coffee;
    }

    public Donut getDonut(){
        return donut;
    }

    public Tea getTea(){
        return tea;
    }

    public Croissant getCroissant(){
        return croissant;
    }

    private void setActivityLogoTitle() {
        android.app.ActionBar actionbar = getActionBar(); //this will return null if in androidmanifest, application uses         android:theme="@style/AppTheme">
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setLogo(R.drawable.coffee_icon);
        actionbar.setTitle(R.string.menu_title);
        actionbar.setDisplayUseLogoEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                getActionBar().setTitle(R.string.title_devices);
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_connect:
                if(mBluetoothLeService != null)
                    if(!mConnected)
                        mBluetoothLeService.connect(mDeviceAddress);
                break;
            case R.id.menu_disconnect:
                if(mBluetoothLeService != null)
                    if(mConnected)
                        mBluetoothLeService.disconnect();
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to displayCoffeeQuantity a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        list_device.setAdapter(mLeDeviceListAdapter);
        list_device.setOnItemClickListener(new onItemClickListener());

//        setListAdapter(mLeDeviceListAdapter);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        scanLeDevice(true); //when launching this activity, will start scanning by default
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }



        swipeToScanBLE.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "onRefresh called from SwipeRefreshLayout before initiateRefresh");

                initiateRefresh();

            }
        });


    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_BLE_WRITE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE_SUCCESS);
        return intentFilter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
//        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.


            // Detach our existing connection.
            unbindService(mServiceConnection);
            mIsBound = false;

        }
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Toast.makeText(this, resourceId, Toast.LENGTH_SHORT).show();
//                mConnectionState.setText(resourceId);
            }
        });
    }

    private String getBTDevieType(BluetoothDevice d){
        String type = "";

        switch (d.getType()){
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;
            default:
                type = "unknown...";
        }

        return type;
    }

    private class onItemClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final BluetoothDevice device=mLeDeviceListAdapter.getDevice(position);
            if (device == null) return;
            mDeviceName = device.getName();
            mDeviceAddress = device.getAddress();
            if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            }

            Intent gattServiceIntent = new Intent(DeviceScanActivity.this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE); //this will then create Service
            //but if service still binded, it will bind again, hence will callback onServiceConnected, and then no connect(mDeviceAddress)
            mIsBound = true;
            //    private final ServiceConnection mServiceConnection = new ServiceConnection()
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Toast.makeText(context, R.string.connected, Toast.LENGTH_SHORT).show();
                updateConnectionState(R.string.connected);

                mLeDeviceListAdapter.clear();
                mLeDeviceListAdapter.notifyDataSetChanged();
                getActionBar().setTitle(mDeviceName);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                Toast.makeText(context, R.string.disconnected, Toast.LENGTH_SHORT).show();
                updateConnectionState(R.string.disconnected);
                getActionBar().setTitle(R.string.ble_device_scan);
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.e(TAG, "BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED");
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data) {
                        stringBuilder.append(String.format("%02X ", byteChar));
                    }
                    handleBLEResData(data);
                }
            }
            else if (BluetoothLeService.ACTION_BLE_WRITE.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data) {
                        stringBuilder.append(String.format("%02X ", byteChar));
                    }
                }
            }
            else if (BluetoothLeService.ACTION_DATA_WRITE_SUCCESS.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA); //data should mean the cmd Tx
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data) {
                        stringBuilder.append(String.format("%02X ", byteChar));
                    }

                    //if successfully sent DI3 pay command
                    //use below if to distinguish DI3, because sometimes this will be ACK 06 to write when for response ACK
                    if(new String(Arrays.copyOfRange(data,1,4)).equals("DI3")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanActivity.this);
                        builder.setMessage("Please tap your card\n\n\n\n\n");
                        mPayDialog = builder.show();
                    }

                }
            }
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();//cast service to LocalBinder, and call its getService
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device); //mLeDeviceListAdapter is LeDeviceListAdapter private class
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private void scanLeDevice(final boolean enable) {
        if (enable) {

            // Stops scanning after a pre-defined scan period, 10 seconds
            //mHandler will fire a separate thread after scan_period
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD); //scan_period  10 seconds

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }



    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    public void dumpLog (String strTag, String strMsg, byte[] bData){

        StringBuffer sb = new StringBuffer();
        if (strTag == null || bData.length <= 0)
            return;
        else {
            Log.d(strTag, strMsg + "  " + String.valueOf(bData.length));

            for (int index = 0; index < bData.length; index++){
                if(index == 0 && bData[index] == Utility.XAC_ACK_COMMAND){
                }
                else {
                    sb.append(String.format("%02X", bData[index] & 0xFF));
                    sb.append(" ");
                }
            }
            //alan: this will cause app to crash
            //sendResThread thread = new sendResThread(sb.toString());
//            new Thread(thread).start();
            Log.d(strTag, sb.toString());
            sb.reverse();
        }
    }

    private void handleBLEResData(byte[] bData) {

        if(bData != null){
            byte[] getData1;

            if(endData == null)
                getData1 = bData;
            else {
                getData1 = new byte[endData.length + bData.length];
                System.arraycopy(endData,0,getData1,0,endData.length);
                System.arraycopy(bData,0,getData1,endData.length,bData.length);
                endData = null;
            }

            if(getData1.length != 1 || endData != null) {
                boolean isFinish = false;
                int flag = 0;
                while(!isFinish) {
                    byte[] data = Utility.cutArray(getData1, flag, getData1.length);
                    if(data == null)
                        isFinish = true;
                    else if (Utility.findStartPoint(data, data.length)) {
                        int first = Utility.getStartPoint(data, data.length);
                        int end = 0;

                        //Find End Point 03
                        if (Utility.findRSPoint(data, data.length)) {//RS
                            if( Utility.getRSPoint(data, data.length) <=  (data.length - 3)) {
                                int positionRS = Utility.getRSPoint(data, data.length);
                                int len = ((data[positionRS + 1] & 0xff) << 8) | (data[positionRS + 2] & 0xff);
                                if (positionRS + 2 + len < data.length - 2) {
                                    end = positionRS + 2 + len + 1;
                                }
                            }
                        }
                        else {
                            if (Utility.findEndPointBackward(data, data.length) && Utility.getEndPointBackward(data, data.length) != (data.length - 1))
                                end = Utility.getEndPointBackward(data, data.length);
                        }

                        //Check LRC
                        if(end != 0){
                            byte[] cmd = Arrays.copyOfRange(data, first, end + 2);

                            if(mPayDialog != null) {
                                mPayDialog.cancel();
                                mPayDialog.dismiss();
                                mPayDialog = null;
                            }

                            if (Utility.checkLRC(cmd)) {
                                //check DI3 response for receipt demo
                                if(new String(Arrays.copyOfRange(cmd,1,5)).equals("DI30"))
                                {
                                    // Stops scanning after a pre-defined scan period.
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            final AlertDialog.Builder receipt = new AlertDialog.Builder(DeviceScanActivity.this);
                                            receipt.setTitle("Receipt");
                                            receipt.setMessage("Item: coffee\nDate: 2017/11/16\nAmount: "+ totalPrice +"\n");
                                            receipt.setPositiveButton(android.R.string.ok, null);
                                            receipt.setOnDismissListener(new DialogInterface.OnDismissListener(){

                                                @Override
                                                public void onDismiss(DialogInterface dialog){
                                                    Toast.makeText(DeviceScanActivity.this, "Thank you", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                            receipt.show();
                                            totalPrice = 0;
                                        }
                                    }, RECEIPT_DELAY);

                                }
                                mBluetoothLeService.sendData(Utility.XAC_ACK);
                                dumpLog("Response", "Read Data", cmd); //here will cause app to crash
                            } else {
                                mBluetoothLeService.sendData(Utility.XAC_NAK);
                            }
                            flag = end + 2;
                            endData = Utility.cutArray(getData1, flag, getData1.length);
                        } else {
                            endData = Utility.cutArray(getData1, flag, getData1.length);
                            isFinish = true;
                        }
                    }
                    else {
                        endData = null;
                        isFinish = true;
                    }
                }
            }
        }
    }


    private void checkBLE()
    {
        //https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        //Build.VERSION.SDK_INT: The user-visible SDK version of the framework; its possible values are defined in Build.VERSION_CODES.
        //Build.VERSION_CODES.M M is for Marshmallow! 6.0 – 6.0.1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener(){

                    @Override
                    public void onDismiss(DialogInterface dialog){
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }



    public class Coffee {

        private int numberOfCoffees;
        boolean hasChocolate;
        boolean hasWhippedCream;
        private final double coffeePrice = 1.25; // coffee price
        TextView tv_quantity;

        public void setTextViewQuantity(TextView tv_quantity){
                this.tv_quantity = tv_quantity;
        }

        /**
         * This method is called when the + button is clicked.
         */
        public void incrementCoffee(View v) {
            if (numberOfCoffees <= 49) {
                numberOfCoffees = numberOfCoffees + 1;
                displayCoffeeQuantity(v, numberOfCoffees);
                displayTotalPrice(hasWhippedCream, hasChocolate);
            } else {
                Toast.makeText(DeviceScanActivity.this, R.string.max_reached, Toast.LENGTH_SHORT).show();
            }

        }

        /**
         * This method is called when the - button is clicked.
         */
        public void decrementCoffee(View v) {
            if (numberOfCoffees >= 1) {
                numberOfCoffees = numberOfCoffees - 1;
                displayCoffeeQuantity(v, numberOfCoffees);
                displayTotalPrice(hasWhippedCream, hasChocolate);
            }
        }

//        public void addWhippedCream(View view) {
//            CheckBox whippedCreamCheckBox = (CheckBox) findViewById(R.id.whipped_cream_checkbox);
//            hasWhippedCream = whippedCreamCheckBox.isChecked();
//            displayTotalPrice(numberOfCoffees,hasWhippedCream,hasChocolate);
//        }
//
//        public void addChocolate(View view) {
//            CheckBox chocolateCheckBox = (CheckBox) findViewById(R.id.chocolate_checkbox);
//            hasChocolate = chocolateCheckBox.isChecked();
//            displayTotalPrice(numberOfCoffees,hasWhippedCream,hasChocolate);
//        }


        /**
         * This method displays the given quantity value on the screen.
         */
        public void displayCoffeeQuantity(View v, int number) {
            TextView quantityTextView = (TextView) v;
            quantityTextView.setText("" + number);
        }

    }

    public class Donut{
        private int numberOfDonuts;
        private final double donutPrice= 0.89; // Donut price

        TextView tv_quantity;

        public void setTextViewQuantity(TextView tv_quantity){
            this.tv_quantity = tv_quantity;
        }

        public void incrementDonut(View v) {
            if (numberOfDonuts <= 49) {
                numberOfDonuts = numberOfDonuts + 1;
                displayDonutQuantity(v, numberOfDonuts);
                displayTotalPrice(false, false);
            } else {
                Toast.makeText(DeviceScanActivity.this, R.string.max_reached, Toast.LENGTH_SHORT).show();
            }
        }

        public void decrementDonut(View v) {
            if (numberOfDonuts >= 1) {
                numberOfDonuts = numberOfDonuts - 1;
                displayDonutQuantity(v, numberOfDonuts);
                displayTotalPrice(false, false);
            }
        }

        public void displayDonutQuantity(View v ,int number) {
            TextView quantityTextView = (TextView) v;
            quantityTextView.setText("" + number);
        }
    }

    public class Tea{
        private int numberOfTea;
        private final double teaPrice= 1.00; // Tea price

        TextView tv_quantity;

        public void setTextViewQuantity(TextView tv_quantity){
            this.tv_quantity = tv_quantity;
        }

        public void incrementTea(View v) {
            if (numberOfTea <= 49) {
                numberOfTea = numberOfTea + 1;
                displayTeaQuantity(v, numberOfTea);
                displayTotalPrice(false, false);
            } else {
                Toast.makeText(DeviceScanActivity.this, R.string.max_reached, Toast.LENGTH_SHORT).show();
            }
        }

        public void decrementTea(View v) {
            if (numberOfTea >= 1) {
                numberOfTea = numberOfTea - 1;
                displayTeaQuantity(v, numberOfTea);
                displayTotalPrice(false, false);
            }
        }

        public void displayTeaQuantity(View v ,int number) {
            TextView quantityTextView = (TextView) v;
            quantityTextView.setText("" + number);
        }

    }

    public class Croissant{
        private int numberOfCroissants;
        private final double croissantPrice= 2.1; // croissant price

        TextView tv_quantity;

        public void setTextViewQuantity(TextView tv_quantity){
            this.tv_quantity = tv_quantity;
        }

        public void incrementCroissant(View v) {
            if (numberOfCroissants <= 49) {
                numberOfCroissants = numberOfCroissants + 1;
                displayCroissantQuantity(v, numberOfCroissants);
                displayTotalPrice(false, false);
            } else {
                Toast.makeText(DeviceScanActivity.this, R.string.max_reached, Toast.LENGTH_SHORT).show();
            }
        }

        public void decrementCroissant(View v) {
            if (numberOfCroissants >= 1) {
                numberOfCroissants = numberOfCroissants - 1;
                displayCroissantQuantity(v, numberOfCroissants);
                displayTotalPrice(false, false);
            }
        }

        public void displayCroissantQuantity(View v ,int number) {
            TextView quantityTextView = (TextView) v;
            quantityTextView.setText("" + number);
        }

    }

        /**
         * This method displays the given totalPrice on the screen.
         */
        public void displayTotalPrice( boolean hasWhippedCream, boolean hasChocolate) {
            double priceChocolate = 0.5;
            double priceWhippedCream = 0.5;

            totalPrice = 0.00;
            totalPrice += coffee.numberOfCoffees * coffee.coffeePrice;
            totalPrice += donut.numberOfDonuts * donut.donutPrice;
            totalPrice += tea.numberOfTea * tea.teaPrice;
            totalPrice += croissant.numberOfCroissants * croissant.croissantPrice;


            if (hasWhippedCream) {
                totalPrice += priceWhippedCream * coffee.numberOfCoffees;
            }
            if (hasChocolate) {
                totalPrice += priceChocolate * coffee.numberOfCoffees;
            }
            TextView priceTextView = (TextView) findViewById(R.id.price_text_view);
            priceTextView.setText(getString(R.string.price) +": " + NumberFormat.getCurrencyInstance().format(totalPrice));
        }



        public void payOrder(View view){
            if (mBluetoothLeService != null) {
                final byte[] cmd_DI3={0x44,0x49,0x33};
                mBluetoothLeService.sendData(Utility.getCommand(cmd_DI3));
            }
    //            Toast.makeText(DeviceScanActivity.this, "submit order", Toast.LENGTH_SHORT).show();
        }

        public void resetOrder(){
            coffee.numberOfCoffees = 0;
            coffee.displayCoffeeQuantity(coffee.tv_quantity,coffee.numberOfCoffees);
            donut.numberOfDonuts = 0;
            donut.displayDonutQuantity(donut.tv_quantity,donut.numberOfDonuts);
            tea.numberOfTea = 0;
            tea.displayTeaQuantity(tea.tv_quantity,tea.numberOfTea);
            croissant.numberOfCroissants = 0;
            croissant.displayCroissantQuantity(croissant.tv_quantity,croissant.numberOfCroissants);
            displayTotalPrice(false,false);
        }

        public void cancelOrder(View view){
            resetOrder();
            if (mBluetoothLeService != null) {
                final byte[] cmd_Cancel={0x37,0x32};
                mBluetoothLeService.sendData(Utility.getCommand(cmd_Cancel));
            }
            //            Toast.makeText(DeviceScanActivity.this, "submit order", Toast.LENGTH_SHORT).show();
        }



    /**
     * By abstracting the refresh process to a single method, the app allows both the
     * SwipeGestureLayout onRefresh() method and the Refresh action item to refresh the content.
     */
    private void initiateRefresh() {
        Log.i(TAG, "initiateRefresh");

        /**
         * Execute the background task, which uses {@link android.os.AsyncTask} to load the data.
         */
        new DummyBackgroundTask().execute();
    }
    // END_INCLUDE (initiate_refresh)

    // BEGIN_INCLUDE (refresh_complete)
    /**
     * When the AsyncTask finishes, it calls onRefreshComplete(), which updates the data in the
     * ListAdapter and turns off the progress bar.
     */
    private void onRefreshComplete(List<String> result) {
        Log.i(TAG, "onRefreshComplete");

        // Remove all items from the ListAdapter, and then replace them with the new items
//        mListAdapter.clear();
//        for (String cheese : result) {
//            mListAdapter.add(cheese);
//        }

        // Stop the refreshing indicator
        swipeToScanBLE.setRefreshing(false);
    }
    // END_INCLUDE (refresh_complete)

    /**
     * Dummy {@link AsyncTask} which simulates a long running task to fetch new cheeses.
     */
    private class DummyBackgroundTask extends AsyncTask<Void, Void, List<String>> {

        static final int TASK_DURATION = 10 * 1000; // 3 seconds

        @Override
        protected List<String> doInBackground(Void... params) {
            // Sleep for a small amount of time to simulate a background-task
//            try {
//                Thread.sleep(TASK_DURATION);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    getActionBar().setTitle(R.string.title_devices);
                    mLeDeviceListAdapter.clear();
                    scanLeDevice(true);

                }
            });


            // Return a new random list of cheeses
            return null;
//            return Cheeses.randomList(LIST_ITEM_COUNT);
        }

        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);

            // Tell the Fragment that the refresh has completed
            onRefreshComplete(result);
        }

    }



}