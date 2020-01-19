package com.lin.socketdemo;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Lin on 2020/1/17.
 * Time: 10:28
 * Description:服务器
 */
public class ClientActivity extends AppCompatActivity {
    private EditText mEtPort;
    private EditText mEtMessage;
    private EditText mEtReceive;
    private EditText mEtIp;
    private Button mBtStart;
    private Button mBtStop;
    private Button mBtSend;
    private Button mBtSend2;
    private TextView mTvIp;
    Socket socket;
    private ServerThread mServerThread;
    private boolean isStop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        mTvIp = findViewById(R.id.tv_ip);
        mEtIp = findViewById(R.id.et_ip);
        mEtPort = findViewById(R.id.et_port);
        mEtMessage = findViewById(R.id.et_message);
        mEtReceive = findViewById(R.id.et_receive);
        mBtStart = findViewById(R.id.bt_start);
        mBtStop = findViewById(R.id.bt_stop);
        mBtSend = findViewById(R.id.bt_send);
        mBtSend2 = findViewById(R.id.bt_send2);

        mTvIp.setText(getLocalIpAddress());
        mBtStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStop = false;
                mServerThread = new ServerThread();
                mServerThread.start();
            }
        });
        mBtStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServerSocket();
            }
        });
        mBtSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String returnServer = mEtMessage.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (socket == null) return;
                            OutputStream om = socket.getOutputStream();
                            om.write(Constant.SERVER_TEXT.getBytes());
                            om.write(returnServer.getBytes());
                            om.write("\n".getBytes());//[10]
                            om.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("Lin", e.toString());
                        }
                    }
                }).start();

            }
        });
        mBtSend2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAlbum();
            }
        });
    }

    private void stopServerSocket() {
        isStop = true;
        try {
            if (socket != null) {
                socket.close();
            }
            Toast.makeText(ClientActivity.this, "停止服务", Toast.LENGTH_SHORT).show();
            Log.i("Lin", "停止服务");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ServerThread extends Thread {
        public void run() {
            try {
                String ip = mEtIp.getText().toString();
                int port = Integer.valueOf(mEtPort.getText().toString());//获取portEditText中的端口号
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port));
                final String socketAddress = socket.getRemoteSocketAddress().toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ClientActivity.this, "启动服务", Toast.LENGTH_SHORT).show();
                        Log.i("Lin", "启动服务");
                        Toast.makeText(ClientActivity.this, "成功建立与客户端的连接 : " + socketAddress, Toast.LENGTH_SHORT).show();
                        Log.i("Lin", "成功建立与客户端的连接 : " + socketAddress);
                    }
                });
                while (!isStop) {
                    byte[] b = new byte[1024 * 1024];
                    InputStream in = socket.getInputStream();
                    int len = in.read(b);
                    final String s = new String(b, 0, len);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mEtReceive.setText(mEtReceive.getText().toString() + socketAddress + " : " + s);
                        }
                    });
                    Log.i("Lin", "反馈为：" + s);

                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Lin", e.toString());
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (100 == requestCode) {
            if (data != null) {
                //获取数据
                //获取内容解析者对象
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i("Lin", data.getData().toString());

                            String imagePath = null;
                            Uri uri = data.getData();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                if (DocumentsContract.isDocumentUri(ClientActivity.this, uri)) {
                                    //如果是document类型的uri，则通过document id处理
                                    String docId = DocumentsContract.getDocumentId(uri);
                                    if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                                        //解析出数字格式的id
                                        String id = docId.split(":")[1];
                                        String selection = MediaStore.Images.Media._ID + "=" + id;
                                        imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                                    } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                                        Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                                        imagePath = getImagePath(contentUri, null);
                                    } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                                        //如果是content类型的uri，则使用普通方式处理
                                        imagePath = getImagePath(uri, null);
                                    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                                        //如果是file类型的uri，直接获取图片路径即可
                                        imagePath = uri.getPath();
                                    }
                                    //根据图片路径显示图片
                                    Log.i("Lin", imagePath);
                                    String finalImagePath = imagePath;

                                    File file = new File(finalImagePath);
                                    if (file.exists()) {
                                        final String fileMD5 = nullOfString(getFileMD5(file));
                                        OutputStream outputStream = socket.getOutputStream();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mEtReceive.setText(mEtReceive.getText().toString() + "file = " + fileMD5 + "\n");
                                            }
                                        });

                                        FileInputStream fileInputStream = new FileInputStream(file);
                                        outputStream.write(Constant.SERVER_FILE.getBytes());
                                        outputStream.flush();
                                        outputStream.write(fileMD5.getBytes());
                                        outputStream.flush();
                                        DataOutputStream dis = new DataOutputStream(outputStream);
                                        // 文件名和长度
                                        dis.writeUTF(file.getName());
                                        dis.flush();
                                        dis.writeLong(file.length());
                                        dis.flush();
                                        byte[] buffer = new byte[1024 * 1024];
                                        int len = 0;
                                        while ((len = fileInputStream.read(buffer, 0, buffer.length)) > 0) {
                                            dis.write(buffer, 0, len);
                                            dis.flush();
                                        }
                                        fileInputStream.close();
                                        Log.i("Lin", "传输成功");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("Lin", e.toString());
                        }
                    }
                }).start();
            }
        }
    }

    public String nullOfString(String str) {
        if (str == null) {
            str = "";
        }
        return str;
    }

    private static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest;
        FileInputStream in;
        try {
            byte[] buffer = new byte[1024];
            int len;
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
            BigInteger bigInt = new BigInteger(1, digest.digest());
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 通过uri和selection来获取真实的图片路径
     */
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * 打开系统相册
     */
    public void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        //设置请求码，以便我们区分返回的数据
        startActivityForResult(intent, 100);
    }

    /**
     * 获取WIFI下ip地址
     */
    @SuppressLint("DefaultLocale")
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
}
