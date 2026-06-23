# ☕ VoidRP Launcher (Java)

> Автономный Java-лаунчер VoidRP — единый fat JAR без внешних зависимостей.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-0090D3)
![OkHttp](https://img.shields.io/badge/OkHttp-4.x-brightgreen)
![Build](https://img.shields.io/badge/build-Gradle_Shadow-02303A?logo=gradle)
![License](https://img.shields.io/badge/license-proprietary-red)

---

## 🗺️ Место в экосистеме

```
  Игрок запускает JAR
        │
┌───────────────────────────────┐
│  voidrp-launcher-java (JAR)   │
│  JavaFX UI · OkHttp           │
└───────────┬───────────────────┘
            │ play-ticket auth (HTTPS)
            ▼
  minecraft-backend → Minecraft Server
```

Альтернатива [voidrp-launcher-vue](https://github.com/VOIDRP-MINECRAFT/voidrp-launcher-vue) для случаев, когда Electron недоступен или нежелателен.

---

## ✨ Возможности

- **Единый fat JAR** (~56 MB) — не требует установки, достаточно Java 21
- **Авторизация** через аккаунт VoidRP (play-ticket flow)
- **Автообновление модпака** — скачивает, проверяет SHA-256 контрольные суммы
- **Bootstrap JVM** — запускает Minecraft с нужными параметрами
- **JavaFX UI** — нативный интерфейс без браузера
- **Верификация манифеста** — каждый файл модпака проверяется по хешу перед запуском

---

## 📋 Требования

| Компонент | Версия |
|---|---|
| Java | 21+ |
| Интернет | доступ к `https://void-rp.ru` |

---

## 🚀 Сборка и запуск

```bash
cd voidrp_launcher_java

# Сборка fat JAR
./gradlew shadowJar
# → build/libs/voidrp-launcher-*.jar  (~56 MB)

# Запуск
java -jar build/libs/voidrp-launcher-*.jar
```

---

## 🏗️ Структура

```
src/main/java/ru/voidrp/launcher/
├── App.java                  # точка входа JavaFX Application
├── auth/                     # авторизация, play-ticket, токены
├── manifest/                 # парсинг манифеста, проверка SHA-256
├── download/                 # скачивание файлов модпака
├── launch/                   # bootstrap JVM для Minecraft
└── ui/
    └── views/                # экраны JavaFX (Login, Main, Settings, Mods)
```

---

## 🔗 Связанные репозитории

| Репо | Связь |
|---|---|
| [minecraft-backend](https://github.com/VOIDRP-MINECRAFT/minecraft-backend) | Auth API, play-ticket endpoint |
| [voidrp-launcher-vue](https://github.com/VOIDRP-MINECRAFT/voidrp-launcher-vue) | Основной лаунчер (Electron + .NET) |

---

<div align="center">
<a href="https://void-rp.ru">🌐 Сайт</a> ·
<a href="https://github.com/VOIDRP-MINECRAFT">🏠 Организация</a>
</div>
