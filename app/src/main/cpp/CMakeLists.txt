cmake_minimum_required(VERSION 3.18.1)

project(app CXX)

add_library(app SHARED opus_recorder.cpp opus_decoder.cpp)

find_package(opus REQUIRED CONFIG)
target_link_libraries(app opus::libopus.so)
target_link_libraries(app log)