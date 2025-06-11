# Socket编程聊天程序

## 项目概述
本项目实现了基于TCP和UDP协议的Socket聊天程序。
项目展示了面向连接(TCP)和无连接(UDP)两种不同网络通信方式的编程实现。

## 项目结构
```
socket/
├── tcp/                    # TCP版本聊天程序
│   ├── server/            # TCP服务器端
│   │   ├── TCPChatServer.java    # TCP服务器主类
│   │   └── ClientHandler.java    # 客户端处理器
│   ├── client/            # TCP客户端
│       └── TCPChatClient.java    # TCP客户端主类
├── udp/                   # UDP版本聊天程序
│   ├── server/            # UDP服务器端
│   │   └── UDPChatServer.java    # UDP服务器主类
│   ├── client/            # UDP客户端
│   │   └── UDPChatClient.java    # UDP客户端主类
└── README.md              # 项目说明
```

## 实现状态

### ✅ 第一阶段：TCP版本（已完成）
1. TCP服务器端 - 监听连接，处理多客户端
2. TCP客户端 - 连接服务器，发送接收消息
3. 支持多用户同时在线聊天
4. 完整的用户管理和消息广播

### ✅ 第二阶段：UDP版本（已完成）
1. UDP服务器端 - 接收数据报，消息转发
2. UDP客户端 - 发送数据报，接收消息
3. 无连接通信模式
4. 数据报包处理机制

## 技术要点
- Java Socket编程（TCP/UDP）
- 多线程并发处理
- 网络协议对比分析
- 异常处理和资源管理
- 字符编码处理（UTF-8）

## 功能
- ✅ TCP面向连接的Socket编程
- ✅ UDP无连接的Socket编程（加分项）
- ✅ 客户端发送，服务器接收并回复
- ✅ Java语言实现
- ✅ C/S架构模式
- ✅ Windows/Linux兼容
