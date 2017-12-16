package com.example.administrator.locationclient.utils;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

/**
 * Created by Administrator on 2017/12/15.
 */

public class TCPUtils
{
    private Socket socket;
    private ExecutorService mThreadPool;

    OutputStream outputStream;
    private DataOutputStream dataOutputStream;
    private ByteArrayOutputStream byteArrayOutputStream;

    public TCPUtils(int client_port)
    {
        try
        {
            socket = new Socket("0.tcp.ngrok.io", client_port);
            if(socket.isConnected())
                Log.d("MainActivity", "connected");
            else
                Log.d("MainActivity", "failed to connect");
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void send_compass(double compass)
    {
        if(socket == null)
        {
            Log.d("MainActivity", "socket is null");
            return;
        }
        try
        {
            outputStream = socket.getOutputStream();
            outputStream.write((Double.toString(compass)+"\n").getBytes("utf-8"));
            // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞
            outputStream.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void send_image(Bitmap bitmap)
    {
        if(socket == null)
        {
            Log.d("MainActivity", "socket is null");
            return;
        }
        try
        {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            int bytes = bitmap.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(bytes);
            bitmap.copyPixelsToBuffer(buffer);
            byte[] byteArray = buffer.array();//-128~127

            Log.d("MainActivity", "byteArray.length: " + byteArray.length);
            dataOutputStream.write(byteArray);
            dataOutputStream.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
