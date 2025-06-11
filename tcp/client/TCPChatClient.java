import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * TCP聊天客户端
 * 功能：
 * 1. 连接到TCP聊天服务器
 * 2. 发送和接收聊天消息
 * 3. 支持聊天室命令
 * 4. 多线程处理消息接收和发送
 */
public class TCPChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Scanner scanner;
    private boolean isConnected = false;
    private String username;
    
    // 消息接收线程
    private Thread messageReceiver;
    
    public TCPChatClient() {
        scanner = new Scanner(System.in);
    }
    
    /**
     * 连接到服务器
     */
    public boolean connect(String host, int port) {
        try {
            System.out.println("正在连接到服务器 " + host + ":" + port + "...");
            
            // 创建Socket连接
            socket = new Socket(host, port);
            
            // 创建输入输出流
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            
            isConnected = true;
            System.out.println("连接成功！");
            
            return true;
            
        } catch (IOException e) {
            System.err.println("连接失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 处理登录过程
     */
    public boolean login() {
        try {
            // 接收服务器的欢迎消息
            String welcomeMessage = reader.readLine();
            System.out.println(welcomeMessage);
            
            // 输入用户名
            System.out.print("请输入用户名: ");
            username = scanner.nextLine().trim();
            
            // 发送用户名到服务器
            writer.println(username);
            
            // 接收服务器响应
            String response = reader.readLine();
            System.out.println(response);
            
            if (response.startsWith("SUCCESS:")) {
                System.out.println("\n=== 欢迎来到TCP聊天室 ===");
                System.out.println("输入消息并按回车发送");
                System.out.println("输入 /help 查看命令帮助");
                System.out.println("输入 /quit 退出聊天室");
                System.out.println("========================\n");
                return true;
            } else {
                System.err.println("登录失败，程序将退出");
                return false;
            }
            
        } catch (IOException e) {
            System.err.println("登录过程中出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 启动客户端
     */
    public void start() {
        if (!isConnected) {
            System.err.println("未连接到服务器");
            return;
        }
        
        // 启动消息接收线程
        startMessageReceiver();
        
        // 主线程处理用户输入
        handleUserInput();
    }
    
    /**
     * 启动消息接收线程
     */
    private void startMessageReceiver() {
        messageReceiver = new Thread(() -> {
            try {
                String message;
                while (isConnected && (message = reader.readLine()) != null) {
                    // 显示接收到的消息
                    System.out.println(message);
                }
            } catch (IOException e) {
                if (isConnected) {
                    System.err.println("接收消息时出错: " + e.getMessage());
                }
            }
        });
        
        messageReceiver.setDaemon(true); // 设置为守护线程
        messageReceiver.start();
    }
    
    /**
     * 处理用户输入
     */
    private void handleUserInput() {
        try {
            String input;
            while (isConnected && (input = scanner.nextLine()) != null) {
                
                // 检查是否为退出命令
                if (input.equals("/quit") || input.equals("/exit")) {
                    break;
                }
                
                // 发送消息到服务器
                writer.println(input);
                
                // 检查发送是否成功
                if (writer.checkError()) {
                    System.err.println("发送消息失败，连接可能已断开");
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("处理用户输入时出错: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        isConnected = false;
        
        try {
            if (writer != null) {
                writer.println("/quit"); // 通知服务器客户端退出
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            System.out.println("已断开连接，再见！");
            
        } catch (IOException e) {
            System.err.println("断开连接时出错: " + e.getMessage());
        }
    }
    
    /**
     * 显示使用帮助
     */
    private static void showUsage() {
        System.out.println("TCP聊天客户端使用说明:");
        System.out.println("java TCPChatClient [服务器地址] [端口号]");
        System.out.println("例如:");
        System.out.println("  java TCPChatClient                    # 连接到 localhost:8888");
        System.out.println("  java TCPChatClient 192.168.1.100      # 连接到 192.168.1.100:8888");
        System.out.println("  java TCPChatClient 192.168.1.100 9999 # 连接到 192.168.1.100:9999");
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        
        // 解析命令行参数
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("端口号格式错误: " + args[1]);
                showUsage();
                return;
            }
        }
        if (args.length > 2) {
            System.err.println("参数过多");
            showUsage();
            return;
        }
        
        // 创建并启动客户端
        TCPChatClient client = new TCPChatClient();
        
        // 添加关闭钩子，确保正常断开连接
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        
        // 连接服务器
        if (client.connect(host, port)) {
            // 登录
            if (client.login()) {
                // 开始聊天
                client.start();
            }
        }
        
        System.exit(0);
    }
}
