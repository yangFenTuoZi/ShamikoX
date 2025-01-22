cmake_minimum_required(VERSION 3.22.1)

set(CMAKE_CXX_STANDARD 11)

project("server")

add_executable(server
        server.cpp
        logger.cpp
        global.cpp
        ${log-lib})

add_executable(daemon
        daemon.cpp
        logger.cpp
        global.cpp
        ${log-lib})

find_library(log-lib
        log)

target_link_libraries(
        server
        ${log-lib}
)

target_link_libraries(
        daemon
        ${log-lib}
)

set_target_properties(server PROPERTIES PREFIX "lib")
set_target_properties(server PROPERTIES SUFFIX ".so")

set_target_properties(daemon PROPERTIES PREFIX "lib")
set_target_properties(daemon PROPERTIES SUFFIX ".so")