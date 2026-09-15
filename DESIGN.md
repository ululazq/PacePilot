# PacePilot Design Direction

Dial: ENERGY 2 / RHYTHM 2 / MOTION 1

## Identity & Concept
PacePilot adalah instrumen kokpit sepeda (cycling HUD) yang berfokus pada efisiensi pacing, ketahanan energi, dan pencapaian target waktu tempuh (Cut-Off Time). Visual dirancang tegas, terbaca sekilas saat ponsel terpasang di handlebar sepeda, dan tidak membebani konsentrasi pesepeda di jalan raya.

## Palette & Hierarchy
- Background: `#0B0F17` (Deep Obsidian, kontras tinggi dan hemat daya baterai OLED)
- Card / Panel: `#161E2E` (Dark Slate)
- Border / Stroke: `#26334B`
- Primary Accent: `#00E676` (Neon Green: sinyal ritme on-pace, start action, progress)
- Secondary Accent: `#00E5FF` (Bright Cyan: metrik target COT & required speed)
- Alert & Adjustment: `#FF9100` (Amber: sinyal percepat kayuhan, jadwal nutrisi)
- Critical Alert: `#FF3D00` (Red: overtime COT, tombol stop)
- Text Primary: `#F0F6FC` (WCAG AAA contrast terhadap background)
- Text Secondary: `#8B949E` (WCAG AA contrast)

## Typography & Touch
- Numerik Kecepatan: 58sp font weight Black untuk pembacaan instan tanpa distraksi.
- Label Metrik: 10-12sp SemiBold dengan tracking jelas.
- Touch Targets: Semua tombol minimal 48dp x 48dp, ramah sarung tangan sepeda.

## Reason Log (R-31)
- Warna gelap: Mencegah silau, menghemat konsumsi layar saat GPS foreground service berjalan berjam-jam.
- Kontras warna: Hijau neon dan cyan memisahkan metrik aktual dengan target kalkulasi secara visual tanpa membaca teks panjang.
- No decorative animations: Animasi dibatasi micro-transition (150ms) agar framerate tetap 60fps saat rendering peta GPS.
