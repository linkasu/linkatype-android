# TODO — LINKa Type: backend migration + dialog (iOS/Android, shared-first)

## 0. Контракт и окружение
- [x] Зафиксировать prod base URL: https://backend.linka.su
- [x] Зафиксировать auth flow: /v1/auth, /v1/auth/register, /v1/auth/reset, /v1/auth/refresh, /v1/auth/logout
- [x] Подтверждены форматы/лимиты: web (OGG Opus + WAV), Android (AAC/MP4 16k), лимит 8MB
- [x] Описаны требования к оффлайну и конфликтам (LWW по updated_at)

## 1. Shared Core (KMP)
- [x] Скелет модулей: api, sync, db
- [x] Общие модели (Category, Statement, UserState, Preferences, Dialog*)
- [x] Интерфейсы репозиториев (Auth/Categories/Statements/UserState/Dialog)
- [x] Ktor клиент + auth/refresh интерцептор
- [x] Secure storage expect/actual (Keystore/Keychain)
- [x] SQLDelight схема + миграции (стартовая)
- [x] Offline queue: типы операций + сериализация
- [x] Sync: flush при online + периодический (на старте + периодически в активном состоянии)
- [x] Realtime: long-poll /v1/changes (минимум)

## 2. Auth
- [x] AuthRepository: login/register/reset/refresh/logout
- [x] Сессия: access token in-memory, refresh token в secure storage
- [x] Авто refresh при 401 с одним retry

## 3. Банк фраз (Categories/Statements)
- [x] Repositories: CRUD + сортировка
- [x] Offline-first: локальный кеш, очередь операций (без явного резолва конфликтов)
- [x] Поддержка глобальных категорий (import)

## 4. UserState / Quickes / Preferences
- [x] get/put /v1/user/state
- [x] Нормализация quickes (6 слотов)
- [x] Синхронизация preferences через state

## 5. Dialog (текст + аудио)
- [x] DialogRepository: chats/messages/suggestions/apply/dismiss
- [x] Отправка текста и аудио multipart
- [x] Подсказки (pending, apply, dismiss)

## 6. Android интеграция
- [x] Заменить Firebase Auth/RTDB на shared
- [x] Подключить shared repos в UI/ViewModel
- [x] Экран диалога (чат-лист, сообщения, аудио)
- [x] Оставить Firebase Analytics

## 7. iOS интеграция
- [x] Удалить Firebase менеджеры
- [x] SwiftUI экраны на shared state
- [x] Диалог + запись аудио

## 8. Тесты
- [x] shared/commonTest: MockEngine + SQLDelight in-memory
- [x] Android unit (обновлены под shared)
- [x] Android instrumentation
- [x] iOS XCTest

## 9. Роллаут
- [x] Cohort/feature flag read source
- [x] Метрики синка/ошибок
