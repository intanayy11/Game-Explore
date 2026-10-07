# Game Explore - Katalog & Eksplorasi Video Game

Aplikasi **Game Explore** adalah aplikasi mobile berbasis Android yang dikembangkan menggunakan **Kotlin** dan **Jetpack Compose** untuk memenuhi tugas Responsi Pemrograman Mobile. Aplikasi ini mengambil data game secara dinamis dari **RAWG Video Game Database API**.

---

## 📱 Fitur Utama
1. **Home Screen**:
   - Menampilkan daftar video game secara dinamis dari RAWG API menggunakan `LazyColumn`.
   - Menampilkan gambar poster/background, judul game, rating, dan tanggal rilis.
   - Fitur **Search Bar** untuk mencari game berdasarkan nama secara real-time.
2. **Game Detail Screen**:
   - Menampilkan informasi lengkap game yang dipilih meliputi gambar banner, judul, rating bintang, tanggal rilis, genre, dan deskripsi lengkap.
   - Tombol kembali (back) untuk bernavigasi kembali ke Home Screen.
3. **State Management**:
   - Menerapkan arsitektur **MVVM** (Model-View-ViewModel) dengan state-driven UI (`HomeUiState` & `DetailUiState`) dan coroutines.

---

## 🛠️ Penjelasan Teknis & Arsitektur
Aplikasi ini menerapkan arsitektur **MVVM** dan clean code principles:
- **`data/model`**: Berisi data classes (`Game`, `GameResponse`, `Genre`) yang dianotasi dengan Gson annotations.
- **`data/remote`**: Berisi `RawgApiService` (Retrofit interface) dan `RetrofitClient` untuk konfigurasi koneksi jaringan ke `https://api.rawg.io/api/`.
- **`data/repository`**: `GameRepository` yang menjembatani pengambilan data dari REST API ke ViewModel.
- **`ui/viewmodel`**: `GameViewModel` mengelola business logic, coroutine scopes, search state, dan state UI loading/success/error.
- **`ui/screen`**: Antarmuka berbasis **Jetpack Compose** dan **Material Design 3** (`HomeScreen` dan `GameDetailScreen`).
- **`ui/navigation`**: Navigasi antar layar menggunakan **Jetpack Navigation Compose**.

---

## 🚀 Cara Menjalankan Aplikasi
1. Clone repository ini atau buka project di **Android Studio**.
2. Pastikan koneksi internet aktif (karena aplikasi mengambil data dari RAWG REST API).
3. Jalankan aplikasi pada emulator atau perangkat fisik Android (Minimum SDK 29).
