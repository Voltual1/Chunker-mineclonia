cmake_minimum_required(VERSION 3.11) # FetchContent 需要 3.11+

project(MC2MT)

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED TRUE)

message(STATUS "*** Building MC2MT Android Shared Library ***")

# 1. 自动下载并集成 SQLite3
include(FetchContent)
FetchContent_Declare(
  sqlite3
  URL      https://sqlite.org/2026/sqlite-amalgamation-3530300.zip
  URL_HASH SHA256=646421e12aac110282ef8cc68f1a62d4bb15fc7b8f09da0b53e29ee690500431
)

FetchContent_GetProperties(sqlite3)
if(NOT sqlite3_POPULATED)
  FetchContent_Populate(sqlite3)
  # 创建一个静态库目标
  add_library(sqlite3_static STATIC "${sqlite3_SOURCE_DIR}/sqlite3.c")
  target_include_directories(sqlite3_static PUBLIC "${sqlite3_SOURCE_DIR}")
  # SQLite3 在 Android 上需要线程支持
  target_compile_definitions(sqlite3_static PRIVATE SQLITE_THREADSAFE=1)
endif()

# 2. 查找其他依赖
find_package(Threads REQUIRED)
find_package(ZLIB REQUIRED)

# 3. 设置包含目录
include_directories(
	${PROJECT_SOURCE_DIR}/src
	${PROJECT_SOURCE_DIR}/src/nbt
	${ZLIB_INCLUDE_DIRS})

# 4. 定义源码
set(SRCS
	src/jni_api.cpp
	src/convert.cpp
	src/threads.cpp
	src/MCMap.cpp
	src/MTMap.cpp
	src/nbt/nbt.cpp
	src/nbt/serialization.cpp
	src/nbt/compression.cpp)

# 5. 生成共享库
add_library(${PROJECT_NAME} SHARED ${SRCS})

# 6. 链接库
# 链接我们刚刚定义的 sqlite3_static，以及系统库
target_link_libraries(${PROJECT_NAME}
	sqlite3_static
	Threads::Threads
	${ZLIB_LIBRARIES}
	log) # Android Log 库，用于调试