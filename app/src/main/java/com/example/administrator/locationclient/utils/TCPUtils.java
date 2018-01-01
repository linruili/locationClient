package com.example.administrator.locationclient.utils;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private ByteBuffer b;

    private DataOutputStream dataOutputStream;
    private ByteArrayOutputStream byteArrayOutputStream;

    public TCPUtils(int client_port)
    {
        try
        {
            b = ByteBuffer.allocate(4);
            byteArrayOutputStream = new ByteArrayOutputStream();
            socket = new Socket("amax.lan", client_port);
//            socket = new Socket("0.tcp.ngrok.io", client_port);
            if(socket.isConnected())
                Log.d("MainActivity", "connected");
            else
                Log.d("MainActivity", "failed to connect");
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void disconnect()
    {
        try
        {
            dataOutputStream.close();
            byteArrayOutputStream.close();
            socket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public void send_image_compass(Bitmap bitmap, double compass)
    {
        if (socket == null)
        {
            Log.d("MainActivity", "socket is null");
            return;
        }
        byteArrayOutputStream.reset();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        Log.d("MainActivity", "byteArray.length: " + byteArray.length);

        try
        {
            b.clear();
            b.putInt(byteArray.length);
            dataOutputStream.write(b.array());
            dataOutputStream.write(byteArray);

            byte[] doubleArray = (Double.toString(compass)).getBytes("utf-8");
            b.clear();
            b.putInt(doubleArray.length);
            dataOutputStream.write(b.array());
            dataOutputStream.write(doubleArray);
            dataOutputStream.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void send_fin()
    {
        if (socket == null)
        {
            Log.d("MainActivity", "socket is null");
            return;
        }
        int fin = 0;
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(fin);
        try
        {
            dataOutputStream.write(b.array());
            dataOutputStream.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String rec_result()
    {
        String response = "";
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        if (socket == null)
        {
            Log.d("MainActivity", "socket is null");
            return null;
        }
        try
        {
            is = socket.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            response = br.readLine();
        } catch (IOException e)
        {
            e.printStackTrace();
        }finally
        {
            try
            {
                br.close();
                isr.close();
                is.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        }
        return response;

    }


}
