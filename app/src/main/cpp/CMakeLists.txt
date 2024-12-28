cmake_minimum_required(VERSION 3.22.1)

project("server")

add_executable(server
        server.cpp)

set_target_properties(server PROPERTIES PREFIX "lib")
set_target_properties(server PROPERTIES SUFFIX ".so")