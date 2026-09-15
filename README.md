# PacePilot - All-In-One Cycling Computer, Navigator & Health Pacer

**PacePilot** adalah aplikasi Android all-in-one berbasis **Kotlin & Jetpack Compose** yang menggabungkan fitur terbaik dari **Strava** (analisis riwayat & performa), **Komoot** (perencana rute, rute favorit & GPX import/export), **Huawei Health** (kalkulasi kalori METs, status hidrasi & waktu pemulihan tubuh), serta **Google Maps** (navigasi turn-by-turn dan panduan suara Text-to-Speech) dengan fokus utama pencapaian target waktu tempuh (**COT / Cut-Off Time**).

Proyek ini terintegrasi penuh dengan **GitHub Actions** untuk kompilasi APK otomatis tanpa perlu instalasi SDK lokal.

---

## Fitur All-In-One

### 1. Navigasi Sepeda Turn-by-Turn & Voice Cues (Google Maps Style)
- **Turn-by-Turn Navigation Banner**: Indikator arah belok interaktif (belok kiri, belok kanan, putar balik, serong, bundaran, tujuan) lengkap dengan hitung mundur jarak belokan.
- **Panduan Suara (Text-to-Speech)**: Mengumumkan instruksi navigasi ("Dalam 100 meter, belok kanan ke Jalan Sudirman") serta peringatan pacing tanpa perlu menatap layar.
- **Peta Bebas Biaya (OpenStreetMap / osmdroid)**: Tidak memerlukan Google Cloud API Key atau kartu kredit.
- **Rute Khusus Sepeda**: Didukung algoritma perutean jalur pesepeda **OSRM Bicycle Engine**.

### 2. Perencana & Penyimpanan Rute (Komoot Style)
- **Simpan Rute Favorit**: Simpan hasil perutean dengan nama kustom untuk digunakan kembali kapan saja.
- **Ekspor GPX 1.1**: Ekspor rute ke format `.gpx` standar untuk dipindahkan ke Garmin, Wahoo, atau dibagikan ke pesepeda lain.
- **Impor GPX**: Impor berkas GPX dari penyimpanan ponsel langsung ke daftar rute.

### 3. Riwayat Aktivitas & Analisis Performa (Strava Style)
- **Otomatis Tersimpan**: Sesi gowes otomatis tersimpan saat tombol akhiri ditekan.
- **Log Metrik Lengkap**: Total jarak, waktu tempuh, kecepatan rata-rata, kecepatan maksimum, kalori aktif, dan status keberhasilan target COT.
- **Lifetime Statistics**: Ringkasan akumulasi seluruh sesi gowes dan rekor kecepatan tertinggi.

### 4. Intelijen Kesehatan & Kebugaran (Huawei Health Style)
- **Kalkulasi Kalori Ilmiah (METs)**: Menghitung pembakaran kalori aktif secara dinamis berdasarkan kecepatan dan bobot pesepeda.
- **Estimasi Kehilangan Cairan & Hidrasi**: Menghitung cairan tubuh yang hilang (Liter) untuk panduan rehidrasi.
- **Waktu Pemulihan Tubuh (Recovery Hours)**: Rekomendasi jam istirahat sebelum sesi gowes berikutnya.
- **Panduan Cadence & Zona Detak Jantung**: Rekomendasi irama kayuhan pedal (80-90 RPM endurance).

### 5. Asisten Pacing Real-Time & Peringatan Cerdas (COT Engine)
- **Penyesuaian Kecepatan Real-Time**: Perbandingan instan antara kecepatan aktual dengan kecepatan wajib untuk lolos target waktu tempuh (COT).
- **Notifikasi Nutrisi / Fueling**: Pengingat berkala (default: 30 menit) untuk asupan elektrolit dan camilan energi.
- **Notifikasi Istirahat (Rest Stop)**: Pengingat jeda peregangan otot kaki (default: 60 menit).
- **Background Foreground Service**: Pelacakan GPS dan perhitungan pacing terus berjalan lancar saat layar ponsel mati.

---

## Arsitektur Aplikasi

```
PacePilot/
├── .github/workflows/
│   └── android-build.yml           # CI/CD otomatis kompilasi APK di GitHub
├── app/src/main/
│   ├── AndroidManifest.xml         # Izin GPS, Audio, Foregound Service, Notifikasi
│   └── java/com/pacepilot/app/
│       ├── MainActivity.kt            # Scaffold 4 Tab: Navigasi, Rute, Aktivitas, Kesehatan
│       ├── PacePilotApp.kt            # Setup OSM cache & notification channels
│       ├── data/
│       │   ├── model/
│       │   │   ├── RouteModels.kt     # Model rute, pacing, dan metrik live
│       │   │   └── NavigationModels.kt# Model step navigasi, saved route, log gowes
│       │   ├── api/RoutingService.kt  # OSRM Bicycle API & Geocoding
│       │   └── repository/
│       │       ├── RouteRepository.kt    # Penyimpanan rute lokal & GPX parser
│       │       └── ActivityRepository.kt # Penyimpanan riwayat aktivitas gowes
│       ├── fitness/
│       │   └── HealthCalculator.kt    # Formula kalori METs, hidrasi & recovery
│       ├── pacing/
│       │   └── PacingEngine.kt        # Algoritma hitung required speed & status pacing
│       ├── service/
│       │   ├── RideTrackingService.kt # Foreground GPS tracking service
│       │   └── VoiceNavigationManager.kt # Android native TextToSpeech engine
│       └── ui/
│           ├── components/
│           │   ├── BottomNavBar.kt    # Bar navigasi 4 tab
│           │   ├── TurnBanner.kt      # Banner petunjuk belokan Google Maps style
│           │   ├── SpeedGauge.kt      # Speedometer digital kontras tinggi
│           │   ├── PacingBadge.kt     # Arahan tambah/kurangi kecepatan
│           │   └── OsmMapView.kt      # Tampilan peta OpenStreetMap
│           └── screens/
│               ├── RouteSetupScreen.kt     # Pemilihan tujuan, COT, & simpan rute
│               ├── ActiveRideScreen.kt     # Cockpit HUD live saat gowes
│               ├── SavedRoutesScreen.kt    # Layar rute favorit & GPX
│               ├── ActivityHistoryScreen.kt# Layar log riwayat gowes
│               ├── HealthMetricsScreen.kt  # Layar kalori, hidrasi & recovery
│               ├── SettingsBottomSheet.kt  # Pengaturan interval & suara
│               └── RideSummaryDialog.kt    # Ringkasan hasil pencapaian COT
```

---

## Cara Mengunduh APK dari GitHub Actions

1. Commit dan push kode ke cabang `main` di GitHub.
2. Buka tab **Actions** di repositori GitHub Anda: [https://github.com/ululazq/PacePilot/actions](https://github.com/ululazq/PacePilot/actions).
3. Klik workflow run terbaru **"Build Android APK"**.
4. Setelah selesai (tanda centang hijau), unduh berkas APK dari bagian **Artifacts (`PacePilot-Debug-APK`)**.
5. Pasang berkas `.apk` pada perangkat Android Anda.
