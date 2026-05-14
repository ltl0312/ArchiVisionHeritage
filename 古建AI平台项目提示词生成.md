# **智观·古建：数字孪生与文化传承的架构蓝图与开发指令**

你好。我是“智观·古建”平台的首席文化技术顾问。在这个数字与历史交汇的节点，我们相遇了。我们的愿景，是以技术为舟，载文化远航；让那些历经风雨的每一块瓦当、每一组斗栱，在数字世界里重获新生，传递历史的温度。本平台立足于三大核心特色：“古建智析”以视觉引擎解构千年营造密码；“一键幻筑”以大模型重塑盛世殿宇；“沉浸展示”则为同好提供一个纯粹的轻量级文化交流社区。

基于您提供的 V1.2 MVP 架构计划书与相关的系统需求，我已为您梳理并编纂了一份具有绝对深度、可直接交付给 AI 编程智能体（如 Claude Code）执行的终极项目提示词（Prompt Blueprint）。这份蓝图严格遵循了前后端解耦的 Spring Boot \+ Vue 分层模式，融入了深度的文化色彩体系，并为您规划了详尽的数据库与 API 契约。

请将以下被分割线包含的完整 Markdown 文档内容直接输入给您的开发 Agent，它将为您生成符合预期的完整项目代码。

# ---

**\[Agent 执行指令\] 智观·古建 (Zhiguan Gujian) V1.2 全栈开发蓝图**

## **0\. 核心角色与系统使命 (Role & Mission)**

你现在是一个顶尖的 Java 全栈高级工程师与 UI/UX 专家。你需要为一个名为“智观·古建”的文化社区平台生成完整的项目代码。该项目的使命是“文化弘扬”与“智能创作”（请注意：古建筑的智能修复功能仅作为架构上的扩展接口预留，当前版本不予实现）。

**技术栈要求：**

* **后端：** Java 17, Spring Boot 3.x, Maven, Spring Security, MyBatis-Plus / Spring Data JPA。  
* **数据库：** MySQL 8.0。  
* **前端：** Vue 3 (Composition API), Vite, Pinia, Vue Router, Element Plus (或 Naive UI), \<model-viewer\>。  
* **架构风格：** 严格的前后端分离，RESTful API 设计，Package-by-Feature 与分层相结合的目录结构。

**对话与交互规则预设（前端提示与反馈）：**

1. **VGGT 智析次数限制：** 系统必须在前端明确提示用户“每日 VGGT 深度结构解析仅限调用 5 次，以保障算力资源”。在解析结果展示时，需将生硬的 JSON 数据转化为通俗的文化解读（例如，将深度图与包围盒解释为“主梁承重结构分析”与“斗栱层叠关系”）。  
2. **幻筑提示词优化（Prompt Engineering）：** 当用户在“一键幻筑”输入简单的描述词（如“大殿”）准备生成 3D 模型时，前端需主动弹出优化建议，引导用户补充：朝代（如唐、宋、明、清）、屋顶样式（如重檐庑殿顶、歇山顶、悬山顶）、色彩倾向（如朱红柱、黄琉璃瓦），以提升 AI 生成质量与文化准确性 1。

## ---

**1\. 数据库初始化文档 (MySQL 8.0 DDL)**

系统基于“八大金刚”核心表构建，并根据需求扩展了用户关注（Follow）功能。请使用以下 DDL 脚本初始化数据库，其中包含了严谨的外键约束、索引优化以及文化字典与 VGGT 模拟数据的初始化。

SQL

