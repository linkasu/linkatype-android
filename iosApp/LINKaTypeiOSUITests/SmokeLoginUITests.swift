import XCTest

final class SmokeLoginUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testLoginFlow() {
        let email = readEnvValue("SMOKE_EMAIL")
        let password = readEnvValue("SMOKE_PASSWORD")
        guard let email, !email.isEmpty else {
            XCTFail("Missing SMOKE_EMAIL")
            return
        }
        guard let password, !password.isEmpty else {
            XCTFail("Missing SMOKE_PASSWORD")
            return
        }

        let app = XCUIApplication()
        app.launchArguments.append("ui_test")
        app.launch()

        let emailField = app.textFields["auth_email"]
        if emailField.waitForExistence(timeout: 5) {
            emailField.tap()
            emailField.typeText(email)

            let passwordField = app.secureTextFields["auth_password"]
            XCTAssertTrue(passwordField.waitForExistence(timeout: 5))
            passwordField.tap()
            passwordField.typeText(password)

            let loginButton = app.buttons["auth_primary"]
            XCTAssertTrue(loginButton.waitForExistence(timeout: 5))
            if !loginButton.isHittable {
                app.swipeUp()
            }
            loginButton.tap()
        }

        let inputField = findInputField(in: app)
        XCTAssertTrue(inputField.waitForExistence(timeout: 30))
        inputField.tap()
        inputField.typeText("Test")
        dismissKeyboardIfNeeded(in: app)

        let spotlightButton = app.buttons["spotlight_button"]
        if spotlightButton.waitForExistence(timeout: 5) {
            spotlightButton.tap()
            let spotlightView = element(in: app, withId: "spotlight_view")
            if spotlightView.waitForExistence(timeout: 5) {
                tapElement(spotlightView)
            } else {
                app.tap()
            }
        }

        if app.buttons["ui_test_settings"].waitForExistence(timeout: 2) {
            tapElement(app.buttons["ui_test_settings"])
        } else {
            openMainMenu(in: app)
            tapMenuItem("menu_settings", in: app)
        }
        let settingsView = element(in: app, withId: "settings_view")
        if settingsView.waitForExistence(timeout: 10) {
            tapElement(app.navigationBars.buttons.element(boundBy: 0))
        }

        if app.buttons["ui_test_dialog"].waitForExistence(timeout: 2) {
            tapElement(app.buttons["ui_test_dialog"])
        } else {
            openMainMenu(in: app)
            tapMenuItem("menu_dialog", in: app)
        }
        let dialogView = element(in: app, withId: "dialog_view")
        if dialogView.waitForExistence(timeout: 10) {
            app.swipeDown()
        }

