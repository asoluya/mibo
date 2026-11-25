<div align="center">

<img src="https://raw.githubusercontent.com/asoluya/mibo/main/frontend/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" height="96" alt="Mibo" />

# Mibo

**第三方微博客户端** · Kotlin + Jetpack Compose · UI 基于 Miuix（HyperOS 设计语言）

[![Release](https://img.shields.io/github/v/release/asoluya/mibo?label=release&color=1f6feb)](https://github.com/asoluya/mibo/releases/latest)
![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-1.13-4285F4?logo=jetpackcompose&logoColor=white)
![Miuix](https://img.shields.io/badge/UI-Miuix%20%C2%B7%20HyperOS-111111)

</div>

---

## 特性

- **信息流**：推荐（`containerid=102803`）、关注（`feed/friends`）、好友圈（`feed/circle`），`since_id` / `max_id` 分页，下拉刷新
- **微博卡片**：正文富文本（@ / 话题 / 链接 / 表情）、九宫格图片、视频、转发块、来源与时间
- **详情页**：正文、评论（热度 / 时间双排序）、点赞、评论、转发
- **发布**：文字 + 多图上传、@ / 话题 / 表情工具条、草稿箱、编辑历史
- **图片**：查看器、原图加载、长按玻璃菜单保存（菜单从按下点展开、收回按下点）
- **主页与关系**：个人主页、用户微博、关注 / 粉丝
- **搜索**：热搜、关键词搜索、话题页
- **消息**：私信列表、通知
- **本地数据**：浏览历史、草稿、多账号、设置（深色模式 / 字号）
- **登录**：全屏官方网页登录，自动抓取并保存 Cookie
- **外部链接直达**（Mibo 新增）：京东 / 淘宝 / 天猫 / 唯品会链接点击直接跳对应 App；微博小店带货链接自动解包跳转商品页，不再停留在内置网页

## 技术栈

| 层 | 选型 |
| :--- | :--- |
| 语言 / UI | Kotlin 2.4 · Jetpack Compose 1.13 |
| 设计系统 | [Miuix](https://github.com/compose-miuix-ui/miuix) 0.9.5-beta04 |
| 构建 | AGP 9.5.2 · Gradle 9.9.2 · JDK 17 |
| 网络 | OkHttp 4.12 |
| 图片 | Coil 3.6 |
| 视频 | Media3 ExoPlayer 1.11 |
| 数据层 | `m.weibo.cn` Web API（Cookie 复用） |

## 工程信息

| 项 | 值 |
| :--- | :--- |
| applicationId | `com.mibo.app` |
| 应用名 | Mibo |
| 版本 | 0.0.1（versionCode 1） |
| minSdk / targetSdk | 26 / 37 |
| 工程 | `frontend/`（独立 Gradle 工程） |

## 目录

```
mibo/
├── frontend/                     # Android 应用（com.mibo.app）
│   ├── app/                      # 应用模块
│   │   └── src/main/kotlin/com/mibo/app/
│   │       ├── data/             # API、模型、缓存、设置
│   │       ├── screen/           # 页面
│   │       └── ui/               # 导航、主题、组件
│   ├── scripts/e2e.sh            # 真机端到端测试
│   └── gradlew                   # Gradle 9.9.2
└── README.md
```

## 构建

```bash
cd frontend
./gradlew :app:assembleDebug       # 产物：app/build/outputs/apk/debug/app-debug.apk
```

需要 JDK 17 和安装了 API 37、Build Tools 37.0.0 的 Android SDK。在 `frontend/local.properties` 设置 `sdk.dir`。Miuix 依赖 `top.yukonga.miuix.kmp:*:0.9.5-beta04` 直接从 Maven Central 拉取，常规构建无需额外配置。

## 下载

正式版 APK 在 [GitHub Releases](https://github.com/asoluya/mibo/releases/latest) 发布，应用内更新检查也指向本仓库，可直接覆盖安装升级。

## 安装

```bash
adb install -r --no-incremental app/build/outputs/apk/debug/app-debug.apk
```

## 测试

```bash
cd frontend
./scripts/e2e.sh                   # 真机端到端测试（需已登录）
```

## 文档

| 文档 | 内容 |
| :--- | :--- |
| [`01-微博API.md`](docs/01-微博API.md) | Host、公共参数、接口目录、上传、返回结构与错误码 |
| [`02-技术方案.md`](docs/02-技术方案.md) | 技术栈、模块拆分、数据模型、里程碑、关键实现 |
| [`03-功能清单.md`](docs/03-功能清单.md) | 已实现功能与对应接口、已知边界 |
| [`04-前端架构.md`](docs/04-前端架构.md) | COUI 主题、导航、页面清单与实测记录 |
| [`05-端到端测试.md`](docs/05-端到端测试.md) | E2E 套件、覆盖范围与运行方式 |

## 说明

第三方客户端，与微博官方无关，仅个人使用。
