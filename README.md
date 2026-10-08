# Game Explore - Katalog & Eksplorasi Video Game

Aplikasi Android untuk menelusuri katalog video game: data diambil **secara dinamis dari RAWG Video Games Database API** (bukan data hardcode) dan ditampilkan dengan **Kotlin + Jetpack Compose + Material Design 3**.

Repository: https://github.com/intanayy11/Game-Explore

---

## Screenshots

<table border="1">
  <tr>
    <td align="center" width="33%">
      <b>Home Screen</b><br>
      <img src="screenshots/home.png" width="100%">
    </td>
    <td align="center" width="33%">
      <b>Search Screen</b><br>
      <img src="screenshots/search.png" width="100%">
    </td>
    <td align="center" width="33%">
      <b>Game Detail Screen</b><br>
      <img src="screenshots/detail.png" width="100%">
    </td>
    <td align="center" width="33%">
      <b>Empty State</b><br>
      <img src="screenshots/empty-state.png" width="100%">
    </td>
    <td align="center" width="33%">
      <b>Error State</b><br>
      <img src="screenshots/error-state.png" width="100%">
    </td>
  </tr>
</table>

---

## Fitur

**Home Screen**
- Daftar game dari RAWG API memakai `LazyVerticalGrid` (2 kolom), tiap kartu menampilkan poster gambar dengan *floating rating badge*, judul, genre, dan tanggal rilis (ISO 8601, `YYYY-MM-DD`).
- Search bar: mengetik langsung memperbarui pencarian game secara real-time.
- Kondisi UI responsif: loading (spinner), kosong ("Game tidak ditemukan"), dan error (pesan + tombol Coba Lagi).

**Game Detail Screen**
- Mengambil detail game berdasarkan `id` yang dipilih: banner gambar besar, judul, rating bintang, tanggal rilis, genre, dan deskripsi lengkap.
- Tombol kembali (back) untuk kembali ke Home.

---

## Pemenuhan Persyaratan Teknis

| Persyaratan | Implementasi |
|---|---|
| Kotlin: data class, null safety, lambda | `Game`, `GameResponse`, `Genre` (data class); seluruh field API nullable; lambda pada `clickable`, `joinToString`, `items` |
| Jetpack Compose + Material 3 | Seluruh UI berbasis composable, `MaterialTheme` dengan color scheme custom (Modern Gaming Dark) |
| Theme & typography | `ui/theme/Color.kt`, `Type.kt`, `Theme.kt` |
| Lazy layout | `LazyVerticalGrid` (2 kolom) di `HomeScreen` |
| State & recomposition / search | `mutableStateOf` pada `searchQuery` + `HomeUiState` |
| Networking RAWG | Retrofit 2.11 + Gson, endpoint `/games` dan `/games/{id}` |
| Field wajib | `name`, `rating`, `released` (ISO 8601), `description` / `description_raw` |
| Arsitektur MVVM | Retrofit → `GameRepository` → `GameViewModel` (StateFlow/Compose State) → Composable |
| 2 screen minimum | `HomeScreen`, `GameDetailScreen` |

---

## Arsitektur & Alur Data

```
RAWG API ──► Retrofit (data/remote) ──► DTO (data/model)
                                              │
                                              ▼
                                       GameRepository
                                              │
                                              ▼
                          GameViewModel (sealed UiState + coroutine)
                                              │
                                              ▼
                HomeScreen / GameDetailScreen (Compose, state-driven)
```

**Penjelasan tiap lapisan:**

1. **`data/model/Game.kt`** — data class yang memetakan JSON dari API ke objek Kotlin memakai anotasi `@SerializedName` dari Gson. Semua field di-declare **nullable** (`String?`, `Double?`, `List<Genre>?`) sebagai penerapan *null safety*.
2. **`data/remote/RawgApiService.kt`** — interface Retrofit berisi endpoint list (`games`) dan detail (`games/{id}`). `suspend` function menjamin pemanggilan berjalan di background thread.
3. **`data/remote/RetrofitClient.kt`** — objek tunggal (`object` + `by lazy`) yang membangun `Retrofit` dengan `GsonConverterFactory`.
4. **`data/repository/GameRepository.kt`** — lapisan perantara yang menangani networking dan `null` pada list (`?: emptyList()`).
5. **`ui/viewmodel/GameViewModel.kt`** — mengelola *business logic*, `homeUiState` & `detailUiState` via `mutableStateOf`, serta coroutine scope (`viewModelScope`).
6. **`ui/screen/*`** — composable murni (`HomeScreen` & `GameDetailScreen`) yang merender UI berdasarkan state.
7. **`ui/navigation/NavGraph.kt`** — `NavHost` dengan rute `home` dan `detail/{gameId}`.

## Cara Menjalankan

1. Clone repository ini lalu buka di **Android Studio**.
2. Pastikan API key Anda sudah terpasang di `app/src/main/java/com/pemmob/gameexplore/util/Constants.kt` → `API_KEY`.
3. Tunggu Gradle sync selesai.
4. Jalankan pada emulator atau perangkat fisik Android (Minimum API 29).
5. Pastikan koneksi internet aktif.

Data game disediakan oleh [RAWG](https://rawg.io).