        let importButton = app.buttons["bank_import_button"]
        if importButton.waitForExistence(timeout: 10) {
            tapElement(importButton)
            let importView = element(in: app, withId: "global_import_view")
            XCTAssertTrue(importView.waitForExistence(timeout: 10))
            tapElement(app.navigationBars.buttons.element(boundBy: 0))
        }
    }

    private func findInputField(in app: XCUIApplication) -> XCUIElement {
        let textField = app.textFields["input_text"]
        if textField.exists {
            return textField
        }
        return app.textViews["input_text"]
    }

    private func dismissKeyboardIfNeeded(in app: XCUIApplication) {
        let keyboard = app.keyboards.element
        guard keyboard.exists else { return }
        if app.keyboards.buttons["return"].exists {
            app.keyboards.buttons["return"].tap()
            return
        }
        if app.keyboards.buttons["Done"].exists {
            app.keyboards.buttons["Done"].tap()
        }
    }

    private func element(in app: XCUIApplication, withId identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }

    private func openMainMenu(in app: XCUIApplication, file: StaticString = #filePath, line: UInt = #line) {
        let menuButton = app.buttons["main_menu"]
        XCTAssertTrue(menuButton.waitForExistence(timeout: 5), file: file, line: line)
        tapElement(menuButton, file: file, line: line)
    }

    private func tapMenuItem(_ identifier: String, in app: XCUIApplication, file: StaticString = #filePath, line: UInt = #line) {
        let menuItem = app.menuItems[identifier]
        if menuItem.waitForExistence(timeout: 2) {
            tapElement(menuItem, file: file, line: line)
            return
        }
        let anyElement = element(in: app, withId: identifier)
        if anyElement.waitForExistence(timeout: 2) {
            tapElement(anyElement, file: file, line: line)
            return
        }
        let button = app.buttons[identifier]
        if button.waitForExistence(timeout: 2) {
            tapElement(button, file: file, line: line)
            return
        }
        if let localizedTitle = localizedMenuTitle(for: identifier) {
            let localizedMenuItem = app.menuItems[localizedTitle]
            if localizedMenuItem.waitForExistence(timeout: 2) {
                tapElement(localizedMenuItem, file: file, line: line)
                return
            }
            let localizedButton = app.buttons[localizedTitle]
            if localizedButton.waitForExistence(timeout: 2) {
                tapElement(localizedButton, file: file, line: line)
                return
            }
        }
        XCTFail("Missing menu item \(identifier)", file: file, line: line)
    }

    private func tapElement(_ element: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        guard element.exists else {
            XCTFail("Missing element to tap", file: file, line: line)
            return
        }
        if element.isHittable {
            element.tap()
            return
        }
        element.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
    }

    private func readEnvValue(_ key: String) -> String? {
        let env = ProcessInfo.processInfo.environment
        if let value = env[key], !value.isEmpty {
            return value
        }
        let fileValues = readEnvFile()
        return fileValues[key]
    }

    private func readEnvFile() -> [String: String] {
        let filePath = #filePath
        let fileUrl = URL(fileURLWithPath: filePath)
        let rootUrl = fileUrl
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let envUrl = rootUrl.appendingPathComponent(".env")
        guard let contents = try? String(contentsOf: envUrl, encoding: .utf8) else {
            return [:]
        }
        var values: [String: String] = [:]
        for line in contents.split(separator: "\n") {
            let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.isEmpty || trimmed.hasPrefix("#") { continue }
            guard let separatorIndex = trimmed.firstIndex(of: "=") else { continue }
            let key = String(trimmed[..<separatorIndex]).trimmingCharacters(in: .whitespacesAndNewlines)
            var value = String(trimmed[trimmed.index(after: separatorIndex)...])
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if value.hasPrefix("\""), value.hasSuffix("\""), value.count >= 2 {
                value = String(value.dropFirst().dropLast())
            }
            values[key] = value
        }
        return values
    }

    private func localizedMenuTitle(for identifier: String) -> String? {
        switch identifier {
        case "menu_settings":
            return localizedString(forKey: "settings")
        case "menu_dialog":
            return localizedString(forKey: "dialog_title")
        case "menu_clear":
            return localizedString(forKey: "clear")
        case "menu_logout":
            return localizedString(forKey: "logout")
        default:
            return nil
        }
    }

    private func localizedString(forKey key: String) -> String? {
        let filePath = #filePath
        let fileUrl = URL(fileURLWithPath: filePath)
        let rootUrl = fileUrl
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let resourcesUrl = rootUrl.appendingPathComponent("iosApp/LINKaTypeiOS/Resources")
        let locales = ["ru", "en"]
        for locale in locales {
            let stringsUrl = resourcesUrl.appendingPathComponent("\(locale).lproj/Localizable.strings")
            guard let contents = try? String(contentsOf: stringsUrl, encoding: .utf8) else {
                continue
            }
            if let value = parseStringsValue(key: key, contents: contents) {
                return value
            }
        }
        return nil
    }

    private func parseStringsValue(key: String, contents: String) -> String? {
        for line in contents.split(separator: "\n") {
            let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.isEmpty || trimmed.hasPrefix("//") || trimmed.hasPrefix("/*") {
                continue
            }
            guard trimmed.hasPrefix("\""), let separatorIndex = trimmed.firstIndex(of: "=") else {
                continue
            }
            let keyPart = trimmed[..<separatorIndex]
            let valuePart = trimmed[trimmed.index(after: separatorIndex)...]
            let keyString = keyPart
                .replacingOccurrences(of: "\"", with: "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if keyString != key {
                continue
            }
            let rawValue = valuePart
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .trimmingCharacters(in: CharacterSet(charactersIn: ";"))
                .trimmingCharacters(in: CharacterSet(charactersIn: "\""))
            return rawValue.replacingOccurrences(of: "\\\"", with: "\"")
        }
        return nil
    }
}
