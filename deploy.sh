#!/bin/bash

# ==========================================
# 配置参数
# ==========================================
APP_NAME="spring-ai-rag"  # 容器名称
APP_PORT="8081"              # 宿主机映射端口
BRANCH="main"                # Git 分支名称
IMAGE_NAME="spring-ai-rag-app"      # 镜像名称

# 获取当前脚本所在目录（即项目根目录）
PROJECT_PATH=$(cd $(dirname $0); pwd)
cd $PROJECT_PATH

echo "========================================"
echo ">>> 1. 同步远程代码 (Branch: $BRANCH)"
echo "========================================"
# 强制重置本地修改并拉取最新代码（生产环境建议）
git fetch --all
git reset --hard origin/$BRANCH
git pull origin $BRANCH

echo "========================================"
echo ">>> 2. 使用 Docker 多阶段构建镜像 (Java 21)"
echo "========================================"
# 构建镜像，标记为 latest 和 时间戳版本
TIME_TAG=$(date +%Y%m%d%H%M%S)
docker build -t $IMAGE_NAME:$TIME_TAG .
docker tag $IMAGE_NAME:$TIME_TAG $IMAGE_NAME:latest

echo "========================================"
echo ">>> 3. 停止并移除旧容器"
echo "========================================"
if [ $(docker ps -aq --filter name=$APP_NAME) ]; then
    echo "停止容器: $APP_NAME"
    docker stop $APP_NAME
    docker rm $APP_NAME
fi

echo "========================================"
echo ">>> 4. 启动新容器 (内存硬限制 256MB)"
echo "========================================"
# --memory="256m": 限制物理内存
# --memory-swap="512m": 允许使用 256M 物理内存 + 256M Swap
docker run -d \
  --name $APP_NAME \
  -p $APP_PORT:8081 \
  --memory="256m" \
  --memory-swap="512m" \
  --restart always \
  -e "SPRING_THREADS_VIRTUAL_ENABLED=true" \
  $IMAGE_NAME:latest

echo "========================================"
echo ">>> 5. 清理冗余镜像 (虚悬镜像)"
echo "========================================"
docker image prune -f

echo ">>> 部署完成！"
echo ">>> 容器运行状态："
docker ps --filter name=$APP_NAME
echo ">>> 内存占用实时统计："
docker stats $APP_NAME --no-stream