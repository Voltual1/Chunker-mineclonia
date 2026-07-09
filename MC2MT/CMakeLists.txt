cmake_minimum_required(VERSION 3.11) 

project(MC2MT)

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED TRUE)

message(STATUS "*** Building MC2MT Android Shared Library ***")

include(FetchContent)
FetchContent_Declare(
  sqlite3
  URL      https://sqlite.org/2026/sqlite-amalgamation-3530300.zip
  URL_HASH SHA256=646421e12aac110282ef8cc68f1a62d4bb15fc7b8f09da0b53e29ee690500431
)

FetchContent_GetProperties(sqlite3)
if(NOT sqlite3_POPULATED)
  FetchContent_Populate(sqlite3)
  add_library(sqlite3_static STATIC "${sqlite3_SOURCE_DIR}/sqlite3.c")
  target_include_directories(sqlite3_static PUBLIC "${sqlite3_SOURCE_DIR}")
  target_compile_definitions(sqlite3_static PRIVATE SQLITE_THREADSAFE=1)
endif()

find_package(Threads REQUIRED)
find_package(ZLIB REQUIRED)

include_directories(
	${PROJECT_SOURCE_DIR}/src
	${PROJECT_SOURCE_DIR}/src/nbt
	${ZLIB_INCLUDE_DIRS})

set(SRCS
	src/jni_api.cpp
	src/convert.cpp
	src/threads.cpp
	src/MCMap.cpp
	src/MTMap.cpp
	src/nbt/nbt.cpp
	src/nbt/serialization.cpp
	src/nbt/compression.cpp)

add_library(${PROJECT_NAME} SHARED ${SRCS})

target_link_libraries(${PROJECT_NAME}
	sqlite3_static
	Threads::Threads
	${ZLIB_LIBRARIES}
	log) 