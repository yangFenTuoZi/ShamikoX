#include "global.h"

exec_result exec(const char *_Nonnull command) {
    exec_result result;
    FILE *fp = popen(
            command,
            "r+");
    if (fp == nullptr) {
        perror("popen失败");
    }
    char buffer[1024];
    while (fgets(buffer, sizeof(buffer), fp) != nullptr) {
        result.message.append(buffer);
    }
    result.exit = WEXITSTATUS(pclose(fp));
    return result;
}
