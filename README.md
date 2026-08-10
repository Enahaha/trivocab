# 雅思英·中·韩三语背词软件

这是一个面向雅思备考的三语背词 MVP：以英文单词为核心，同时展示中文和韩文释义，并根据学习结果安排下一次复习。项目内置两本词书（雅思 3000 词 + 范洪滔 GitHub 雅思词汇表 3611 词），开发环境不需要预先安装数据库。

## 已实现功能

- 内置两本词书：雅思 3000 词、范洪滔 GitHub 雅思词汇表 3611 词，均包含英文、音标、词性、中文释义和韩文释义
- 词书选择：每个用户可切换要学习的词书，切换时立即按该词书重新估算完成天数
- 每用户每词书独立保存学习进度与每日学习目标，切换词书互不影响
- 我的页面：账号信息、已背时长、已学单词、连续/累积签到
- 每日签到日历：已签到日期标记，自动统计连续签到与累积签到
- 学习统计详情：按“最近一周 / 本月”查看每日学习、复习单词与学习时长柱状图
- 仪表盘：总词数、待复习、学习中、已掌握和每日目标
- 词书分页与关键词搜索
- 学习队列：先返回到期词，再补充新词
- `AGAIN`、`HARD`、`GOOD`、`EASY` 四档学习反馈
- 简化间隔重复算法、复习日志、幂等请求编号和乐观锁
- 每日学习计划：可选择每天 10 至 100 个单词，并估算完成词书所需天数与日期
- BCrypt 密码、Session 登录、CSRF 防护和 6 位验证码找回密码
- `USER` / `ADMIN` 角色隔离、登录审计和用户留言表
- 原生 HTML/CSS/JavaScript 学习页面，无需单独启动前端工程

## 技术栈

- Java 21
- Spring Boot 4.1.0
- MyBatis Spring Boot Starter 4.0.0（XML Mapper）
- H2 文件数据库（默认开发环境，MySQL 兼容模式）
- MySQL 8.4（`mysql` profile）
- Maven
- HTML、CSS、原生 JavaScript

## 目录结构

```text
.
├── pom.xml
├── compose.yaml
├── scripts/
│   └── generate-seed.py          # 从词库 JSON 生成开发种子 SQL
└── src/
    ├── main/java/com/trivocab/ielts/
    │   ├── controller/           # REST API
    │   ├── service/              # 学习与复习规则
    │   ├── mapper/               # MyBatis Mapper 接口
    │   ├── domain/               # 数据库行模型与枚举
    │   ├── dto/                  # API 请求/响应
    │   └── exception/            # 统一异常处理
    └── main/resources/
        ├── application.yml          # H2 默认配置
        ├── application-mysql.yml    # MySQL profile
        ├── db/schema.sql            # H2/MySQL 通用表结构
        ├── db/data-h2.sql           # demo 用户与雅思 3000 词
        ├── mappers/                 # MyBatis XML
        └── static/                  # 浏览器页面
```

## 使用 H2 一键启动

需要 Java 21 以及 Maven 3.6.3 或更高版本。在项目根目录执行：

```bash
mvn spring-boot:run
```

启动后访问：

- 登录页面：<http://localhost:8081/login.html>
- 学习页面：<http://localhost:8081/>
- 管理后台：<http://localhost:8081/admin.html>
- 健康检查：<http://localhost:8081/actuator/health>

默认使用 `8081`，避免和本机已有的 `8080` 服务冲突。如需指定其他端口，可在启动前设置 `SERVER_PORT`。

默认使用项目 `data/` 目录中的 H2 文件数据库。首次启动会自动建表并载入 demo 用户和 3000 个单词；旧版数据库会以 JDBC metadata 幂等补齐认证表和字段，不会清除学习进度。

本地初始账号为 `Enahaha / 123456`（管理员）和 `demo / 123456`（普通用户）。密码在数据库中仅保存 BCrypt 哈希；正式部署前必须通过环境变量替换初始管理员密码。

## 使用 MySQL 8.4

`compose.yaml` 会启动 MySQL，并在首次创建数据卷时自动执行表结构和 3000 词种子 SQL：

```bash
docker compose up -d
docker compose ps
```

启动应用：

```bash
SPRING_PROFILES_ACTIVE=mysql \
DB_URL='jdbc:mysql://localhost:3306/trivocab?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false' \
DB_USERNAME=trivocab \
DB_PASSWORD=trivocab \
mvn spring-boot:run
```

