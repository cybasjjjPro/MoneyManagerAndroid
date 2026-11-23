# Money Manager Android (v1 — Native Kotlin + Jetpack Compose + SQLite)

A 100% native Android double-entry expense & budget tracker inspired by Realbyte's Money Manager, built with **Kotlin**, **Jetpack Compose (Material 3)**, and local **SQLite**—with a built-in **GitHub Actions** workflow that compiles your `.apk` in the cloud for free (no Android Studio needed).

## Included in v1
- **Trans. Tab:** Daily grouped ledger, Calendar grid with daily income/expense totals, Monthly 12-month summary, Carry-Over balance, and Search/#Tag filter.
- **Add/Edit Sheet:** Income, Expense, and Transfer (with optional Transfer Fee auto-expense), **Smart Quick-Parse** (`18.50 coffee tng #work`), **Built-in Calculator**, and **Save / Continue** batch entry.
- **Stats & Budgets:** Native Compose Canvas Donut chart, category percentage breakdown, and monthly Budgets with a daily pace marker.
- **Accounts Tab:** Real-time double-entry Assets, Liabilities, Net Worth, Credit Card cut-off/payment dates, and one-tap **Pay** settlement transfers.
- **More Tab:** Dark Mode, 6 Realbyte accent themes, Currency switcher (`RM`, `$`, `S$`, `€`, `¥`), Carry-Over toggle, and native Android **Share CSV** export.

## How to Build Your APK in 3 Minutes ($0, No Android Studio)
1. **Unzip** `MoneyManagerAndroid_v1.zip` on your computer.
2. **Create a new repository** on [GitHub](https://github.com/new) (choose **Public** for unlimited free build minutes, or **Private** to use your free monthly allowance).
3. **Upload all files** from inside the `MoneyManagerAndroid/` folder (make sure `.github/workflows/build-apk.yml` is included!).
   - Tip: Using Git in terminal or GitHub Desktop is easiest so hidden folders like `.github/` are included automatically:
     ```bash
     cd MoneyManagerAndroid
     git init
     git add .
     git commit -m "Initial v1 native Money Manager app"
     git branch -M main
     git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPO>.git
     git push -u origin main
     ```
4. **Download your APK:** Open your repo on GitHub -> **Actions** tab -> click the completed **Build Android APK** run -> download **MoneyManager-v1-debug-apk** under **Artifacts**.
5. **Install on your phone:** Unzip the artifact to get `app-debug.apk`, transfer or download it on your Android phone, tap it, allow **Install unknown apps**, and open **Money Manager**!
