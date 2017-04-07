package com.zhangmin.mybuletooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_OPEN_BT_CODE = 101;
    private static final int REQUEST_DISCOVERY_BT_CODE = 102;
    private static final int REQUEST_PERMISSION_BULETOOTH_MIN = 103;
    private BluetoothAdapter mBluetoothAdapter;


    private Button btOpen;
    private Button btClose;
    private Button btOpenBySystem;
    private Button btDiscoveryDevice;
    private Button btCancelDiscovery;
    private Button btDiscoveryBySystem;
    private Button btBoundDrivers;
    private EditText etText;
    private TextView tvShowMsg;

    private Button btSendMsgToSangsung;
    private Button btSendMsgToXiaomi;


    //创建客户端蓝牙Socket  // 客户端输出流 ---> 服务端
    private BluetoothSocket clientSocket;
    private OutputStream os;

    //创建服务端蓝牙Socket
    private AcceptThread acceptThread;
    private final UUID MY_UUID = UUID.fromString("abcd1234-ab12-ab12-ab12-abcdef123456");//和客户端相同的UUID
    private final String NAME = "Bluetooth_Socket";
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket mSocket;
    private InputStream is;//输入流

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            tvShowMsg.setText(String.valueOf(msg.obj));
            super.handleMessage(msg);
        }
    };


    //保存查找到的蓝牙设备
    private ArrayList<BluetoothDevice> btDeviceList = new ArrayList<>();
    private BluetoothDevice mFitDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 获得本机蓝牙适配器对象引用

        // 使用此检查确定BLE是否支持在设备上，然后你可以有选择性禁用BLE相关的功能
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        //广播
        initBroadcastReceiver();
        //打印目前已经绑定的蓝牙设备
        printBoundBluetooth();

        switch (mBluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_ON:
                break;
            case BluetoothAdapter.STATE_OFF:
                //bluetoothAdapter.enable();
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                break;
            default:
                break;
        }


        //客户端初始化  --> 小米 --> 手机
        initClient();
        //服务端初始化 --> 三星  --> 车
        initService();
    }

    private void initClient() {
//        //创建客户端蓝牙Socket
//        if (mFitDevice == null) {
//            mFitDevice = mBluetoothAdapter.getRemoteDevice("08:FD:0E:AA:04:C2");
//        }
//        //开始连接蓝牙，如果没有配对则弹出对话框提示我们进行配对
//        try {
//            clientSocket = mFitDevice.createRfcommSocketToServiceRecord(MY_UUID);
//            clientSocket.connect();
//            os = clientSocket.getOutputStream();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void initService() {
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    /**
     * 打印处当前已经绑定成功的蓝牙设备
     */
    private void printBoundBluetooth() {
        Set<BluetoothDevice> bts = mBluetoothAdapter.getBondedDevices();
        Iterator<BluetoothDevice> iterator = bts.iterator();
        while (iterator.hasNext()) {
            BluetoothDevice bd = iterator.next();
            Log.i(TAG, " Name : " + bd.getName() + " Address : " + bd.getAddress());
            Log.i(TAG, "Device class" + bd.getBluetoothClass());
        }

        BluetoothDevice findDevice = mBluetoothAdapter.getRemoteDevice("00:11:22:33:AA:BB");
        Log.i(TAG, "findDevice Name : " + findDevice.getName() + "  findDevice Address : " + findDevice.getAddress());
        Log.i(TAG, "findDevice class" + findDevice.getBluetoothClass());
    }

    private void initView() {
        btOpen = (Button) findViewById(R.id.btOpen);
        btClose = (Button) findViewById(R.id.btClose);
        btOpenBySystem = (Button) findViewById(R.id.btOpenBySystem);
        btDiscoveryDevice = (Button) findViewById(R.id.btDiscoveryDevice);
        btCancelDiscovery = (Button) findViewById(R.id.btCancelDiscovery);
        btDiscoveryBySystem = (Button) findViewById(R.id.btDiscoveryBySystem);
        btBoundDrivers = (Button) findViewById(R.id.btBoundDrivers);
        etText = (EditText) findViewById(R.id.et_text);
        tvShowMsg = (TextView) findViewById(R.id.tv_showMsg);
        btSendMsgToSangsung = (Button) findViewById(R.id.btSendMsg_toSangsung);
        btSendMsgToXiaomi = (Button) findViewById(R.id.btSendMsg_toXiaomi);


        btOpen.setOnClickListener(this);
        btClose.setOnClickListener(this);
        btOpenBySystem.setOnClickListener(this);
        btDiscoveryDevice.setOnClickListener(this);
        btCancelDiscovery.setOnClickListener(this);
        btDiscoveryBySystem.setOnClickListener(this);
        btBoundDrivers.setOnClickListener(this);
        btSendMsgToSangsung.setOnClickListener(this);
        btSendMsgToXiaomi.setOnClickListener(this);
    }

    private void initBroadcastReceiver() {
        //蓝牙状态发生改变  扫描模式发生改变  蓝牙开关状态发生改变
        final BroadcastReceiver buletoothStateChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    printBTState(state);
                } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {
                    int cur_mode_state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
                    int previous_mode_state = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
                    Log.v(TAG, "### cur_mode_state ##" + cur_mode_state + " ~~ previous_mode_state" + previous_mode_state);
                }

            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(buletoothStateChangeReceiver, intentFilter);


        //蓝牙扫描时的广播接收器
        BroadcastReceiver BTDiscoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        btDeviceList.clear();
                        Log.v(TAG, "### BT ACTION_DISCOVERY_STARTED ##");
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        Log.v(TAG, "### BT ACTION_DISCOVERY_FINISHED ##");
                        Log.v(TAG, "一共找到" + btDeviceList.size() + "个蓝牙设备");
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        Log.v(TAG, "### BT BluetoothDevice.ACTION_FOUND ##");
                        BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        btDeviceList.add(btDevice);
                        if (btDevice != null)
                            Log.v(TAG, "Name : " + btDevice.getName() + " Address: " + btDevice.getAddress());
                        break;
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        Log.v(TAG, "### BT ACTION_BOND_STATE_CHANGED ##");
                        int cur_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                        int previous_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                        Log.v(TAG, "### cur_bond_state ##" + cur_bond_state + " ~~ previous_bond_state" + previous_bond_state);
                        // 配对了远程设备
                        if (BluetoothDevice.BOND_BONDED == cur_bond_state) {
                            toast(mFitDevice.getName() + "..设备已经配对");
                            btBoundDrivers.setTextColor(Color.BLUE);
                        } else {
                            btBoundDrivers.setTextColor(Color.BLACK);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        IntentFilter btDiscoveryFilter = new IntentFilter();
        btDiscoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);  // 开始连接
        btDiscoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //  结束连接
        btDiscoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);  //发现设备
        btDiscoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);  //连接设备状态改变
        registerReceiver(BTDiscoveryReceiver, btDiscoveryFilter);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_BT_CODE) {
            if (resultCode == RESULT_CANCELED) {
                toast("Sorry , 用户拒绝了您的打开蓝牙请求.");
            } else
                toast("Year , 用户允许了您的打开蓝牙请求.");
        } else if (requestCode == REQUEST_DISCOVERY_BT_CODE) {
            if (resultCode == RESULT_CANCELED) {
                toast("Sorry , 用户拒绝了，您的蓝牙不能被扫描.");
            } else
                toast("Year , 用户允许了，您的蓝牙能被扫描");
        }
    }

    @Override
    public void onClick(View v) {
        boolean wasBtOpened = mBluetoothAdapter.isEnabled(); // 是否已经打开

        switch (v.getId()) {
            case R.id.btOpen: // 打开
                boolean result = mBluetoothAdapter.enable();
                if (result)
                    toast("蓝牙打开操作成功");
                else if (wasBtOpened)
                    toast("蓝牙已经打开了");
                else
                    toast("蓝牙打开失败");
                break;
            case R.id.btClose: // 关闭
                boolean result1 = mBluetoothAdapter.disable();
                if (result1)
                    toast("蓝牙关闭操作成功");
                else if (!wasBtOpened)
                    toast("蓝牙已经关闭");
                else
                    toast("蓝牙关闭失败.");
                break;
            case R.id.btOpenBySystem:  //调用系统API打开蓝牙设备
                Log.e(TAG, " ## click btOpenBySystem ##");
                //未打开蓝牙，才需要打开蓝牙
                if (!wasBtOpened) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_OPEN_BT_CODE);
                } else
                    toast("Hi ，蓝牙已经打开了，不需要在打开啦 ~~~");
                break;
            case R.id.btDiscoveryDevice: //扫描时，必须先打开蓝牙
                if (!mBluetoothAdapter.isDiscovering()) {
                    Log.i(TAG, "btCancelDiscovery ### the bluetooth dont't discovering");
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mBluetoothAdapter.startDiscovery();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_BULETOOTH_MIN);
                    }
                } else
                    toast("蓝牙正在搜索设备了 ---- ");
                break;
            case R.id.btCancelDiscovery:   //取消扫描
                if (mBluetoothAdapter.isDiscovering()) {
                    Log.i(TAG, "btCancelDiscovery ### the bluetooth is isDiscovering");
                    mBluetoothAdapter.cancelDiscovery();
                } else
                    toast("蓝牙并未搜索设备 ---- ");
                break;

            case R.id.btDiscoveryBySystem:  //使蓝牙能被扫描
                Intent discoveryintent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoveryintent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(discoveryintent, REQUEST_DISCOVERY_BT_CODE);
                break;

            case R.id.btBoundDrivers:  //使蓝牙能被扫描
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("08:FD:0E:AA:04:C2");
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {//判断给定地址下的device是否已经配对
                    try {
                        //蓝牙请求配对
                        Method createBondMethod = device.getClass().getMethod("createBond");
                        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    try{
//                        ClsUtils.createBond(device.getClass(), device);
//                    } catch (Exception e) {
//                        // TODO: handle exception
//                        System.out.println("配对不成功");
//                    }
                } else {
                    toast("已经配对成功");
                }
                mFitDevice = device;
                break;

            case R.id.btSendMsg_toSangsung:  //
                //创建客户端蓝牙Socket
                try {
                    mFitDevice = mBluetoothAdapter.getRemoteDevice("08:FD:0E:AA:04:C2");
                    Method createBondMethod = mFitDevice.getClass().getMethod("createBond");
                    createBondMethod.invoke(mFitDevice);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //开始连接蓝牙，如果没有配对则弹出对话框提示我们进行配对
                try {
                    clientSocket = mFitDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    clientSocket.connect();
                    os = clientSocket.getOutputStream();
                    os.write(etText.getText().toString().trim().getBytes("utf-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btSendMsg_toXiaomi:
                //创建客户端蓝牙Socket
                try {
                    mFitDevice = mBluetoothAdapter.getRemoteDevice("18:59:36:19:D4:50");
                    Method createBondMethod = mFitDevice.getClass().getMethod("createBond");
                    createBondMethod.invoke(mFitDevice);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //开始连接蓝牙，如果没有配对则弹出对话框提示我们进行配对
                try {
                    clientSocket = mFitDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    clientSocket.connect();
                    os = clientSocket.getOutputStream();
                    os.write(etText.getText().toString().trim().getBytes("utf-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

        }
    }


    private void printBTState(int btState) {
        switch (btState) {
            case BluetoothAdapter.STATE_OFF:
                toast("蓝牙状态:已关闭");
                Log.v(TAG, "BT State ： BluetoothAdapter.STATE_OFF ###");
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                toast("蓝牙状态:正在关闭");
                Log.v(TAG, "BT State :  BluetoothAdapter.STATE_TURNING_OFF ###");
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                toast("蓝牙状态:正在打开");
                Log.v(TAG, "BT State ：BluetoothAdapter.STATE_TURNING_ON ###");
                break;
            case BluetoothAdapter.STATE_ON:
                toast("蓝牙状态:已打开");
                Log.v(TAG, "BT State ：BluetoothAdapter.STATE_ON ###");
                break;
            default:
                break;
        }
    }

    private void toast(String str) {
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_BULETOOTH_MIN:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mBluetoothAdapter.startDiscovery();
                } else {
                    toast("权限申请被拒绝");
                }
                break;
            default:
                break;
        }
    }


    //**************************************************************************/
    //服务端监听客户端的线程类
    private class AcceptThread extends Thread {

        public AcceptThread() {
            try {
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (Exception e) {
            }
        }

        public void run() {
            try {
                mSocket = serverSocket.accept();
                is = mSocket.getInputStream();
                while (true) {
                    byte[] buffer = new byte[1024];
                    int count = is.read(buffer);
                    Message msg = new Message();
                    msg.obj = new String(buffer, 0, count, "utf-8");
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
            }
        }


    }
}