MySQL profile 支持以下环境变量：

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/trivocab?...` | JDBC 连接地址 |
| `DB_USERNAME` | `trivocab` | 数据库用户 |
| `DB_PASSWORD` | `trivocab` | 数据库密码 |
| `MYSQL_PORT` | `3306` | compose 映射到本机的端口 |
| `BOOTSTRAP_ADMIN_USERNAME` | `Enahaha` | 初始管理员用户名 |
| `BOOTSTRAP_ADMIN_EMAIL` | `enahaha@local.trivocab` | 初始管理员邮箱 |
| `BOOTSTRAP_ADMIN_PASSWORD` | `123456` | 仅用于首次生成 BCrypt 哈希 |

Compose 中的账号密码仅供本地开发使用，部署前必须替换。空数据卷由 MySQL 容器执行初始 SQL；应用启动时只会进行幂等补列、补表和初始管理员检查。

如果修改了初始 SQL，已有数据卷不会再次执行它们。请优先在新数据卷上验证，不要直接删除存有学习进度的数据卷。

## 部署到手机 / iPad

应用自带 PWA 清单与图标（`manifest.webmanifest`、`icons/`、`apple-touch-icon.png`），
手机或 iPad 浏览器可以把网站“添加到主屏幕”，像原生 App 一样独立窗口使用。

### 方式一：局域网部署（最快，适合家里/公司同一 Wi-Fi）

1. 打包并启动（需要 Java 21+）：

   ```bash
   mvn -o package -DskipTests
   java -jar target/ielts-vocabulary-0.1.0-SNAPSHOT.jar
   ```

2. 查看电脑的局域网 IP（macOS）：

   ```bash
   ipconfig getifaddr en0
   ```

3. 手机 / iPad 连同一个 Wi-Fi，用 Safari 打开 `http://<电脑IP>:8081`，
   点“分享 → 添加到主屏幕”即可生成带图标的 TriVocab 应用。

注意事项：

- 本机防火墙需要放行 `8081` 端口；电脑睡眠或关机时无法访问。
- 局域网 HTTP 下 iOS 的“添加到主屏幕”可用，但离线缓存（Service Worker）需要 HTTPS。
- 管理后台地址：`http://<电脑IP>:8081/admin.html`。

### 方式二：Docker 常驻部署（推荐长期使用，适合云服务器 / 家中常开设备）

项目提供生产镜像与编排文件：

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

默认监听 `8080`，H2 数据文件保存在 Docker 卷 `trivocab-data`（容器内 `/data`）。
常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `APP_PORT` | `8080` | 映射到宿主机的端口 |
| `BOOTSTRAP_ADMIN_PASSWORD` | `123456` | 初始管理员密码（部署前必须改为强密码） |
| `DEMO_PASSWORD` | `123456` | demo 演示账号密码（建议改成随机值） |
| `SESSION_COOKIE_SECURE` | `false` | 开启 HTTPS 后设为 `true` |
| `EXPOSE_RESET_CODE` | `false` | 生产环境保持 `false`，忘记密码响应不返回验证码 |

访问地址：`http://<服务器IP>:8080`（学习页）、`http://<服务器IP>:8080/admin.html`（管理后台）。

### Windows 安装包

Windows 版由 GitHub Actions 在 Windows 构建机生成（macOS 无法直接交叉打包 Windows 程序）：

```bash
# 手动触发一次构建
gh workflow run build-windows.yml --repo Enahaha/trivocab --ref main
```

构建完成后在 Actions 页面下载 `trivocab-windows` 工件，包含：

- `TrVocab-1.1.0.exe`：安装器（需要 Java 21+ 或由 CI 内置运行时，推荐便携版）
- `TrVocab-1.1.0-windows.zip`：便携版，解压后运行 `TrVocab\TrVocab.exe` 即可

Windows 版使用同样的 8090 端口、自动打开浏览器；数据保存在用户目录 `~/TrVocab/trivocab.mv.db`。

### 方式三：云服务器直接跑 jar

把 `target/ielts-vocabulary-0.1.0-SNAPSHOT.jar` 上传到服务器，用 `systemd` 或
`nohup` 常驻运行即可（建议配合 Nginx / Caddy 反代 HTTPS）：

```bash
SPRING_PROFILES_ACTIVE=prod \
SERVER_PORT=8080 \
BOOTSTRAP_ADMIN_PASSWORD='你的强密码' \
DEMO_PASSWORD='随机值' \
java -jar ielts-vocabulary-0.1.0-SNAPSHOT.jar
```

