# 🚴 PacePilot - Android Cycling Pacer & COT Navigator

**PacePilot** adalah aplikasi Android berbasis **Kotlin & Jetpack Compose** khusus untuk pesepeda yang ingin menaklukkan target waktu tempuh (**COT / Cut-Off Time**). Aplikasi ini secara cerdas memantau ritme kayuhan, memberikan arahan pacing langsung (tambah/kurangi kecepatan), serta mengingatkan jadwal hidrasi/fueling dan istirahat agar tenaga pesepeda terjaga optimal.

Proyek ini telah dikonfigurasi untuk **di-build secara otomatis menjadi APK melalui GitHub Actions**.

---

## 🌟 Fitur Utama

1. **Rute Khusus Sepeda (Bicycle Routing)**:
   - Menggunakan peta **OpenStreetMap (osmdroid)** yang 100% gratis & open source tanpa batasan API key berbayar.
   - Algoritma rute ramah pesepeda didukung oleh **OSRM Bicycle Engine**.
   - Cari tujuan dengan teks atau cukup **tap langsung di atas peta** untuk langsung menghitung rute.

2. **Target Waktu Tempuh / Cut-Off Time (COT)**:
   - Tentukan target durasi perjalanan (contoh: 2 jam, 3.5 jam, atau custom jam & menit).
   - Menghitung kecepatan rata-rata wajib (*Required Speed*) sejak awal hingga garis finis.

3. **Asisten Pacing Cerdas (Real-time Pacing Engine)**:
   - **⚡ Tambah Kecepatan**: Memberi tahu jika kecepatan saat ini tertinggal dari target COT beserta berapa km/jam yang harus ditambah.
   - **🛑 Kurangi Kecepatan**: Mengingatkan jika Anda mengayuh terlalu agresif agar tidak kehabisan tenaga (*bonking*) sebelum tanjakan atau sisa rute.
   - **✅ Pacing Optimal**: Konfirmasi bahwa ritme kayuhan Anda sudah pas sesuai rencana.

4. **Pengingat Fueling & Istirahat (Smart Reminders)**:
   - **🍌 Notifikasi Fueling / Nutrisi**: Notifikasi & getar berkala (default: tiap 30 menit) agar pesepeda tidak lupa minum elektrolit dan asupan karbohidrat.
   - **☕ Notifikasi Istirahat (Rest Stop)**: Mengingatkan jeda sejenak (default: tiap 60 menit) untuk peregangan otot dan cek kondisi sepeda.
   - Interval dapat diatur sesuai kebutuhan di menu **Pengaturan**.

5. **Pelacakan Background (Foreground Service)**:
   - Menggunakan Android **Foreground Service** dengan notifikasi interaktif terus-menerus.
   - GPS dan perhitungan pacing tetap aktif meski layar ponsel dimatikan atau saat membuka aplikasi lain.

6. **Cockpit HUD Ramah Pesepeda**:
   - Speedometer digital besar dengan kontras tinggi (mudah dibaca saat ponsel terpasang di handlebar sepeda).
   - Visual progress rute, sisa jarak (km), countdown COT, dan countdown ke fueling/rest berikutnya.
   - Mode **Simulasi GPS** untuk menguji coba fitur tanpa harus keluar ruangan.

---

## 🚀 Panduan Build APK Menggunakan GitHub Actions

Proyek ini sudah dilengkapi file workflow CI/CD di [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml). Anda tidak perlu menginstal Android Studio atau SDK di komputer lokal untuk mengompilasi APK.

### Langkah-langkah:

1. **Inisialisasi Git dan Commit**:
   Buka terminal di folder `PacePilot` dan jalankan:
   ```bash
   git init
   git add .
   git commit -m "feat: Initial commit for PacePilot cycling COT app"
   ```

2. **Buat Repository Baru di GitHub**:
   - Buka [GitHub New Repository](https://github.com/new).
   - Beri nama repository, misalnya `PacePilot`.
   - Pilih *Public* atau *Private*.

3. **Hubungkan & Push ke GitHub**:
   ```bash
   git branch -M main
   git remote add origin https://github.com/<USERNAME-ANDA>/PacePilot.git
   git push -u origin main
   ```

4. **Unduh APK dari GitHub Actions**:
   - Masuk ke tab **Actions** di repositori GitHub Anda.
   - Anda akan melihat workflow **"Build Android APK"** berjalan secara otomatis.
   - Setelah selesai (tanda centang hijau ✅), klik workflow run tersebut.
   - Di bagian bawah (**Artifacts**), Anda akan menemukan file **`PacePilot-Debug-APK`**.
   - Download file ZIP tersebut, ekstrak, lalu install file `.apk` ke ponsel Android Anda!

---

## 📁 Struktur Kode

```
PacePilot/
├── .github/workflows/
│   └── android-build.yml           # CI/CD GitHub Actions untuk build APK
├── app/
│   ├── build.gradle.kts            # Dependensi Android, Compose, & OSM
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Permissions (GPS, Notif, Foreground Service)
│       │   └── java/com/pacepilot/app/
│       │       ├── PacePilotApp.kt            # Setup OSM cache & Notification Channels
│       │       ├── MainActivity.kt            # Entry point & permission handling
│       │       ├── data/
│       │       │   ├── model/RouteModels.kt   # Model data rute, metrics, dan pacing
│       │       │   └── api/RoutingService.kt  # Integrasi OSRM Bike Routing & Nominatim
│       │       ├── pacing/
│       │       │   └── PacingEngine.kt        # Algoritma hitung required speed & COT
│       │       ├── service/
│       │       │   └── RideTrackingService.kt # Foreground service GPS & alarm scheduler
│       │       └── ui/
│       │           ├── components/
│       │           │   ├── OsmMapView.kt      # Komponen Map OSM & Polyline
│       │           │   ├── SpeedGauge.kt      # Speedometer digital HUD
│       │           │   └── PacingBadge.kt     # Banner arahan pacing (+/- km/jam)
│       │           └── screens/
│       │               ├── RouteSetupScreen.kt    # Pilih tujuan, rute & set COT
│       │               ├── ActiveRideScreen.kt    # Cockpit tampilan saat gowes
│       │               ├── SettingsBottomSheet.kt # Pengaturan interval fueling/rest
│       │               └── RideSummaryDialog.kt   # Ringkasan hasil & pencapaian COT
│       └── test/
│           └── java/com/pacepilot/app/
│               └── PacingEngineTest.kt        # Unit test perhitungan pacing
├── gradle/wrapper/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## ⚙️ Persyaratan Sistem
- **Minimum Android SDK**: Android 8.0 (API Level 26)
- **Target SDK**: Android 14 (API Level 34)
- **JDK Target**: Java 17
