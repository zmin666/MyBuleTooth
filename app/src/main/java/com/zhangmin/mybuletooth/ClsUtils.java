package com.zhangmin.mybuletooth;

import android.bluetooth.BluetoothDevice;

import java.lang.reflect.Method;

/**
 * @author: ZhangMin
 * @date: 2017/4/7 15:41
 * @desc:
 */
class ClsUtils {

    //自动配对设置Pin值
    static public boolean autoBond(Class btClass, BluetoothDevice device, String strPin) throws Exception {
        Method autoBondMethod = btClass.getMethod("setPin",new Class[]{byte[].class});
        Boolean result = (Boolean)autoBondMethod.invoke(device,new Object[]{strPin.getBytes()});
        return result;
    }

    //开始配对
    static public boolean createBond(Class btClass,BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

}
