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
#include <sys/stat.h>
#include <android/log.h>
#include <sys/time.h>
#include <sys/resource.h>

#define LOG_TAG "shamikox_server"

#define whiteListFile "/data/adb/shamiko/whitelist"
#define autoRestartServer "/data/adb/shamiko/auto_restart_server"
#define PORT 11451
#define BACKLOG 1
#define TIMEOUT_SECONDS 3
#define EXIT_RESTART_SERVER 10

struct exec_result {
    std::string message;
    int exit;
};

[[noreturn]] void main_server();

void on_app_upgrade(bool);

void app_watch_thread();

bool change_shamiko_mode(bool);

void request_root(int);

void handle_client(int);

exec_result exec(const char *_Nonnull);

void info(const char *_Nonnull, ...);

void warn(const char *_Nonnull, ...);

void error(const char *_Nonnull, ...);