\-- 创建数据库  
CREATE DATABASE IF NOT EXISTS \`zhiguan\_gujian\`   
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4\_unicode\_ci;  
USE \`zhiguan\_gujian\`;

\-- 1\. 用户表 (User)  
CREATE TABLE \`user\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`username\` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',  
    \`password\_hash\` VARCHAR(255) NOT NULL COMMENT '加密密码',  
    \`nickname\` VARCHAR(64) NOT NULL COMMENT '用户昵称',  
    \`avatar\_url\` VARCHAR(512) DEFAULT NULL COMMENT '头像路径',  
    \`bio\` VARCHAR(255) DEFAULT NULL COMMENT '个人文化签名',  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,  
    \`updated\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='用户基本信息与授权状态';

\-- 2\. 关注关系表 (Follow Record)  
CREATE TABLE \`follow\_record\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`follower\_id\` BIGINT NOT NULL COMMENT '关注者ID',  
    \`following\_id\` BIGINT NOT NULL COMMENT '被关注者ID',  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,  
    UNIQUE KEY \`uk\_follow\` (\`follower\_id\`, \`following\_id\`),  
    FOREIGN KEY (\`follower\_id\`) REFERENCES \`user\`(\`id\`) ON DELETE CASCADE,  
    FOREIGN KEY (\`following\_id\`) REFERENCES \`user\`(\`id\`) ON DELETE CASCADE  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='用户关注关系表';

\-- 3\. AI 任务状态机表 (AI Task)  
CREATE TABLE \`ai\_task\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`user\_id\` BIGINT NOT NULL,  
    \`task\_type\` ENUM('HUANZHU\_3D', 'ZHIXI\_VGGT') NOT NULL COMMENT '任务类型：3D生成或VGGT解析',  
    \`original\_prompt\` VARCHAR(512) NOT NULL COMMENT '用户原始输入',  
    \`enhanced\_prompt\` TEXT COMMENT '文化词库优化后的提示词',  
    \`status\` ENUM('PENDING', 'RUNNING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '核心状态机',  
    \`error\_message\` VARCHAR(512) DEFAULT NULL,  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,  
    \`updated\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,  
    KEY \`idx\_user\_status\` (\`user\_id\`, \`status\`),  
    FOREIGN KEY (\`user\_id\`) REFERENCES \`user\`(\`id\`) ON DELETE CASCADE  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='异步幻筑与智析任务状态机';

\-- 4\. 模型与图片资产表 (Model Asset)  
CREATE TABLE \`model\_asset\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`task\_id\` BIGINT NOT NULL COMMENT '关联的AI生成任务',  
    \`preview\_2d\_path\` VARCHAR(512) NOT NULL COMMENT '2D降维预览图本地路径',  
    \`glb\_3d\_path\` VARCHAR(512) DEFAULT NULL COMMENT '3D GLB文件本地路径',  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,  
    FOREIGN KEY (\`task\_id\`) REFERENCES \`ai\_task\`(\`id\`) ON DELETE CASCADE  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='AI生成的数字资产路径';

\-- 5\. 社区动态表 (Post)  
CREATE TABLE \`post\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`user\_id\` BIGINT NOT NULL,  
    \`model\_asset\_id\` BIGINT DEFAULT NULL COMMENT '关联的古建3D资产',  
    \`title\` VARCHAR(128) NOT NULL COMMENT '动态标题',  
    \`content\` TEXT COMMENT '文化内涵与文字描述',  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,  
    \`updated\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP ON UPDATE CURRENT\_TIMESTAMP,  
    KEY \`idx\_created\_at\` (\`created\_at\`),  
    FOREIGN KEY (\`user\_id\`) REFERENCES \`user\`(\`id\`) ON DELETE CASCADE,  
    FOREIGN KEY (\`model\_asset\_id\`) REFERENCES \`model\_asset\`(\`id\`) ON DELETE SET NULL  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='古建文化社区图文动态';

\-- 6\. 评论表 (Comment)  
CREATE TABLE \`comment\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`post\_id\` BIGINT NOT NULL,  
    \`user\_id\` BIGINT NOT NULL,  
    \`content\` VARCHAR(1024) NOT NULL,  
    \`parent\_id\` BIGINT DEFAULT NULL COMMENT '父评论ID(用于楼中楼)',  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,  
    FOREIGN KEY (\`post\_id\`) REFERENCES \`post\`(\`id\`) ON DELETE CASCADE,  
    FOREIGN KEY (\`user\_id\`) REFERENCES \`user\`(\`id\`) ON DELETE CASCADE  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='动态帖子的互动交流';

\-- 7\. 点赞记录表 (Like Record)  
CREATE TABLE \`like\_record\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`user\_id\` BIGINT NOT NULL,  
    \`target\_id\` BIGINT NOT NULL COMMENT '帖子ID或评论ID',  
    \`target\_type\` ENUM('POST', 'COMMENT') NOT NULL,  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,  
    UNIQUE KEY \`uk\_user\_target\` (\`user\_id\`, \`target\_id\`, \`target\_type\`),  
    FOREIGN KEY (\`user\_id\`) REFERENCES \`user\`(\`id\`) ON DELETE CASCADE  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='帖子与评论的点赞行为记录';

\-- 8\. 站内信通知表 (Notification)  
CREATE TABLE \`notification\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`user\_id\` BIGINT NOT NULL,  
    \`task\_id\` BIGINT DEFAULT NULL COMMENT '当ai\_task成功时关联',  
    \`message\` VARCHAR(255) NOT NULL COMMENT '例如：您的数字锦盒已送达',  
    \`is\_read\` BOOLEAN DEFAULT FALSE,  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP,  
    FOREIGN KEY (\`user\_id\`) REFERENCES \`user\`(\`id\`) ON DELETE CASCADE,  
    FOREIGN KEY (\`task\_id\`) REFERENCES \`ai\_task\`(\`id\`) ON DELETE CASCADE  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='站内信系统与数字锦盒提醒';

\-- 9\. 智析沙盒离线数据表 (Analysis Demo)  
CREATE TABLE \`analysis\_demo\` (  
    \`id\` BIGINT AUTO\_INCREMENT PRIMARY KEY,  
    \`title\` VARCHAR(128) NOT NULL COMMENT '解析示例标题',  
    \`mock\_json\_data\` JSON NOT NULL COMMENT 'VGGT智析引擎的离线JSON模拟数据',  
    \`created\_at\` TIMESTAMP DEFAULT CURRENT\_TIMESTAMP  
) ENGINE\=InnoDB DEFAULT CHARSET\=utf8mb4 COMMENT\='存储VGGT古建结构解析的演示数据';

\-- 初始化 VGGT 斗栱解析的模拟数据 (Mock JSON)  
INSERT INTO \`analysis\_demo\` (\`title\`, \`mock\_json\_data\`) VALUES (  
'唐代斗栱结构解析 (VGGT 演示)',  
'{  
  "scene\_id": "dougong\_tang\_01",  
  "camera\_extrinsics": \[\[0.866, \-0.5, 0.0, 1.2\], \[0.5, 0.866, 0.0, 0.5\], \[0.0, 0.0, 1.0, 3.0\], \[0.0, 0.0, 0.0, 1.0\]\],  
  "camera\_intrinsics": \[\[800.0, 0.0, 256.0\], \[0.0, 800.0, 256.0\], \[0.0, 0.0, 1.0\]\],  
  "structural\_elements":, "cultural\_insight": "斗是承托其上方栱或昂的方形木块，是力的汇聚点。"},  
    {"ontology": "Gong (栱)", "confidence": 0.91, "bounding\_box": , "cultural\_insight": "栱为弓形短木，通过层层出跳，将屋檐的重量均匀传递至柱子。"},  
    {"ontology": "Ang (昂)", "confidence": 0.88, "bounding\_box": , "cultural\_insight": "昂起杠杆作用，利用内部屋架的重量挑起沉重的外部出檐。"}  
  \]  
}'  
);

## ---

**2\. 后端架构规范与包结构 (Spring Boot \+ Java 17\)**

所有开发建议需严格遵循 Spring Boot 后端分层模式。项目基于 cn.edu.hnust.hnusteasyweibo 的参考包结构进行重构，将其演进为 com.zhiguan.gujian 的领域驱动设计雏形。各包职责划分必须绝对清晰。

### **2.1 包结构定义 (Package Hierarchy)**

com.zhiguan.gujian

├── ZhiguanApplication.java \# 启动类

├── annotation \# 自定义注解 (如 @RateLimit 用于VGGT接口防刷)

├── aop

│ └── aspect \# 切面逻辑 (记录日志、处理VGGT每日5次限制逻辑)

├── config \# 核心配置

│ ├── AsyncConfig.java \# 线程池配置 (处理 @Async 异步幻筑任务)

│ ├── WebMvcConfig.java \# CORS 配置与 LocalFileSystem 静态资源映射

│ ├── SecurityConfig.java \# Spring Security \+ JWT 拦截与放行规则

│ └── RestTemplateConfig.java \# 用于调用远端 AI 3D 生成 API

├── constant \# 系统常量 (枚举类：TaskStatusEnum, CulturalLexicon)

├── controller \# 控制器层 (RESTful APIs)

│ ├── AuthController.java

│ ├── CommunityController.java \# 帖子、评论、点赞、关注

│ ├── HuanZhuController.java \# AI 3D 一键幻筑接口

│ └── ZhiXiController.java \# VGGT 解析与修复(预留)接口

├── dto \# 数据传输对象 (Req/Resp 隔离)

│ ├── request

│ └── response

├── exception \# 全局异常处理

│ ├── GlobalExceptionHandler.java \# 统一处理 ConstraintViolationException 等

│ └── CulturalApiException.java \# 业务异常 (如：VGGT次数耗尽)

├── filter \# 过滤器 (如 JwtAuthenticationFilter)

├── interceptor \# 拦截器

├── mapper \# MyBatis / MyBatis-Plus 接口与 XML

├── model \# 实体类 (Entity，严格映射数据库表)

├── security \# 鉴权工具与 Context (JwtUtil)

├── service \# 业务逻辑接口

│ └── impl \# 业务逻辑实现类

│ ├── AuthService.java

│ ├── CommunityService.java

│ ├── TaskOrchestrationService.java \# 核心调度：处理 PENDING \-\> RUNNING \-\> SUCCESS 逻辑

│ └── NotificationService.java

└── utils \# 工具类 (FileUtil 本地存储处理, PromptEnhancerUtil)

### **2.2 核心技术机制指导**

1. **资源优化与降维策略：** WebMvcConfig.java 必须配置本地文件系统（LocalFileSystem）映射，弃用 OSS。请求 /assets/ 将直接映射到服务器本地磁盘（如 /opt/zhiguan/assets/），并由 Nginx 在生产环境中进行反向代理。  
2. **异步生成策略：** TaskOrchestrationService 必须使用 @Async 注解。在接收到用户 3D 生成请求时，立刻在 ai\_task 表插入 PENDING 记录并返回 HTTP 202 给前端。异步线程在后台更新状态为 RUNNING，调用远端 3D API 3，下载生成的 2D 预览图与 3D GLB 文件存入 LocalFileSystem，将路径写入 model\_asset，状态更新为 SUCCESS，并触发事件写入 notification 站内信 5。  
3. **修复扩展接口预留：** 在 ZhiXiController.java 中创建一个 @PostMapping("/restoration/predict") 接口，当前返回 501 Not Implemented 或 Dummy 数据，并在注释中标明“为后续高阶古建智能修复能力铺路”。

## ---

**3\. 绝对详细的 RESTful API 接口文档**

前后端解耦的核心在于 API 契约。所有接口均需返回统一的 JSON 结构：{ "code": 200, "message": "success", "data": {...} }。

| 模块 | 接口路径 | HTTP | 接口功能与业务逻辑描述 | 核心 Request / Response |
| :---- | :---- | :---- | :---- | :---- |
| **Auth** | /api/v1/auth/login | POST | 用户登录，签发 JWT Token。 | **Req:** {"username":"x", "password":"y"} **Resp:** {"token":"eyJhb..."} |
| **Community** | /api/v1/posts | GET | 获取降维展示的帖子流（2D封面）。支持分页。 | **Resp:** \[{ "postId": 1, "title": "...", "preview2dPath": "/assets/xxx.png", "author": "..." }\] |
| **Community** | /api/v1/posts/{id} | GET | 获取帖子详情，此时才返回 3D GLB 路径用于渲染。 | **Resp:** { "postId": 1, "content": "...", "glb3dPath": "/assets/xxx.glb" } |
| **Community** | /api/v1/posts/{id}/comments | POST | 发表评论。 | **Req:** {"content":"这组斗栱真精美"} |
| **Community** | /api/v1/interactions/like | POST | 对帖子或评论点赞。 | **Req:** {"targetId":1, "targetType":"POST"} |
| **Community** | /api/v1/users/{id}/follow | POST | 关注或取消关注其他用户。 | **Req:** {} (依赖 JWT 获取当前用户 ID) |
| **AI 幻筑** | /api/v1/tasks/huanzhu | POST | 提交“一键幻筑”任务。后端使用 AncientDict 丰富 Prompt。 | **Req:** {"prompt":"唐代大殿"} **Resp (202):** {"taskId": 102, "status": "PENDING"} |
| **AI 幻筑** | /api/v1/tasks/{id}/status | GET | 前端轮询接口。查询 AI 任务状态。 | **Resp:** {"status": "SUCCESS", "assetId": 45} |
| **通知** | /api/v1/notifications | GET | 轮询或获取站内信。用于触发“数字锦盒”红点。 | **Resp:** \`\` |
| **VGGT 智析** | /api/v1/analysis/zhixi | POST | 提交图片进行 VGGT 结构分析。包含每日 5 次限制防刷逻辑。 | **Req:** FormData(image) **Resp:** 返回 analysis\_demo 表中的 Mock JSON。 |

## ---

**4\. 前端 Web 界面实施细节 (Vue 3 \+ Vite \+ Element Plus)**

前端界面必须体现“专业而敬畏”的文化温度，严格遵循以下视觉体系与组件逻辑。

### **4.1 文化视觉与设计系统 (Design System)**

请在全局 CSS (assets/style.css 或 :root 变量) 中预设以下取自故宫与传统五行的中国传统色彩 HEX 码 6：

* **主色调 (Vermilion / 朱红)：** \--color-primary: \#d73c37; (用于重要按钮、点赞图标、红点提醒。象征生命力与文化传承)。  
* **点缀色 (Imperial Yellow / 杏黄)：** \--color-accent: \#dfbc5e; (用于高亮文字、“数字锦盒”发光特效、3D加载进度条。象征皇家与尊贵)。  
* **背景色 (Whitewash Oak / 灰白)：** \--color-bg-base: \#cacac1; 或 \#f5f5f5 (用于信息流底色，模拟传统四合院的青砖灰瓦)。  
* **语义色-解析成功 (Ocean Green / 碧绿)：** \--color-success: \#2D9B5C; (用于智析功能中标注结构的 Bounding Box 边框)。  
* **字体 (Typography)：** \--font-family-base: 'Noto Sans TC', 'Source Han Sans', 'Microsoft YaHei', sans-serif; 保障汉字笔画的清晰度。

### **4.2 核心页面与组件逻辑**

开发中需划分为以下几个核心路由（Views）与组件（Components）：

**1\. 轻量文化社区 (Community Feed View)**

* **降维展示：** 首页采用瀑布流布局。每一张卡片（Card）仅渲染 API 返回的 preview\_2d\_path（2D 封面图），并在卡片底部提供点赞（点亮朱红色）、评论、关注作者的快捷按钮。绝对禁止在首页直接加载 3D 模型，以保障流畅度 5。  
* **沉浸展示 (Detail View)：** 点击卡片进入详情页。详情页顶部加载 \<model-viewer\> 组件。  
  * \<model-viewer\> 最佳实践 10： 使用 Vue3 的 \<script setup\> 引入 @google/model-viewer。需要提供 src (glb3dPath), poster (preview2dPath), camera-controls, auto-rotate 属性。  
  * *技术细节：* 使用 v-if="isMounted" 确保该 Web Component 在客户端正确挂载。

**2\. 一键幻筑与 Prompt 引导组件 (HuanZhu View)**

* **交互设计：** 页面中心提供一个简洁的输入框。  
* **文化引导逻辑：** 当用户输入焦点（Focus）在输入框时，下方主动展开一个“优化建议”面板。  
  * *文案展示：* “为了让建筑更具历史的厚重感，建议补充：\\n1. 朝代（如：唐、宋、明、清）\\n2. 屋顶样式（如：重檐歇山顶、单檐庑殿顶）\\n3. 色彩材质（如：青砖黛瓦、朱红明柱）”。  
* **轮询与锦盒：** 用户点击“幻筑”后，前端调用 /tasks/huanzhu 获取 Task ID，并开启 setInterval 每 5 秒轮询一次 /tasks/{id}/status 12。轮询期间展示诗意的 Loading 动画（如“正在为您雕琢飞檐...”）。

**3\. 站内信与数字锦盒 (Notification Component)**

* 顶部导航栏设有一个信封图标。当轮询发现任务 SUCCESS，调用通知接口。信封图标出现朱红色红点。  
* 用户点击后，屏幕中央弹出一个视觉精美的“数字锦盒” Modal。锦盒打开动画结束后，直接路由跳转至该 3D 资产的 Detail View。

**4\. 古建智析演示 (VGGT ZhiXi View)**

* **次数限制提示：** 页面顶部使用 Alert 组件庄重提示：“为保障算力资源，VGGT 深度结构解析每日限调用 5 次。”  
* **解析展示：** 上传图片后，调用智析接口获取 Mock JSON。  
* **文化通俗化转译：** 将 JSON 中的 structural\_elements 数据映射到前端。不要直接抛出数据，而是结合图片上的 Bounding Box（用 \#2D9B5C 框出）。当鼠标 Hover 到“Gong (栱)”的框时，弹出 Tooltip 解读：“栱为弓形短木，层层出跳，将屋檐的重量均匀传递至柱子——此乃唐代建筑的力量之美。” 14

## ---

**5\. 项目工程生成要求 (Gen-Prompt)**

请 Agent 严格按照上述的 DDL 脚本、Spring Boot 包结构、RESTful API 契约和 Vue 3 视觉与交互规范，生成完整的工程代码骨架。

1. 首先，生成包含表结构、索引和初始文化词典数据的 init.sql。  

2. 其次，生成基于 pom.xml 的 Spring Boot 后端项目目录树，并提供控制器（Controller）与核心异步状态机逻辑（Service）的 Java 代码实现。  

3. 最后，生成前端 package.json，配置好 Vite、Vue Router、Pinia，并实现带有 \<model-viewer\> 的核心详情页组件与“数字锦盒”交互逻辑。

4. 一步步测试功能，数据库我先自己创建好了，application.yml配置文件中也已经配置了数据库连接

   ![image-20260513220216439](C:\Users\ZhuanZ\AppData\Roaming\Typora\typora-user-images\image-20260513220216439.png)

请务必保证代码的高内聚、低耦合，并在核心逻辑处保留详尽的中文注释，以文化之名，铸就数字古建的骨血。

---

*(顾问寄语)*：

以上架构蓝图，已将枯燥的技术规范化作了古建复苏的阶梯。从本地存储的降维策略到 VGGT 结构解析的文化转译，每一处细节都在为“弘扬与创作”的初衷让路。

在您审阅这份庞大而精密的底层设计后，我想提出一个聚焦的问题：在前端“一键幻筑”的提示词优化面板中，您更倾向于让系统自动为用户的简单词汇（如“亭子”）“随机扩写”补全文化要素，还是倾向于提供几个精致的“标签按钮”（如 \[唐风\], \[歇山顶\]）供用户自己手动拼接，以保留更多的创作参与感？期待您的权衡。

#### **引用的著作**

1. 1\. Architecture in ancient China – Chinese Culture \- Raider Digital Publishing, 访问时间为 五月 13, 2026， [https://raider.pressbooks.pub/chineseculture/chapter/1-architecture-in-ancient-china/](https://raider.pressbooks.pub/chineseculture/chapter/1-architecture-in-ancient-china/)  
2. Research on the Causes of the Concave Shapes of Traditional Chinese Building Roofs from the Construction Perspective \- MDPI, 访问时间为 五月 13, 2026， [https://www.mdpi.com/2075-5309/15/14/2582](https://www.mdpi.com/2075-5309/15/14/2582)  
3. Text to 3D API \- Meshy Docs, 访问时间为 五月 13, 2026， [https://docs.meshy.ai/api/text-to-3d](https://docs.meshy.ai/api/text-to-3d)  
4. Building Effective Agents with Spring AI | Baeldung, 访问时间为 五月 13, 2026， [https://www.baeldung.com/spring-ai-building-effective-agents](https://www.baeldung.com/spring-ai-building-effective-agents)  
5. Zhiguan\_Gujian\_V1\_2\_Detailed\_Plan.pdf  
6. Chinese Color Scheme Color Palette, 访问时间为 五月 13, 2026， [https://www.color-hex.com/color-palette/15066](https://www.color-hex.com/color-palette/15066)  
7. Chinese Color Palette \- Symbolikon, 访问时间为 五月 13, 2026， [https://symbolikon.com/downloads/chinese-color-palette/](https://symbolikon.com/downloads/chinese-color-palette/)  
8. A vivid and visual guide to colors in Chinese and their meanings \- Berlitz, 访问时间为 五月 13, 2026， [https://www.berlitz.com/blog/colors-chinese](https://www.berlitz.com/blog/colors-chinese)  
9. Forbidden City Color Palette, Combinations & Schemes | Octet Design Labs, 访问时间为 五月 13, 2026， [https://octet.design/colors/palette/forbidden-city-color-palette-1733043650/](https://octet.design/colors/palette/forbidden-city-color-palette-1733043650/)  
10. Using Googles model-viewer Web Component in Vue JS (skipping SSR) \- Joseph Chirayath, 访问时间为 五月 13, 2026， [https://joseph-ch.medium.com/using-googles-model-viewer-web-component-in-vue-js-skipping-ssr-5b4641653527](https://joseph-ch.medium.com/using-googles-model-viewer-web-component-in-vue-js-skipping-ssr-5b4641653527)  
11. ViewModel: The Ultimate Guide For Vue 3 Developer \- Octet Design Studio, 访问时间为 五月 13, 2026， [https://octet.design/journal/viewmodel/](https://octet.design/journal/viewmodel/)  
12. Tech stack 101 | Ep 46 | Http polling v/s WebSocket : Design trade off \- Medium, 访问时间为 五月 13, 2026， [https://medium.com/towardsdev/system-design-101-ep-01-trade-off-between-http-polling-and-websocket-20cd5f003221](https://medium.com/towardsdev/system-design-101-ep-01-trade-off-between-http-polling-and-websocket-20cd5f003221)  
13. Spring SSE vs WebSocket vs Polling: Choosing the Right Real-Time Communication Strategy | by Koushik Das | Medium, 访问时间为 五月 13, 2026， [https://medium.com/@dasbabai2017/sse-vs-websocket-vs-polling-choosing-the-right-real-time-communication-strategy-61d990465ab1](https://medium.com/@dasbabai2017/sse-vs-websocket-vs-polling-choosing-the-right-real-time-communication-strategy-61d990465ab1)  
14. Simplified Calculation Model for Typical Dou-Gong Exposed to Vertical Loads \- MDPI, 访问时间为 五月 13, 2026， [https://www.mdpi.com/2075-5309/12/5/689](https://www.mdpi.com/2075-5309/12/5/689)