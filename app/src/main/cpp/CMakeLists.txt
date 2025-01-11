cmake_minimum_required(VERSION 3.22.1)

set(CMAKE_CXX_STANDARD 11)

project("server")

add_executable(server
        server.cpp
        ${log-lib})

find_library(log-lib
        log)

target_link_libraries(
        server
        ${log-lib}
)

set_target_properties(server PROPERTIES PREFIX "lib")
set_target_properties(server PROPERTIES SUFFIX ".so")