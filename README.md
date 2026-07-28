# 她空间 (Her Space) — Android MVP

## 产品概念
聚焦于女性对实体空间的友好性需求。用户可以通过地图查看空间的女性友好度，也可以投票分享自己的体验。

## MVP 核心流程
```
地图首页（色块展示友好度）→ 搜索酒店/商场/餐厅 → 点击地点查看详情（友好度色块+投票数）
→ 点击"立即投票" → GPS自动推荐附近店铺 → 选择店铺 → 点击三个按钮之一投票 → 返回地图（数据更新）
```

## 技术栈
- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3
- **地图**: OSMDroid (OpenStreetMap, 免费无需API Key)
- **数据库**: Room (本地存储)
- **定位**: Google Play Services Location
- **架构**: MVVM

## 友好度色块规则
| 友好票占比 | 色块 | 含义 |
|-----------|------|------|
| ≥ 80%     | 🟢 绿色 | 最友好 |
| 50%-79%   | 🟡 黄色 | 一般 |
| < 50%     | 🔴 红色 | 不友好 |

## 项目结构
```
she-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/herspace/app/
│   │   │   ├── MainActivity.kt        # 入口
│   │   │   ├── HerSpaceApp.kt         # Application
│   │   │   ├── data/
│   │   │   │   ├── db/                # Room数据库
│   │   │   │   │   ├── AppDatabase.kt  # 数据库
│   │   │   │   │   ├── VoteDao.kt      # DAO
│   │   │   │   │   ├── VoteEntity.kt   # 投票实体
│   │   │   │   │   └── PlaceSummary.kt # 地点汇总
│   │   │   │   ├── model/             # 数据模型
│   │   │   │   │   └── FriendlinessLevel.kt
│   │   │   │   └── repository/        # 数据仓库
│   │   │   │       └── PlaceRepository.kt
│   │   │   ├── ui/
│   │   │   │   ├── map/               # 地图首页
│   │   │   │   │   ├── MapScreen.kt
│   │   │   │   │   └── MapViewModel.kt
│   │   │   │   ├── vote/              # 投票流程
│   │   │   │   │   ├── VoteScreen.kt
│   │   │   │   │   └── VoteViewModel.kt
│   │   │   │   ├── detail/            # 地点详情卡片
│   │   │   │   │   └── PlaceDetailCard.kt
│   │   │   │   ├── theme/             # 主题配色
│   │   │   │   │   └── Theme.kt
│   │   │   │   └── navigation/        # 导航
│   │   │   │       └── NavGraph.kt
│   │   │   └── util/
│   │   │       └── LocationHelper.kt  # 定位工具
│   │   └── res/
│   └── build.gradle.kts
└── build.gradle.kts
```

## 构建与运行

### 前置条件
1. Android Studio Hedgehog (2023.1.1) 或更新版本
2. Android SDK 34
3. JDK 17

### 步骤
1. 用 Android Studio 打开 `D:\Software\she\she-app` 目录
2. 等待 Gradle 同步完成
3. 在 `local.properties` 中配置 SDK 路径（Android Studio 会自动生成）
4. 连接 Android 设备或启动模拟器
5. 点击 Run ▶

### 注意事项
- MVP 阶段使用模拟店铺数据（详见 `LocationHelper.kt`）
- 地图瓦片来自 OpenStreetMap，需要联网加载
- 投票数据存储在本地 Room 数据库，卸载应用会丢失
- 定位权限首次使用时会弹出申请

## 后续迭代方向
- [ ] 接入真实 POI 数据源
- [ ] 用户登录/账户系统
- [ ] 我的投票记录
- [ ] 地点图片上传
- [ ] 评论功能
- [ ] 后端服务 + 云端数据同步
- [ ] 搜索自动补全
- [ ] 路由规划（推荐友好路线）
