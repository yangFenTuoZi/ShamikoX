#include <android/log.h>
#include <ctime>
#include <sys/resource.h>
#include <string>
#include <iostream>
#include <iomanip>
#include <sstream>

#define LOG_TAG "shamikox_server"

void info(const char *_Nonnull, ...);
void warn(const char *_Nonnull, ...);
void error(const char *_Nonnull, ...);