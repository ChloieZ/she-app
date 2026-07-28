pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 高德地图 SDK（阿里云镜像）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "HerSpace"
include(":app")
