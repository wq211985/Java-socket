import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * UDP聊天客户端
 * 功能：
 * 1. 连接到UDP聊天服务器
 * 2. 发送和接收UDP数据报
 * 3. 支持聊天室命令
 * 4. 多线程处理消息接收和发送
 */
public class UDPChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8889;
    
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private Scanner scanner;
    private boolean isConnected = false;
    private String username;
    
    // 消息接收线程
    private Thread messageReceiver;
    
    public UDPChatClient() {
        scanner = new Scanner(System.in);
    }
    
    /**
     * 连接到服务器
     */
    public boolean connect(String host, int port) {
        try {
            System.out.println("正在连接到UDP服务器 " + host + ":" + port + "...");
            
            // 创建UDP Socket
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(host);
            serverPort = port;
            
            isConnected = true;
            System.out.println("UDP Socket创建成功！");
            
            return true;
            
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 处理登录过程
     */
    public boolean login() {
        try {
            // 输入用户名
            System.out.print("请输入用户名: ");
            username = scanner.nextLine().trim();
            
            // 发送注册请求到服务器
            sendToServer("REGISTER:" + username);
            
            // 启动消息接收线程
            startMessageReceiver();
            
            // 等待服务器响应
            Thread.sleep(1000);
            
            System.out.println("\n=== 欢迎来到UDP聊天室 ===");
            System.out.println("输入消息并按回车发送");
            System.out.println("输入 /help 查看命令帮助");
            System.out.println("输入 /quit 退出聊天室");
            System.out.println("========================\n");
            
            return true;
            
        } catch (Exception e) {
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
        
        // 主线程处理用户输入
        handleUserInput();
    }
    
    /**
     * 启动消息接收线程
     */
    private void startMessageReceiver() {
        messageReceiver = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                
                while (isConnected) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    
                    // 处理服务器响应
                    if (message.startsWith("SUCCESS:") || message.startsWith("ERROR:")) {
                        System.out.println(message);
                    } else {
                        // 显示聊天消息
                        System.out.println(message);
                    }
                }
            } catch (Exception e) {
                if (isConnected) {
                    System.err.println("接收消息时出错: " + e.getMessage());
                }
            }
        });
        
        messageReceiver.setDaemon(true);
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
                
                // 检查是否为命令
                if (input.startsWith("/")) {
                    sendToServer("COMMAND:" + input);
                } else {
                    // 普通聊天消息
                    sendToServer("MESSAGE:" + input);
                }
            }
        } catch (Exception e) {
            System.err.println("处理用户输入时出错: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    /**
     * 发送消息到服务器
     */
    private void sendToServer(String message) {
        try {
            byte[] data = message.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(
                data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        isConnected = false;
        
        try {
            // 发送注销消息
            if (socket != null && !socket.isClosed()) {
                sendToServer("UNREGISTER:" + username);
                Thread.sleep(100); // 等待消息发送
                socket.close();
            }
            
            System.out.println("已断开连接，再见！");
            
        } catch (Exception e) {
            System.err.println("断开连接时出错: " + e.getMessage());
        }
    }
    
    /**
     * 显示使用帮助
     */
    private static void showUsage() {
        System.out.println("UDP聊天客户端使用说明:");
        System.out.println("java UDPChatClient [服务器地址] [端口号]");
        System.out.println("例如:");
        System.out.println("  java UDPChatClient                    # 连接到 localhost:8889");
        System.out.println("  java UDPChatClient 192.168.1.100      # 连接到 192.168.1.100:8889");
        System.out.println("  java UDPChatClient 192.168.1.100 9999 # 连接到 192.168.1.100:9999");
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
        UDPChatClient client = new UDPChatClient();
        
        // 添加关闭钩子
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
