# VelioraWar

Plugin minigame PvP berbasis Paper untuk Veliora Gardens. Target utama saat ini adalah Paper/Purpur 1.21.8 dan Java 21.

## Fitur V1.1

- GUI penuh melalui `/vgwar`
- Mode `sword_duel`, `mace_pvp`, `cpvp`, dan `all_mode`
- Ukuran match 1vs1, 2vs2, 3vs3, dan 4vs4
- Team merah dan hijau
- Antrean otomatis saat arena sedang digunakan
- Freeze penuh, proteksi blok/interaksi, sound, blindness, dan hitung mundur 3-2-1-GO
- Timer pertandingan dan pemenang berdasarkan eliminasi atau total damage
- Inventory asli disimpan ke `playerdata.yml` dan dikembalikan setelah keluar
- Temporary block dipulihkan setelah match
- Ledakan crystal/anchor tetap memberi damage tanpa merusak block arena
- All Mode bebas dengan NPC refill bawaan dan cooldown 60 detik
- Cheat Guard ringan untuk item ilegal (interval nyata, bukan task setiap tick)
- Command-lock hanya untuk peserta aktif; pemain biasa tetap dapat memakai `/lobby`
- `/vgwar disable` menolak match baru dan memulangkan seluruh peserta dengan inventory aman
- Permission `veliorawar.owner` untuk land/enable/disable dan `veliorawar.admin` untuk operasional
- Seluruh teks, GUI, loadout, cooldown, dan aturan utama dapat diubah lewat YAML

## Build

```bash
mvn clean package
```

JAR hasil build berada di `target/VelioraWar.jar`.

## Setup Arena

```text
/vgwar pos1
/vgwar pos2
/vgwar claim
/vgwar spawn <sword|mace|cpvp|all> <merah|biru>
/vgwar set stay
/vgwar flag
/vgwar enable
```

Keterangan:

- `1` adalah spawn team merah
- `2` adalah spawn team hijau
- Untuk `all_mode`, gunakan `/vgwar set <arena> all_mode unlimited`
- All Mode hanya memerlukan spawn `1`
- NPC refill bawaan menggunakan entity Bukkit. NPC model PLAYER dengan skin berbeda untuk setiap penonton memerlukan packet NPC (misalnya ProtocolLib/PacketEvents), bukan entity Bukkit biasa.

## Command Admin

```text
/vgwar help
/vgwar pos1
/vgwar pos2
/vgwar claim <arena>
/vgwar delete <arena>
/vgwar list
/vgwar info <arena>
/vgwar setwarp
/vgwar set <arena> <mode> <size>
/vgwar set <arena> <1|2>
/vgwar reset <arena>
/vgwar flag <arena>
/vgwar enable <arena>
/vgwar disable <arena>
/vgwar setnpc <arena>
/vgwar reload
```

Player tidak memerlukan command `leave`, `stats`, atau `guide`. Pilihan mode, panduan, dan tombol keluar tersedia di GUI `/vgwar`.

## Konfigurasi

- `config.yml`: timer, cooldown, proteksi, NPC, void, dan Cheat Guard
- `arenas.yml`: region, mode, size, spawn, flag, dan lokasi NPC
- `modes.yml`: semua item, enchantment, potion, icon, dan deskripsi mode
- `gui.yml`: ukuran GUI, judul, material filler, dan posisi slot
- `messages.yml`: seluruh pesan plugin
- `playerdata.yml`: backup inventory, statistik, dan cooldown aman

> Deteksi auto-totem dibuat konservatif dan tidak melakukan ban server otomatis. Sistem hanya memberi peringatan, mencatat log, lalu mengeluarkan pemain dari aktivitas war setelah batas pelanggaran tercapai.
