package com.armyant.common.command;

import android.os.SystemClock;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 2017/9/20.
 */

public class Command {
    private static final String TAG = "Command";

    private static class InnerClass {
        private static Command INSTANCE = new Command();
    }

    private Command() {

    }

    public static Command getInstance() {
        return InnerClass.INSTANCE;
    }

    public void moveLocationConfig() {//这里是使用示例
        //cat /sys/class/net/wlan0/address
        //cat /sys/class/android_usb/android0/iSerial
        List<String> list = new ArrayList<>();
        list.add("mount -o rw,remount /system");//将system目录挂载为可读可写
        list.add("cp /sdcard/.system/location.conf /etc/");
        list.add("chmod 666 /etc/location.conf");//修改文件权限
        list.add("mount -o ro,remount /system");//将system目录挂载为只读状态
        executeBySu(list);
    }

    /**
     * 无需su权限
     * @param commands
     * @param limitTime
     * @return
     */
    public String execute(List<String> commands, long limitTime) {
        long costTime = 0;//已经消耗的时间
        StringBuilder sb = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            InputStream is = process.getInputStream();
            for (String cmd : commands) {
                os.writeBytes(cmd + "\n");
                os.flush();
            }
            //这里要等一段时间才有输入流
            while (is.available() <= 0) {
                SystemClock.sleep(10);
                costTime += 10;
                if (costTime > limitTime) {
                    break;
                }
            }
            byte[] buff = new byte[1024];
            int len;
            //读取完了数据以后，如果inputStream.available()==0，则InputStream.read()方法一直会被阻塞
            while (is.available() > 0
                    && (len = is.read(buff)) != -1) {
                sb.append(new String(buff, 0, len));
            }
            os.writeBytes("exit\n");
            os.flush();
            os.close();
            is.close();
            return sb.toString();
        } catch (IOException e) {
            Log.w("Command", "execute error=", e);
        } catch (Throwable th) {
            Log.w("Command", "Error executing internal operation", th);
        }
        return sb.toString();
    }

    /**
     * 执行需要su权限的命令
     *
     * @param commands
     */
    public void executeBySu(List<String> commands) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            for (String cmd : commands) {
                os.writeBytes(cmd + "\n");
                os.flush();
            }
            os.writeBytes("exit\n");
            os.flush();

            os.close();
        } catch (IOException e) {
            Log.w("ROOT", "Can't get root access", e);
        } catch (Throwable th) {
            Log.w("ROOT", "Error executing internal operation", th);
        }
    }
}
