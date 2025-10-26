# LINKa Type - Kotlin Multiplatform Project

Этот проект теперь поддерживает кроссплатформенную разработку с помощью Kotlin Multiplatform для Android, iOS, macOS и Windows.

## Структура проекта

- `shared/` - Общий модуль с бизнес-логикой и UI компонентами
- `app/` - Android приложение
- `iosApp/` - iOS приложение
- `desktopApp/` - Desktop приложение (macOS, Windows, Linux)

## Требования

- JDK 21
- Android SDK
- Xcode (для iOS)
- Gradle 8.8.2+

## Сборка

### Android
```bash
./gradlew :app:assembleDebug
```

### iOS
```bash
./gradlew :iosApp:linkDebugFrameworkIosX64
./gradlew :iosApp:linkDebugFrameworkIosArm64
```

### Desktop (macOS, Windows, Linux)
```bash
./gradlew :desktopApp:run
```

### Сборка всех платформ
```bash
./gradlew build
```

## Запуск

### Android
```bash
./gradlew :app:installDebug
```

### Desktop
```bash
./gradlew :desktopApp:run
```

### iOS
Откройте Xcode проект (создается автоматически) и запустите из Xcode.

## Особенности

1. **Общий код**: Вся бизнес-логика, модели данных и UI компоненты находятся в shared модуле
2. **Платформо-специфичный код**: Реализации для каждой платформы находятся в соответствующих source sets
3. **Compose Multiplatform**: Единый UI для всех платформ
4. **Firebase**: Интеграция с Firebase для Android (требует настройки для других платформ)

## Настройка Firebase для других платформ

Для iOS и Desktop приложений необходимо настроить Firebase отдельно:
- iOS: Добавить GoogleService-Info.plist
- Desktop: Настроить веб-версию Firebase

## Разработка

1. Изменения в shared модуле автоматически применяются ко всем платформам
2. Платформо-специфичный код изолирован в соответствующих source sets
3. Используйте expect/actual для создания платформо-специфичных реализаций