package com.github.tvbox.osc.server;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 远程输入 HTTP 服务 (酷9特性)
 * 手机扫码后可通过网页输入文字/数字控制TVBox
 */
public class RemoteServer {

    private static final String TAG = "RemoteServer";
    private static final int PORT = 9978;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean isRunning = false;
    private OnRemoteInputListener listener;

    public interface OnRemoteInputListener {
        void onInput(String text);
        void onCommand(String cmd);
    }

    public void setListener(OnRemoteInputListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        executorService = Executors.newCachedThreadPool();
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                Log.d(TAG, "Remote server started on port " + PORT);
                while (isRunning) {
                    Socket client = serverSocket.accept();
                    executorService.execute(() -> handleClient(client));
                }
            } catch (IOException e) {
                Log.e(TAG, "Server error: " + e.getMessage());
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
        if (executorService != null) executorService.shutdown();
    }

    private void handleClient(Socket client) {
        try {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            byte[] buffer = new byte[4096];
            int len = in.read(buffer);
            if (len <= 0) return;

            String request = new String(buffer, 0, len, StandardCharsets.UTF_8);
            String response;

            if (request.contains("GET / ")) {
                response = buildHtmlPage();
            } else if (request.contains("POST /input")) {
                String body = extractBody(request);
                if (listener != null) listener.onInput(body);
                response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK";
            } else if (request.contains("POST /cmd")) {
                String body = extractBody(request);
                if (listener != null) listener.onCommand(body);
                response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK";
            } else {
                response = "HTTP/1.1 404 Not Found\r\n\r\n";
            }

            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
            client.close();
        } catch (IOException e) {
            Log.e(TAG, "Client error: " + e.getMessage());
        }
    }

    private String extractBody(String request) {
        int idx = request.indexOf("\r\n\r\n");
        if (idx > 0) return request.substring(idx + 4).trim();
        return "";
    }

    private String buildHtmlPage() {
        String html = "<!DOCTYPE html><html><head>" +
            "<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<title>TVBox Remote</title>" +
            "<style>" +
            "body{background:#1a1a1a;color:#fff;font-family:sans-serif;padding:20px;text-align:center;}" +
            "input{width:80%;padding:15px;font-size:18px;border-radius:8px;border:none;margin:10px 0;}" +
            "button{width:40%;padding:15px;margin:5px;font-size:16px;border-radius:8px;border:none;background:#1890ff;color:#fff;}" +
            ".keys{display:flex;flex-wrap:wrap;justify-content:center;gap:5px;margin-top:20px;}" +
            ".key{width:60px;height:60px;font-size:20px;background:#333;border-radius:8px;display:flex;align-items:center;justify-content:center;cursor:pointer;}" +
            "</style></head><body>" +
            "<h2>TVBox Remote Input</h2>" +
            "<input type=\"text\" id=\"textInput\" placeholder=\"输入文字...\" onkeydown=\"if(event.key==='Enter')sendText()\">" +
            "<button onclick=\"sendText()\">发送</button>" +
            "<div class=\"keys\">" +
            "<div class=\"key\" onclick=\"sendCmd('UP')\">▲</div>" +
            "<div class=\"key\" onclick=\"sendCmd('DOWN')\">▼</div>" +
            "<div class=\"key\" onclick=\"sendCmd('LEFT')\">◀</div>" +
            "<div class=\"key\" onclick=\"sendCmd('RIGHT')\">▶</div>" +
            "<div class=\"key\" onclick=\"sendCmd('OK')\">OK</div>" +
            "<div class=\"key\" onclick=\"sendCmd('BACK')\">返回</div>" +
            "<div class=\"key\" onclick=\"sendCmd('MENU')\">菜单</div>" +
            "<div class=\"key\" onclick=\"sendCmd('HOME')\">主页</div>" +
            "</div>" +
            "<script>" +
            "function sendText(){var t=document.getElementById('textInput').value;fetch('/input',{method:'POST',body:t});document.getElementById('textInput').value='';}" +
            "function sendCmd(c){fetch('/cmd',{method:'POST',body:c});}" +
            "</script></body></html>";
        return "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n" + html;
    }

    public int getPort() { return PORT; }
}
