# Vue3 + Spring Boot + MySQL 全栈脚手架

## 目录结构
```
scaffold/
├── backend/                  # Spring Boot 项目
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/demo/
│       │   ├── DemoApplication.java
│       │   ├── config/
│       │   │   ├── CorsConfig.java   # 跨域配置
│       │   │   └── Result.java       # 统一响应格式
│       │   ├── controller/UserController.java
│       │   ├── entity/User.java
│       │   ├── mapper/UserMapper.java
│       │   └── service/
│       │       ├── UserService.java
│       │       └── UserServiceImpl.java
│       └── resources/
│           ├── application.yml       # 数据库配置
│           └── init.sql              # 建表 SQL
└── frontend/                 # Vue3 项目
    ├── vite.config.js        # 含 /api 代理
    ├── src/
    │   ├── main.js
    │   ├── App.vue
    │   ├── api/
    │   │   ├── request.js    # axios 封装（含拦截器）
    │   │   └── user.js       # user 接口模块
    │   ├── router/index.js
    │   └── views/
    │       ├── HomeView.vue  # Hello World 测试页
    │       └── UserView.vue  # CRUD 用户管理页
```

## 快速启动

### 1. 数据库
```sql
-- 在 MySQL 中执行
source backend/src/main/resources/init.sql
```

### 2. 后端
```bash
cd backend
# 修改 application.yml 中的数据库密码
mvn spring-boot:run
# 访问: http://localhost:8080/api/user/hello
```

### 3. 前端
```bash
cd frontend
npm install
npm run dev
# 访问: http://localhost:5173
```

## 技术栈
| 层     | 技术                          |
|--------|-------------------------------|
| 前端   | Vue 3 + Vite + Element Plus   |
| 状态   | Pinia                         |
| HTTP   | Axios（含请求/响应拦截器）    |
| 后端   | Spring Boot 3 + Maven         |
| ORM    | MyBatis-Plus                  |
| 数据库 | MySQL 8                       |

## 统一响应格式
```json
{ "code": 200, "msg": "success", "data": { ... } }
```

## 跨域方案
开发时使用 **Vite proxy**（`/api` → `localhost:8080`），  
生产部署时建议用 **Nginx 反向代理**，无需后端 CorsConfig。
