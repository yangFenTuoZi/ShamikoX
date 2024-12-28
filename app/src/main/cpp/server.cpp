#include <iostream>
#include <sstream>
#include <fstream>
#include <cstring>
#include <cstdlib>
#include <unistd.h>
#include <arpa/inet.h>
#include <thread>
#include <wait.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

#define whiteListFile "/data/adb/shamiko/whitelist"
#define PORT 11451
#define BACKLOG 2
#define TIMEOUT_SECONDS 3

// adb快速调试命令：$(pm path yangFenTuoZi.shamikox | sed 's/.*package\://;s\/base\.apk//')/lib/arm64/libserver.so

bool change_shamiko_mode(bool whitelist) {
    if (whitelist) {
        std::ofstream file(whiteListFile);
        if (file.is_open()) {
            file.close();
        }
        return file.good();
    } else {
        std::remove(whiteListFile);
        std::ifstream file(whiteListFile);
        return file.good();
    }
}

void request_root(int uid) {
    pid_t pid1 = fork();
    if (pid1 == 0) {
        std::ofstream file(whiteListFile);
        bool last_status = file.good();
        change_shamiko_mode(false);
        pid_t pid = fork();
        if (pid == 0) {
            setuid(uid);
            exit(system("su -c 'echo Successful'"));
        } else {
            int status;
            // 等待子进程退出，并获取子进程退出状态
            waitpid(pid, &status, 0);
            if (last_status) {
                change_shamiko_mode(true);
            }
        }
    }
}

void handle_client(int client_socket) {
    char buffer[1024];

    // 设置socket超时时间
    struct timeval timeout;
    timeout.tv_sec = TIMEOUT_SECONDS;
    timeout.tv_usec = 0;
    if (setsockopt(client_socket, SOL_SOCKET, SO_RCVTIMEO, (const char *) &timeout,
                   sizeof(timeout)) < 0) {
        perror("setsockopt failed");
        close(client_socket);
        return;
    }

    // 接收客户端消息
    while (true) {
        std::string message;
        int bytes_received = 0;

        // 从socket中读取数据
        while (true) {
            bytes_received = read(client_socket, buffer, sizeof(buffer) - 1);
            if (bytes_received < 0) {
                if (errno == EWOULDBLOCK || errno == EAGAIN) {
                    // 超时，关闭连接
                    std::cout << "Client timeout, disconnecting..." << std::endl;
                    close(client_socket);
                    return;
                } else {
                    perror("Receive failed");
                    close(client_socket);
                    return;
                }
            }
            if (bytes_received == 0) {
                std::cout << "Client disconnected" << std::endl;
                close(client_socket);
                return;
            }

            // 将接收到的数据添加到消息中，直到找到换行符
            buffer[bytes_received] = '\0';
            message.append(buffer);

            // 检查是否找到了换行符
            if (message.find('\n') != std::string::npos) {
                break;
            }
        }

        message.erase(std::remove(message.begin(), message.end(), '\n'), message.end());
        message.erase(std::remove(message.begin(), message.end(), '\xFF'), message.end());

        int msg = std::stoi(message);

        /*
         *  0       字符串转换为int错误/刷新存在感
         * -1       退出
         * -2       切换白名单模式
         * -3       切换黑名单模式
         * -4       获取白名单模式状态
         * -5       为指定uid请求root权限
         * others   为前台app请求root权限
         */
        if (msg == 0) {
            continue;
        } else if (msg == -1) {
            std::cout << "Server shutting down..." << std::endl;
            close(client_socket);
            exit(EXIT_SUCCESS);
        } else if (msg == -2) {
            std::cout << "Change whitelist mode" << std::endl;
            const char *result = change_shamiko_mode(true) ? "1\n" : "0\n";
            send(client_socket, result, strlen(result), 0);
        } else if (msg == -3) {
            std::cout << "Change blacklist mode" << std::endl;
            const char *result = change_shamiko_mode(false) ? "1\n" : "0\n";
            send(client_socket, result, strlen(result), 0);
        } else if (msg == -4) {
            std::ifstream file(whiteListFile);
            const char *result = file.good() ? "1\n" : "0\n";
            send(client_socket, result, strlen(result), 0);
        } else if (msg == -5) {
            std::cout << "Request root for foreground app" << std::endl;
            FILE *fp = popen(
                    R"(cmd statusbar collapse;dumpsys package $(dumpsys window | grep mTopFullscreenOpaqueWindowState | sed 's/ /\n/g' | tail -n 1 | sed 's/\/.*$//g') | grep uid= | tail -n 1 | sed 's/.*uid=\([0-9]*\).*/\1/')",
                    "r");
            if (fp == nullptr) {
                perror("popen failed");
                close(client_socket);
                exit(EXIT_FAILURE);
            }
            char buffer[1024];
            int uid = 0;
            while (fgets(buffer, sizeof(buffer), fp) != nullptr) {
                uid = atoi(buffer);
            }
            pclose(fp);
            request_root(uid);
        } else {
            if (msg < 1) continue;
            std::cout << "Request root for uid=" << msg << std::endl;
            request_root(msg);
        }
    }
}

int main() {
    if (getuid() != 0) {
        std::cerr << "Please run by root user" << std::endl;
        exit(EXIT_FAILURE);
    }

    int server_socket, client_socket;
    struct sockaddr_in address{};
    int opt = 1;
    int addrlen = sizeof(address);

    // 创建Socket
    if ((server_socket = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("Socket failed");
        exit(EXIT_FAILURE);
    }

    // 附加选项
    setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    // 设置地址
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    // 绑定
    if (bind(server_socket, (struct sockaddr *) &address, sizeof(address)) < 0) {
        perror("Bind failed");
        close(server_socket);
        exit(EXIT_FAILURE);
    }

    // 开始监听
    if (listen(server_socket, BACKLOG) < 0) {
        perror("Listen failed");
        close(server_socket);
        exit(EXIT_FAILURE);
    }

    std::cout << "Waiting for connections..." << std::endl;

    // 持续接受连接
    while (true) {
        if ((client_socket = accept(server_socket, (struct sockaddr *) &address,
                                    (socklen_t *) &addrlen)) < 0) {
            perror("Accept failed");
            continue;
        }

        std::cout << "Connected to client" << std::endl;

        // 处理客户端连接
        handle_client(client_socket);

        // 处理完客户端请求后，关闭 client_socket
        close(client_socket);
        std::cout << "Waiting for a new connection..." << std::endl;
    }

    close(server_socket);
    return 0;
}
