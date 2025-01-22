#include <cstdio>
#include <wait.h>
#include <string>

#define EXIT_RESTART_SERVER 10

struct exec_result {
    std::string message;
    int exit;
};


exec_result exec(const char *_Nonnull);