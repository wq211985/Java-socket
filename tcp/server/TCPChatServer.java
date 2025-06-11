import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * TCP聊天服务器
 * 功能：
 * 1. 监听客户端连接
 * 2. 处理多客户端同时在线
 * 3. 转发消息给所有客户端
 * 4. 管理客户端列表
 */
public class TCPChatServer {
    private static final int PORT = 8888;  // 服务器端口
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    
    // 存储所有连接的客户端
    public Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    
    public TCPChatServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("TCP聊天服务器启动成功！");
            System.out.println("服务器地址: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("监听端口: " + PORT);
            System.out.println("等待客户端连接...");
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动服务器，开始监听客户端连接
     */
    public void start() {
        isRunning = true;
        
        while (isRunning) {
            try {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                
                // 为每个客户端创建处理线程
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                threadPool.execute(clientHandler);
                
                System.out.println("新客户端连接: " + clientSocket.getInetAddress().getHostAddress());
                
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("接受客户端连接时出错: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 添加客户端到在线列表
     */
    public synchronized void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("用户 " + username + " 加入聊天室，当前在线人数: " + clients.size());
        
        // 通知所有客户端有新用户加入
        broadcastMessage("系统消息", username + " 加入了聊天室");
    }
    
    /**
     * 从在线列表移除客户端
     */
    public synchronized void removeClient(String username) {
        clients.remove(username);
        System.out.println("用户 " + username + " 离开聊天室，当前在线人数: " + clients.size());
        
        // 通知所有客户端有用户离开
        broadcastMessage("系统消息", username + " 离开了聊天室");
    }
    
    /**
     * 广播消息给所有在线客户端
     */
    public synchronized void broadcastMessage(String sender, String message) {
        String fullMessage = "[" + getCurrentTime() + "] " + sender + ": " + message;
        
        // 遍历所有客户端，发送消息
        Iterator<Map.Entry<String, ClientHandler>> iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ClientHandler> entry = iterator.next();
            ClientHandler handler = entry.getValue();
            
            if (!handler.sendMessage(fullMessage)) {
                // 如果发送失败，移除该客户端
                iterator.remove();
                System.out.println("移除断开连接的客户端: " + entry.getKey());
            }
        }
    }
    
    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
    }
    
    /**
     * 停止服务器
     */
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            System.err.println("关闭服务器时出错: " + e.getMessage());
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        TCPChatServer server = new TCPChatServer();
        
        // 添加关闭钩子，确保服务器正常关闭
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        // 启动服务器
        server.start();
    }
}
