#include "logger.h"

std::string getFormattedTime() {
    auto now = std::chrono::system_clock::now();
    std::time_t currentTime = std::chrono::system_clock::to_time_t(now);
    std::tm *timeInfo = std::localtime(&currentTime);
    std::stringstream ss;
    ss << std::put_time(timeInfo, "%Y-%m-%d %H:%M:%S");
    return ss.str();
}

void info(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    std::cout << getFormattedTime() << " " << LOG_TAG << " I ";
    vprintf(fmt, args);
    std::cout << std::endl;
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, args);
    va_end(args);
}

void warn(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    std::cout << getFormattedTime() << " " << LOG_TAG << " W ";
    vprintf(fmt, args);
    std::cout << std::endl;
    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, fmt, args);
    va_end(args);
}

void error(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    std::cout << getFormattedTime() << " " << LOG_TAG << " E ";
    vprintf(fmt, args);
    std::cout << std::endl;
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, args);
    va_end(args);
}