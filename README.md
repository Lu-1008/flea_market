# 跳蚤市场系统（Flea Market）

## 项目简介

跳蚤市场系统是一个基于Spring Boot和Vue.js开发的二手物品交易平台，为用户提供便捷的二手物品发布、购买和交易服务。系统包含用户端和管理端两个部分，支持商品分类、购物车、订单管理、评价系统等功能。

## 系统功能

### 用户端功能
- 用户注册与登录
- 商品浏览与搜索
- 商品详情查看
- 发布二手商品
- 购物车管理
- 订单创建与管理
- 商品评价
- 个人中心与信息管理

### 管理端功能
- 管理员登录与权限控制
- 用户管理
- 商品管理
- 分类管理
- 订单管理
- 评价管理
- 数据统计与分析

## 技术栈

### 后端技术
- 核心框架：Spring Boot 3.5.0
- 数据访问：MyBatis
- 数据库：MySQL 8.0
- 文件存储：MinIO
- 身份验证：JWT (JSON Web Token)
- API文档：Swagger/OpenAPI

### 前端技术
- 框架：Vue.js 3
- UI组件库：Element Plus
- 路由：Vue Router
- 状态管理：Pinia
- HTTP客户端：Axios
- 构建工具：Vite

## 系统架构

系统采用前后端分离的架构设计：
- 后端提供RESTful API接口
- 前端分为用户端(flea-market-user)和管理端(flea-market-management)
- 文件存储使用MinIO对象存储服务
- 数据持久化使用MySQL数据库

## 数据库设计

系统包含以下主要数据表：
- users：用户信息表
- categories：商品分类表
- items：商品信息表
- carts：购物车表
- cart_items：购物车商品关联表
- orders：订单表
- order_items：订单商品关联表
- reviews：商品评价表

## 安装部署

### 环境要求
- JDK 21+
- Node.js 18+
- MySQL 8.0+
- MinIO服务器

### 后端部署步骤
1. 克隆代码仓库
```bash
git clone [仓库地址]
```

2. 配置数据库
```bash
# 导入数据库脚本
mysql -u username -p < sql/market_db.sql
```

3. 修改配置文件
```bash
# 编辑application.yml配置数据库连接和MinIO配置
```

4. 编译并运行后端
```bash
mvn clean package
java -jar target/flea-market-0.0.1-SNAPSHOT.jar
```

### 前端部署步骤

1. 安装管理端依赖并运行
```bash
cd flea-market-management
npm install
npm run dev
```

2. 安装用户端依赖并运行
```bash
cd flea-market-user
npm install
npm run dev
```

## 系统角色

系统包含三种用户角色：
- 管理员(ADMIN)：拥有系统所有功能的访问权限
- 分类管理员(CATEGORY_MANAGER)：可以管理商品和分类
- 普通用户(USER)：可以浏览、购买和发布商品

## 开发团队

[在此添加开发团队信息]

## 许可证

[在此添加许可证信息] 