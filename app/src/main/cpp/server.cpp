#include "server.h"

// adb快速调试命令：$(pm path yangFenTuoZi.shamikox | sed 's/.*package\://;s\/base\.apk//')/lib/arm64/libserver.so

void on_app_upgrade(bool removed) {
    if (removed) {
        info("App被卸载, 服务关闭");
        exit(EXIT_SUCCESS);
    } else {
        std::ifstream file(autoRestartServer);
        if (file.good()) {
            info("App更新, 服务重启");
            exit(EXIT_RESTART_SERVER);
        } else {
            info("App更新, 服务关闭");
            exit(EXIT_SUCCESS);
        }
    }
}

void app_watch_thread() {
    exec_result result = exec("pm path yangFenTuoZi.shamikox | sed 's/^package://' | tr -d '\\n'");
    if (result.exit != 0) {
        on_app_upgrade(true);
        return;
    }
    const char *file_path = result.message.data();

    struct stat file_info;
    if (stat(file_path, &file_info) != 0) {
        if (errno == ENOENT) {
            on_app_upgrade(true);
            return;
        } else {
            perror("stat");
            return;
        }
    }
    time_t last_modified_time = file_info.st_mtime;

    while (true) {
        sleep(1); // 避免高 CPU 占用

        if (stat(file_path, &file_info) != 0) {
            if (errno == ENOENT) {
                on_app_upgrade(system("pm path yangFenTuoZi.shamikox 2>&1 >/dev/null") != 0);
                return;
            } else {
                perror("stat");
                break;
            }
        }

        if (file_info.st_mtime != last_modified_time) {
            on_app_upgrade(system("pm path yangFenTuoZi.shamikox 2>&1 >/dev/null") != 0);
            return;
        }
    }
}

int main(int argc, char *argv[]) {
    std::string s_title = "shamikox_server";
    size_t i_size = s_title.size();
    memcpy(argv[0], s_title.data(), i_size);
    argv[0][i_size] = '\0';

    if (getuid() != 0) {
        error("请使用Root用户执行");
        exit(EXIT_FAILURE);
    }

    exec_result result = exec("pm");
    if (result.exit != 255) {
        error("无法访问Package服务(%d)\n可能是SELinux限制, 可以尝试使用不带有tty的命令行执行",
              result.exit);
        exit(EXIT_FAILURE);
    }

    main_server();
    return 0;
}

void main_server() {
    int server_socket, client_socket;
    struct sockaddr_in address{};
    int opt = 1;
    int addrlen = sizeof(address);

    // 创建Socket
    if ((server_socket = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("创建Socket服务失败");
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
        perror("绑定Socket失败");
        close(server_socket);
        exit(EXIT_FAILURE);
    }

    // 开始监听
    if (listen(server_socket, BACKLOG) < 0) {
        perror("监听失败");
        close(server_socket);
        exit(EXIT_FAILURE);
    }

    std::thread th1(app_watch_thread);

    info("等待连接...");

    // 持续接受连接
    while (true) {
        if ((client_socket = accept(server_socket, (struct sockaddr *) &address,
                                    (socklen_t *) &addrlen)) < 0) {
            perror("接受失败");
            continue;
        }

        info("连接成功");

        // 处理客户端连接
        handle_client(client_socket);

        // 处理完客户端请求后，关闭 client_socket
        close(client_socket);
        info("等待新连接...");
    }

    close(server_socket);
}

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
            exec_result result = exec("su -c echo");
            exit(result.exit);
        } else {
            int status;
            // 等待子进程退出，并获取子进程退出状态
            waitpid(pid, &status, 0);
            std::string msg = status == EXIT_SUCCESS ? "已获取Root权限" : "获取Root权限失败";
            info(msg.data());
            if (last_status) {
                change_shamiko_mode(true);
            }
        }
    }
}

void handle_client(int client_socket) {
    char buffer[1024];

    // 设置socket超时时间
    struct timeval timeout{};
    timeout.tv_sec = TIMEOUT_SECONDS;
    timeout.tv_usec = 0;
    if (setsockopt(client_socket, SOL_SOCKET, SO_RCVTIMEO, (const char *) &timeout,
                   sizeof(timeout)) < 0) {
        perror("setsockopt失败");
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
                    warn("客户端超时, 断开连接...");
                    close(client_socket);
                    return;
                } else {
                    perror("接收失败");
                    close(client_socket);
                    return;
                }
            }
            if (bytes_received == 0) {
                info("客户端断开连接");
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
            info("服务关闭...");
            close(client_socket);
            exit(EXIT_SUCCESS);
        } else if (msg == -2) {
            info("切换白名单模式");
            const char *result = change_shamiko_mode(true) ? "1\n" : "0\n";
            send(client_socket, result, strlen(result), 0);
        } else if (msg == -3) {
            info("切换黑名单模式");
            const char *result = change_shamiko_mode(false) ? "1\n" : "0\n";
            send(client_socket, result, strlen(result), 0);
        } else if (msg == -4) {
            std::ifstream file(whiteListFile);
            const char *result = file.good() ? "1\n" : "0\n";
            send(client_socket, result, strlen(result), 0);
        } else if (msg == -5) {
            info("为前台App申请Root权限");
            exec_result result = exec(
                    R"(cmd statusbar collapse;dumpsys package $(dumpsys window | grep mTopFullscreenOpaqueWindowState | sed 's/ /\n/g' | tail -n 1 | sed 's/\/.*$//g') | grep appId= | tail -n 1 | sed 's/.*appId=\([0-9]*\).*/\1/')");
            int uid = 1;
            try {
                uid = stoi(result.message);
            } catch (const std::invalid_argument &e) {
                error("Error: Invalid argument for stoi: %d", e.what());
            }
            request_root(uid);
        } else {
            if (msg < 1) continue;
            info("为App申请Root权限, 该App的UID: %d", msg);
            request_root(msg);
        }
    }
}

