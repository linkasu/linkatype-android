
# LINKa. напиши

Программа, переводящая напечатанный текст в речь и сохраняющая частые фразы. Подходит для людей, которые легко печатают на клавиатуре или экране планшета, но имеют проблемы с речью.

## CI/CD

Проект использует GitHub Actions для автоматизации:

- Запуск unit-тестов при каждом push и PR
- Проверка lint для контроля качества кода
- Сборка APK
- Кеширование Gradle зависимостей для ускорения CI
- Автоматические отчеты о тестах в PR

## Тестирование

Запуск unit-тестов локально:

```bash
./gradlew test
```

Запуск lint проверок:

```bash
./gradlew lintDebug
```

## Kotlin Multiplatform

- Общий код вынесен в модуль `shared` (KMP) и используется Android-приложением.
- Для интеграции на iOS собирайте XCFramework: `./gradlew :shared:assembleSharedDebugXCFramework`.
- Готовый артефакт появится в `shared/build/XCFrameworks/debug/Shared.xcframework` и может быть подключен в Xcode напрямую или через Swift Package Manager.
- Для релизной сборки используйте `./gradlew :shared:assembleSharedReleaseXCFramework`.

Покрытие тестами:
- Структуры данных: Category, Statement
- Менеджеры данных: CategoryManager, StatementManager
- Утилиты: TtsCacheManager, TtsHolder, HashMapAdapter, Callback
- UI компоненты: Component, BankGroup, InputGroup
- Краевые случаи и граничные условия