### HTTPS 建议

公网部署强烈建议启用 HTTPS（PWA 安装、iOS 离线缓存和 Cookie 安全都需要）。
可以用 Caddy 自动申请证书，反代到应用端口：

```text
your-domain.com {
    reverse_proxy 127.0.0.1:8080
}
```

同时把 `SESSION_COOKIE_SECURE` 设为 `true`。

### 数据备份

- H2 模式：数据库就是一个文件。默认位置 `data/trivocab.mv.db`（开发模式），
  Docker 部署在卷 `trivocab-data` 内。备份 = 复制该文件（建议先停止容器），
  恢复 = 放回原路径后启动。
- MySQL 模式：按 MySQL 常规方式备份（`mysqldump` 或物理备份）。
- 建议配合 cron / rsync 每日备份，文件很小（几 MB）。

### 首次登录安全清单

1. 用环境变量设置强密码的 `BOOTSTRAP_ADMIN_PASSWORD` 再启动；
2. 登录管理后台后立即修改管理员密码；
3. 把 `DEMO_PASSWORD` 设成随机值，避免 demo 账号被他人登录；
4. 公网部署必须走 HTTPS，并把 `SESSION_COOKIE_SECURE=true`。

## 主要 API

所有正常响应使用统一包装结构，业务数据位于 `data` 字段。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | 注册并建立 Session |
| `POST` | `/api/v1/auth/login` | 用用户名或邮箱登录 |
| `GET` | `/api/v1/auth/me` | 获取当前账号与 CSRF Token |
| `POST` | `/api/v1/auth/logout` | 注销 Session |
| `POST` | `/api/v1/auth/forgot-password` | 生成 10 分钟有效的 6 位验证码 |
| `POST` | `/api/v1/auth/reset-password` | 使用验证码重置密码 |
| `GET` | `/api/v1/dashboard?bookId=1` | 学习仪表盘 |
| `GET` | `/api/v1/books` | 词书列表 |
| `GET` | `/api/v1/books/{bookId}/words?page=0&size=20&keyword=` | 分页/搜索单词 |
| `GET` | `/api/v1/profile/book-selection` | 当前用户词书选择与各词书完成天数估算 |
| `PUT` | `/api/v1/profile/book-selection` | 切换当前词书并返回新的完成天数估算 |
| `GET` | `/api/v1/profile/stats?range=week|month` | 每日学习/复习单词与学习时长统计及汇总 |
| `POST` | `/api/v1/profile/checkin` | 今日签到（幂等），返回连续/累积签到 |
| `GET` | `/api/v1/profile/checkins?year=&month=` | 查询某月签到日期与连续/累积签到 |
| `GET` | `/api/v1/words/{wordId}` | 单词详情 |
| `GET` | `/api/v1/study/queue?bookId=1&limit=20` | 获取待学队列 |
| `POST` | `/api/v1/study/reviews` | 提交一次学习反馈 |
| `PATCH` | `/api/v1/profile/daily-goal?bookId=1` | 修改每日计划并返回预计完成时间 |
| `GET` / `POST` | `/api/v1/messages` | 查看自己的留言或提交留言 |
| `GET` | `/api/v1/admin/dashboard` | 管理员今日活跃度与平台概览 |
| `GET` / `DELETE` | `/api/v1/admin/users` | 管理员查询或删除普通用户 |
| `GET` / `PATCH` / `DELETE` | `/api/v1/admin/messages` | 管理员处理留言 |
| `GET` / `POST` / `PUT` / `DELETE` | `/api/v1/admin/books` | 管理员维护词书（删除会级联清理其单词与学习记录） |
| `GET` / `POST` / `PUT` / `DELETE` | `/api/v1/admin/words` | 管理员维护词库 |

管理员词库接口中的 `id` 是数据库内部主键，用于 `/api/v1/admin/words/{wordId}` 这类编辑/删除路径；`wordId` 是 Excel 词书编号，对应数据库列 `words.word_id`。内置词书编号为 `IELTS-0001` 到 `IELTS-3000` 与 `FH-IELTS-0001` 到 `FH-IELTS-3611`，建议后续 Excel 使用 `word_id`、`word`、`phonetic`、`part_of_speech`、`chinese_meaning`、`korean_meaning`、`english_example`、`korean_example`、`priority_rank`、`book_name` 这些列。

