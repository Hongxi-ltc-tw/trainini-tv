# 🚂 翻牌時刻表 Google TV 版 (Trainini TV & Screensaver)

專為 **Google TV / Android TV** 打造的復古台鐵翻牌時刻表應用程式與螢幕保護程式（預設載入 [`https://trainini.app/?tra=7380`](https://trainini.app/?tra=7380) 四腳亭站，可隨時透過遙控器切換全臺各車站或自訂網址）。

---

## ✨ 功能特色

1. **📺 支援 Google TV 電視首頁啟動 (`LEANBACK_LAUNCHER`)**
   - 內建電視專用 16:9 橫幅圖示 (`tv_banner`)，安裝後直接顯示於 Google TV 首頁應用程式列。
   - 自動開啟 **螢幕恆亮 (`FLAG_KEEP_SCREEN_ON`)**，作為客廳/工作室即時發車看板時不會自動休眠。
2. **🌙 內建 Android TV 螢幕保護程式 (`DreamService`)**
   - 支援設為系統「螢幕保護程式 / 待機畫面」，當電視閒置時自動跳出復古翻牌時刻表。
   - 支援獨立設定「螢幕保護程式是否靜音」、「夜間亮度調暗遮罩 (`0% ~ 50%`)」。
3. **🔊 自動解鎖翻牌音效**
   - 解除電視 WebView 的手勢音效限制 (`mediaPlaybackRequiresUserGesture = false`) 並自動喚醒 `AudioContext`，無須滑鼠點擊即可聽到經典「啪搭啪搭」翻牌聲（亦可一鍵靜音）。
4. **🎮 專為電視遙控器 (D-Pad) 設計的操控體驗**
   - 按遙控器 **【OK / 確認鍵】** 或 **【選單鍵】** 即可叫出底部電視控制列：
     - **📍 切換車站**：內建四腳亭 (`7380`)、瑞芳 (`7360`)、八堵 (`0920`)、基隆 (`0900`)、臺北 (`1000`)、板橋 (`1020`)、新竹、臺中、臺南、高雄、宜蘭、花蓮、臺東等快速選單，亦支援輸入任意自訂 `trainini.app` 參數網址。
     - **🖱️ 遙控游標模式**：用遙控器方向鍵移動畫面上的琥珀色準星、按 OK 鍵點擊網頁（可直接操作 `trainini.app` 右上角的齒輪設定，修改後的網址會自動記憶）。
     - **🔍 畫面縮放 (`75% ~ 200%`)**：自由調整看板在 4K / 1080p 電視上的顯示比例。
     - **🛡️ OLED 防烙印微移保護**：每分鐘自動以圓周微移 `3px`，保護 OLED/QLED 電視面板長時間顯示不留殘影。

---

## 🎮 電視遙控器操作說明

| 遙控器按鍵 | 一般看板模式 | 遙控游標模式 |
| :--- | :--- | :--- |
| **OK / 確認鍵 / 選單鍵** | 開啟底部「電視控制列」 | 在游標位置點擊網頁 |
| **方向鍵 (上/下/左/右)** | 開啟並選擇控制列按鈕 | 移動畫面上的虛擬滑鼠游標 |
| **返回鍵 (Back)** | 關閉控制列 / 退出 App | 關閉虛擬滑鼠游標模式 |

---

## 📦 如何取得 / 編譯 APK

### 方法 A：使用 GitHub Actions 自動雲端編譯（免安裝開發環境）
1. 將本專案推送至您的 GitHub 儲存庫。
2. 點擊 GitHub 頁面上方的 **Actions** 頁籤 -> **Build Google TV APK**。
3. 執行完成後即可下載編譯好的 `TraininiTV-GoogleTV-APK`（內含 `app-release.apk` 與 `app-debug.apk`）。

### 方法 B：本機使用 Android Studio 或 Gradle 編譯
1. 使用 **Android Studio** 開啟此資料夾，點選選單 **Build > Build Bundle(s) / APK(s) > Build APK(s)**。
2. 或在終端機確認已設定 `ANDROID_HOME` 與 JDK 17 後執行：
   ```bash
   gradle assembleRelease
   ```
   編譯完成的 APK 位於 `app/build/outputs/apk/release/app-release.apk`。

---

## 📺 如何安裝至 Google TV 並設為螢幕保護程式

### 1. 安裝 APK 到 Google TV
- **使用 Send Files to TV**：在手機與 Google TV 安裝 `Send Files to TV`，將 APK 傳送到電視後用檔案管理員安裝。
- **使用 ADB 無線安裝**：
  1. 在 Google TV 開啟「設定 > 系統 > 關於 > Android TV 作業系統版本（連按 7 下開啟開發人員選項）」，開啟「USB 偵錯 / 網路偵錯」。
  2. 在電腦執行：
     ```bash
     adb connect <您的電視IP>:5555
     adb install -r app-release.apk
     ```

### 2. 設定為 Google TV 螢幕保護程式 (Screensaver)
- **方式一（透過 App 內建按鈕）**：
  開啟「翻牌時刻表 TV」App -> 按 OK 鍵叫出控制列 -> 點選 **「⚙️ 屏保與設定」** -> 點擊 **「📺 開啟 Google TV 系統螢幕保護程式設定選單」** -> 選擇 **「台鐵翻牌時刻表 (Trainini)」**。
- **方式二（透過 ADB 指令強制指定，適用於隱藏屏保選單的 Chromecast with Google TV）**：
  Google TV 原廠介面預設使用「環境模式 (Ambient Mode)」並隱藏了第三方屏保選單，您只需執行以下一行 ADB 指令即可將本程式設為預設螢幕保護程式：
  ```bash
  adb shell settings put secure screensaver_components app.trainini.tv/.TraininiDreamService
  ```
  如需立即測試螢幕保護程式效果，可執行：
  ```bash
  adb shell cmd dreams start-dreaming
  ```
