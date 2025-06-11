import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * UDP聊天服务器
 * 功能：
 * 1. 接收客户端UDP数据报
 * 2. 管理客户端地址列表
 * 3. 转发消息给所有注册客户端
 * 4. 处理客户端注册和注销
 */
public class UDPChatServer {
    private static final int PORT = 8889;  // UDP服务器端口
    private DatagramSocket socket;
    private boolean isRunning = false;
    
    // 存储所有注册的客户端地址和用户名
    private Map<String, InetSocketAddress> clients = new ConcurrentHashMap<>();
    private Map<InetSocketAddress, String> addressToUsername = new ConcurrentHashMap<>();
    
    public UDPChatServer() {
        try {
            socket = new DatagramSocket(PORT);
            System.out.println("UDP聊天服务器启动成功！");
            System.out.println("服务器地址: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("监听端口: " + PORT);
            System.out.println("等待客户端连接...");
        } catch (Exception e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动服务器，开始监听UDP数据报
     */
    public void start() {
        isRunning = true;
        
        while (isRunning) {
            try {
                // 接收数据报
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // 处理接收到的消息
                String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                InetSocketAddress clientAddress = new InetSocketAddress(
                    packet.getAddress(), packet.getPort());
                
                handleMessage(message, clientAddress);
                
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("接收数据报时出错: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 处理客户端消息
     */
    private void handleMessage(String message, InetSocketAddress clientAddress) {
        try {
            if (message.startsWith("REGISTER:")) {
                // 客户端注册
                String username = message.substring(9);
                handleClientRegister(username, clientAddress);
            } else if (message.startsWith("UNREGISTER:")) {
                // 客户端注销
                handleClientUnregister(clientAddress);
            } else if (message.startsWith("MESSAGE:")) {
                // 聊天消息
                String chatMessage = message.substring(8);
                handleChatMessage(chatMessage, clientAddress);
            } else if (message.startsWith("COMMAND:")) {
                // 命令处理
                String command = message.substring(8);
                handleCommand(command, clientAddress);
            }
        } catch (Exception e) {
            System.err.println("处理消息时出错: " + e.getMessage());
        }
    }
    
    /**
     * 处理客户端注册
     */
    private void handleClientRegister(String username, InetSocketAddress clientAddress) {
        // 检查用户名是否已存在
        if (clients.containsKey(username)) {
            sendToClient("ERROR:用户名已存在", clientAddress);
            return;
        }
        
        // 注册客户端
        clients.put(username, clientAddress);
        addressToUsername.put(clientAddress, username);
        
        System.out.println("用户 " + username + " 注册成功，地址: " + clientAddress);
        System.out.println("当前在线人数: " + clients.size());
        
        // 发送注册成功消息
        sendToClient("SUCCESS:注册成功！欢迎 " + username, clientAddress);
        
        // 通知所有客户端有新用户加入
        broadcastMessage("系统消息", username + " 加入了聊天室");
    }
    
    /**
     * 处理客户端注销
     */
    private void handleClientUnregister(InetSocketAddress clientAddress) {
        String username = addressToUsername.get(clientAddress);
        if (username != null) {
            clients.remove(username);
            addressToUsername.remove(clientAddress);
            
            System.out.println("用户 " + username + " 注销，当前在线人数: " + clients.size());
            
            // 通知所有客户端有用户离开
            broadcastMessage("系统消息", username + " 离开了聊天室");
        }
    }
    
    /**
     * 处理聊天消息
     */
    private void handleChatMessage(String message, InetSocketAddress clientAddress) {
        String username = addressToUsername.get(clientAddress);
        if (username != null) {
            broadcastMessage(username, message);
        }
    }
    
    /**
     * 处理客户端命令
     */
    private void handleCommand(String command, InetSocketAddress clientAddress) {
        if (command.equals("/users")) {
            sendOnlineUsers(clientAddress);
        } else if (command.equals("/help")) {
            sendHelpMessage(clientAddress);
        } else {
            sendToClient("未知命令: " + command, clientAddress);
        }
    }
    
    /**
     * 广播消息给所有客户端
     */
    private void broadcastMessage(String sender, String message) {
        String fullMessage = "[" + getCurrentTime() + "] " + sender + ": " + message;
        
        // 遍历所有客户端，发送消息
        for (InetSocketAddress clientAddress : clients.values()) {
            sendToClient(fullMessage, clientAddress);
        }
    }
    
    /**
     * 发送消息给指定客户端
     */
    private void sendToClient(String message, InetSocketAddress clientAddress) {
        try {
            byte[] data = message.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(
                data, data.length, clientAddress.getAddress(), clientAddress.getPort());
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
            // 移除无法到达的客户端
            String username = addressToUsername.get(clientAddress);
            if (username != null) {
                clients.remove(username);
                addressToUsername.remove(clientAddress);
            }
        }
    }
    
    /**
     * 发送在线用户列表
     */
    private void sendOnlineUsers(InetSocketAddress clientAddress) {
        StringBuilder userList = new StringBuilder("当前在线用户 (");
        userList.append(clients.size()).append("人): ");
        
        for (String user : clients.keySet()) {
            userList.append(user).append(" ");
        }
        
        sendToClient("系统消息: " + userList.toString(), clientAddress);
    }
    
    /**
     * 发送帮助信息
     */
    private void sendHelpMessage(InetSocketAddress clientAddress) {
        sendToClient("=== UDP聊天室命令帮助 ===", clientAddress);
        sendToClient("/users - 查看在线用户列表", clientAddress);
        sendToClient("/help - 显示此帮助信息", clientAddress);
        sendToClient("/quit - 退出聊天室", clientAddress);
        sendToClient("直接输入文字即可发送聊天消息", clientAddress);
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
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        UDPChatServer server = new UDPChatServer();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        // 启动服务器
        server.start();
    }
}
