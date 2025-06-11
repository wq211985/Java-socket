import java.io.*;
import java.net.*;

/**
 * 客户端处理器
 * 每个连接的客户端都有一个对应的ClientHandler线程
 * 负责处理该客户端的所有通信
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private TCPChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private boolean isConnected = true;
    
    public ClientHandler(Socket socket, TCPChatServer server) {
        this.clientSocket = socket;
        this.server = server;
        
        try {
            // 创建输入输出流
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        } catch (IOException e) {
            System.err.println("创建客户端处理器时出错: " + e.getMessage());
            closeConnection();
        }
    }
    
    @Override
    public void run() {
        try {
            // 首先接收客户端的用户名
            handleLogin();
            
            // 如果登录成功，开始处理消息
            if (username != null) {
                handleMessages();
            }
            
        } catch (IOException e) {
            System.err.println("处理客户端 " + username + " 时出错: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 处理客户端登录
     */
    private void handleLogin() throws IOException {
        // 发送欢迎消息
        writer.println("欢迎来到TCP聊天室！请输入您的用户名:");
        
        // 接收用户名
        String inputUsername = reader.readLine();
        if (inputUsername != null && !inputUsername.trim().isEmpty()) {
            username = inputUsername.trim();
            
            // 检查用户名是否已存在
            if (server.clients.containsKey(username)) {
                writer.println("ERROR:用户名已存在，请重新连接并使用其他用户名");
                return;
            }
            
            // 登录成功
            writer.println("SUCCESS:登录成功！欢迎 " + username);
            server.addClient(username, this);
            
            // 发送在线用户列表
            sendOnlineUsers();
            
        } else {
            writer.println("ERROR:用户名不能为空");
        }
    }
    
    /**
     * 处理客户端消息
     */
    private void handleMessages() throws IOException {
        String message;
        while (isConnected && (message = reader.readLine()) != null) {
            
            // 处理特殊命令
            if (message.startsWith("/")) {
                handleCommand(message);
            } else {
                // 普通聊天消息，广播给所有客户端
                server.broadcastMessage(username, message);
            }
        }
    }
    
    /**
     * 处理客户端命令
     */
    private void handleCommand(String command) {
        if (command.equals("/quit") || command.equals("/exit")) {
            // 客户端主动退出
            writer.println("再见！");
            closeConnection();
        } else if (command.equals("/users")) {
            // 查看在线用户
            sendOnlineUsers();
        } else if (command.equals("/help")) {
            // 显示帮助信息
            sendHelpMessage();
        } else {
            writer.println("未知命令: " + command + "，输入 /help 查看帮助");
        }
    }
    
    /**
     * 发送在线用户列表
     */
    private void sendOnlineUsers() {
        StringBuilder userList = new StringBuilder("当前在线用户 (");
        userList.append(server.clients.size()).append("人): ");
        
        for (String user : server.clients.keySet()) {
            userList.append(user).append(" ");
        }
        
        writer.println("系统消息: " + userList.toString());
    }
    
    /**
     * 发送帮助信息
     */
    private void sendHelpMessage() {
        writer.println("=== 聊天室命令帮助 ===");
        writer.println("/users - 查看在线用户列表");
        writer.println("/help - 显示此帮助信息");
        writer.println("/quit 或 /exit - 退出聊天室");
        writer.println("直接输入文字即可发送聊天消息");
    }
    
    /**
     * 向客户端发送消息
     */
    public boolean sendMessage(String message) {
        if (writer != null && isConnected) {
            writer.println(message);
            return !writer.checkError();
        }
        return false;
    }
    
    /**
     * 关闭连接
     */
    private void closeConnection() {
        isConnected = false;
        
        // 从服务器移除此客户端
        if (username != null) {
            server.removeClient(username);
        }
        
        // 关闭资源
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭客户端连接时出错: " + e.getMessage());
        }
        
        System.out.println("客户端 " + username + " 连接已关闭");
    }
    
    /**
     * 获取用户名
     */
    public String getUsername() {
        return username;
    }
}
