# --- 第一阶段：编译 ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 利用缓存：只下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 编译代码
COPY src ./src
RUN mvn clean package -DskipTests

# --- 第二阶段：运行 (Run) ---
# 使用轻量级 Alpine JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 复制 jar 包
COPY --from=build /app/target/*.jar app.jar

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# Java 21 针对低内存 (256MB) 的神级调优参数：
# 1. -Xmx160m: 堆内存上限。
# 2. -XX:+UseSerialGC: 小型应用首选，内存占用最低。
# 3. -XX:+ExitOnOutOfMemoryError: 发生 OOM 直接重启容器，不卡死。
# 4. -Djdk.virtualThreadScheduler.parallelism=1: 限制虚拟线程并行度，节约内存。
ENTRYPOINT ["java", \
            "-Xmx160m", \
            "-Xms160m", \
            "-XX:+UseSerialGC", \
            "-XX:MaxMetaspaceSize=64m", \
            "-Xss256k", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]