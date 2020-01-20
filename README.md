# 目标
Demo是通过Java **ServerSocket** 和 **Socket** 通信实现客户端**发送消息**和**发送文件**到服务器,服务器接收到**消息和文件**，并且实现解决**inputStream.read()**的**阻塞**问题思路。
#  服务器端
## 创建ServerSocket服务器
```
serverSocket = new ServerSocket(port);//首先创建一个服务端口
//等待客户端的连接请求
socket = serverSocket.accept();
```
## 等待Socket客户端连接

```
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
```
## 接收Socket客户端信息
为了防止接收消息时，服务器**inputStream.read()**读取消息时产生**阻塞**，以换行符**("\n")**结束**inputStream.read()**

```
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
```
## 接收Socket客户端文件
为了防止接收消息时，服务器**inputStream.read()**接受文件时产生**阻塞**，以**文件MD5校验码**进行校验，从而结束**inputStream.read()**

```

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
```

#  客户端
## 创建Socket客户端
## 连接ServerSocket服务器

```
socket = new Socket();
socket.connect(new InetSocketAddress(ip, port));
//ip= 服务器ip
//port= 服务器端口
```
## 向ServerSocket服务器发送消息

```
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
```
为了防止发送消息时，服务器**inputStream.read()**读取消息时产生**阻塞**，客户端以换行符结束发送(**om.write("\n".getBytes());**)


## 向ServerSocket服务器发送文件

```


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
```
为了防止发送文件时，服务器**inputStream.read()**接受文件时产生**阻塞**，客户端先向服务器发送**文件MD5校验码**再发送文件，服务器对接收文件进行校验从而结束**inputStream.read()**阻塞。
# Demo中发送格式
## 字符：SERVER_TEXT + 字符 + 换行符（\n）
## 文件：SERVER_FILE + 文件MD5校验码 + 文件

# 获取文件MD5

```


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
```
# [GitHub](https://github.com/Lin-King/socketdemo)
# THE END







