#include "daemon.h"

int main(int argc, char *argv[]) {
    std::string s_title = "shamikox_daemon";
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

    std::string abi;
    std::string abiProperty = exec("getprop ro.product.cpu.abi").message;
    if (abiProperty == "arm64-v8a\n") {
        abi = "arm64";
    } else if (abiProperty == "armeabi-v7a\n") {
        abi = "arm";
    } else if (abiProperty == "x86\n") {
        abi = "x86";
    } else if (abiProperty == "x86_64\n") {
        abi = "x86_64";
    } else {
        error("不支持的ABI: %s", abiProperty.data());
        exit(255);
    }

    int exit_value = EXIT_RESTART_SERVER;
    while (exit_value == EXIT_RESTART_SERVER) {
        std::string command;
        command.append("$(dirname $(pm path yangFenTuoZi.shamikox | sed 's/^package://'))/lib/");
        command.append(abi);
        command.append("/libserver.so");
        exit_value = WEXITSTATUS(system(command.data()));
    }
    exit(exit_value);
    return 0;
}