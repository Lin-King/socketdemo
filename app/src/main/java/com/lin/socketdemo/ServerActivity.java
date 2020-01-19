package com.lin.socketdemo;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Lin on 2020/1/17.
 * Time: 10:28
 * Description:服务器
 */
public class ServerActivity extends AppCompatActivity {
    ServerSocket serverSocket;//创建ServerSocket对象
    Socket socket;
    private ServerThread mServerThread;
    private boolean isStop;
    private EditText mEtPort;
    private EditText mEtMessage;
    private EditText mEtReceive;
    private Button mBtStart;
    private Button mBtStop;
    private Button mBtSend;
    private TextView mTvIp;
    private String mPath = "";
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        mTvIp = findViewById(R.id.tv_ip);
        mEtPort = findViewById(R.id.et_port);
        mEtMessage = findViewById(R.id.et_message);
        mEtReceive = findViewById(R.id.et_receive);
        mBtStart = findViewById(R.id.bt_start);
        mBtStop = findViewById(R.id.bt_stop);
        mBtSend = findViewById(R.id.bt_send);

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
                            PrintWriter writer = new PrintWriter(om, true);//告诉客户端连接成功 并传状态过去
                            writer.write(returnServer + "\n");
                            writer.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("Lin", e.toString());
                        }
                    }
                }).start();
            }
        });
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
    }

    private void stopServerSocket() {
        isStop = true;
        try {
            if (socket != null) {
                socket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
            Toast.makeText(ServerActivity.this, "停止服务", Toast.LENGTH_SHORT).show();
            Log.i("Lin", "停止服务");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServerSocket();
    }

    class ServerThread extends Thread {
        public void run() {
            try {
                Log.i("Lin", "port " + mEtPort.getText().toString());
                int port = Integer.valueOf(mEtPort.getText().toString());//获取portEditText中的端口号
                serverSocket = new ServerSocket(port);//首先创建一个服务端口
                Log.i("Lin", "等待客户端的连接请求");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ServerActivity.this, "启动服务", Toast.LENGTH_SHORT).show();
                        Log.i("Lin", "启动服务");
                    }
                });
                //等待客户端的连接请求
                socket = serverSocket.accept();
                final String socketAddress = socket.getRemoteSocketAddress().toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ServerActivity.this, "成功建立与客户端的连接 : " + socketAddress, Toast.LENGTH_SHORT).show();
                        Log.i("Lin", "成功建立与客户端的连接 : " + socketAddress);
                    }
                });
                while (!isStop) {
                    InputStream inputStream = socket.getInputStream();
//                    BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
                    String type = "";
                    byte[] typebytes = new byte[Constant.SERVER_TYPE];
                    if (inputStream.read(typebytes) != -1) {
                        type = nullOfString(new String(typebytes));
                    }

                    switch (type) {
                        case Constant.SERVER_TEXT:
                            byte[] bytes = new byte[1];
                            StringBuilder info = new StringBuilder();
                            while (inputStream.read(bytes) != -1) {
                                String str = new String(bytes);
                                if (str.equals("\n")) {
                                    break;
                                }
                                info.append(new String(bytes));
                            }
                            final String finalInfo = info.toString();
                            Log.i("Lin", "text = " + finalInfo);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mEtReceive.setText(mEtReceive.getText().toString() + socketAddress + " : " + finalInfo + "\n");
                                }
                            });
                            break;
                        case Constant.SERVER_FILE:
                            byte[] remote = new byte[32];
                            String md5 = "";
                            if (inputStream.read(remote) != -1) {
                                md5 = nullOfString(new String(remote));
                            }

                            final String root = Environment.getExternalStorageDirectory().getPath();
                            Log.i("Lin", root);
                            byte[] inputByte = new byte[1024 * 1024];
                            int len = 0;
                            long fileSize = 0;

                            DataInputStream dis = new DataInputStream(inputStream);
                            // 文件名和长度
                            String fileName = dis.readUTF();
                            final long fileLength = dis.readLong();
                            Log.i("Lin", "fileName = " + fileName);
                            Log.i("Lin", "fileLength = " + fileLength);
                            mPath = root + "/ECG/" + fileName;
                            File file = new File(root + "/ECG/");
                            if (!file.exists()) file.mkdir();
                            file = new File(mPath);
                            FileOutputStream fileOutputStream = new FileOutputStream(file);
                            String fileMD5 = nullOfString(getFileMD5(new File(mPath)));
                            while (!md5.equals(fileMD5) && (len = dis.read(inputByte, 0, inputByte.length)) > 0) {
                                fileSize += len;
                                fileOutputStream.write(inputByte, 0, len);
                                fileOutputStream.flush();
                                fileMD5 = nullOfString(getFileMD5(new File(mPath)));
                                Log.i("Lin", "md5 = " + md5 + " file = " + fileMD5);
                                Log.i("Lin", "fileLength = " + fileLength + " fileSize = " + fileSize + " " + (fileSize * 100 / fileLength) + "%")
                                ;
                                final long finalFileSize = fileSize;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.setMessage((finalFileSize * 100 / fileLength) + "%");
                                        mProgressDialog.show();
                                    }
                                });
                                if (md5.equals(fileMD5)) {
                                    fileOutputStream.close();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mProgressDialog.hide();
                                        }
                                    });
                                }
                            }
                            Log.i("Lin", "md52 = " + md5 + " file2 = " + getFileMD5(file));
                            fileMD5 = nullOfString(getFileMD5(new File(mPath)));
                            Log.i("Lin", "file = " + fileMD5);
                            final String finalFileMD = fileMD5;
                            final String finalMd = md5;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mEtReceive.setText(mEtReceive.getText().toString() + "文件路径：" + mPath + "\n");
                                    mEtReceive.setText(mEtReceive.getText().toString() + "file = " + finalFileMD + "\n");
                                    mEtReceive.setText(mEtReceive.getText().toString() + "text = " + finalMd + "\n");
                                }
                            });
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Lin", e.toString());
            }
        }
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

    public String nullOfString(String str) {
        if (str == null) {
            str = "";
        }
        return str;
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
