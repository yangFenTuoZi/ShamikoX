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
#include "logger.h"
#include "global.h"

#define whiteListFile "/data/adb/shamiko/whitelist"
#define autoRestartServer "/data/adb/shamiko/auto_restart_server"
#define PORT 11451
#define BACKLOG 1
#define TIMEOUT_SECONDS 3

[[noreturn]] void main_server();

void on_app_upgrade(bool);

void app_watch_thread();

bool change_shamiko_mode(bool);

void request_root(int);

void handle_client(int);