切换词书：学习页“当前词书”面板提供词书下拉框，选择后调用 `PUT /api/v1/profile/book-selection`，后端保存该用户的 `selected_book_id`，并按所选词书的总词数、已学词数与每日目标重新估算剩余天数。每日目标通过 `PATCH /api/v1/profile/daily-goal?bookId=` 按“用户 + 词书”保存（`user_book_settings` 表），各词书互不影响；学习进度按“用户 + 单词”保存，单词从属于词书，因此每本词书进度天然独立。

管理员在管理后台“词书”页可新增、修改、删除词书。新增词书后可在“词库”页按词书筛选并逐个添加单词；删除词书会级联删除其单词、学习进度、复习日志与学习会话，操作前有确认提示。

词书种子数据：`scripts/generate-seed.py` 生成全量种子 `db/data-h2.sql`（两本词书，供全新数据库/MySQL 首次初始化）和增量种子 `db/data-book-fh-ielts.sql`（仅范洪滔词书，启动时由 `DatabaseBootstrap` 幂等补种到已有数据库，不清除既有进度）。

除注册、登录、忘记密码和重置密码外，`/api/v1/**` 都必须已登录。已登录的非 `GET` 请求还必须携带 `X-CSRF-Token`，其值来自登录、注册或 `me` 响应的 `data.csrfToken`。Session Cookie 启用 `HttpOnly` 和 `SameSite=Lax`。

登录请求可使用 `{"identifier":"Enahaha","password":"123456"}`，也兼容 `username` 字段。本地 H2 配置会在忘记密码响应的 `data.resetCode` 中显示验证码；MySQL profile 默认不暴露该值。

提交学习反馈的请求体示例如下。调用时还需携带登录 Session Cookie 和 `X-CSRF-Token`：

```json
{
  "clientReviewId": "example-review-001",
  "wordId": 1,
  "rating": "GOOD",
  "responseMs": 3200
}
```

`clientReviewId` 用于防止浏览器重试导致同一次复习被重复记录。

## 复习规则

- 进度状态与四档反馈的对应为：`NEW`（不认识 / `AGAIN`）、`LEARNING`（模糊 / `HARD`）、`REVIEWING`（记得 / `GOOD`）、`MASTERED`（熟练 / `EASY`）。新词默认为 `NEW`。
- `AGAIN`：本组延后 3 个卡片再出现；若中途退出，10 分钟后进入待复习队列，并重置长期遗忘曲线计数。
- `HARD`：本组延后 3 个卡片再出现；若中途退出，30 分钟后进入待复习队列，不推进长期间隔。
- `GOOD`：结束该词本组循环，按 `1、2、4、7、15、30、60、120、240、365` 天的遗忘曲线安排后续复习。
- `EASY`：立即标记为 `MASTERED`，结束本组循环，且永久不再进入待复习队列。
- `POST /study/reviews` 的响应会返回 `progressStatus`、`repeatInSession`、`repeatAfterCards`、`intervalDays` 和 `nextReviewAt`，前端可用它们重排组内卡片并生成组后复习计划。

今日学习量按不同单词计算；同一单词在组内因 `AGAIN` / `HARD` 多次出现，不会重复占用每日目标。

每日计划支持 `10、20、30……100` 个单词。预计天数按“剩余尚未首次学习的单词数 ÷ 每日目标”向上取整，预计完成日期以首尔当地日期为起点；该估算表示完成词书的首轮学习，不包含之后会随学习反馈变化的长期复习时间。保存后，仪表盘和学习队列都会使用当前用户的计划。

学习进度更新和复习日志写入处于同一数据库事务中，进度表通过 `version` 字段进行乐观并发控制。

## 当前边界

- 找回密码已生成和校验验证码，但正式环境仍需接入邮件或短信发送器；不应在生产环境开启 `app.auth.expose-reset-code`。
- 按照当前需求，没有实现单词或例句朗读。
- `english_example` 和 `korean_example` 数据库字段、API 字段和页面展示位置已预留，但当前来源词库不包含可直接使用的英文/韩文配对例句，因此这两个字段目前为空。
- H2 文件库适合本地单机学习；需要多用户或正式部署时请使用 MySQL。

## 下一步路线

1. 补齐并人工审校英文与韩文配对例句，保留来源和授权信息。
2. 使用 Flyway 管理 MySQL 数据库版本，并增加 Testcontainers MySQL 集成测试。
3. 增加多词书、导入工具、学习日历、错词本与长期统计。
4. 在核心学习流程稳定后，再评估移动端、PWA 和可选朗读功能。
