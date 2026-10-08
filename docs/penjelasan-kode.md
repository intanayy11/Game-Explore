# Penjelasan Kode — Game Explore
### Bahan rekaman video penjelasan kode (bukan demo aplikasi)

> **Aturan main video:** jelaskan *kode & alasan*, jangan sekadar *klik-klik fitur*.
> Setiap bagian di bawah punya **[TUJUAN]**, **[KODE]**, dan **[YANG DIKATAKAN]** (narasi yang bisa dibaca hampir verbatim).
> Estimasi total: 10–12 menit.

---

## 0. Pembuka (±30 detik)

**[YANG DIKATAKAN]**
> "Ini aplikasi **Game Explore** — katalog dan eksplorasi video game berbasis Kotlin + Jetpack Compose, yang mengambil data real-time dari REST API **RAWG**. Arsitekturnya **MVVM**: data dari API masuk lewat *Retrofit → Repository → ViewModel*, lalu dirender oleh *Compose* lewat *state-driven UI*. Saya akan jelaskan kode dari lapisan paling bawah (data) sampai lapisan paling atas (UI), lalu menutup dengan fitur search-nya."

**Struktur paket (tampilkan di video):**
```
com.pemmob.gameexplore/
├── data/
│   ├── model/Game.kt              ← bentuk data dari API
│   ├── remote/RawgApiService.kt   ← endpoint Retrofit
│   ├── remote/RetrofitClient.kt   ← setup Retrofit
│   └── repository/GameRepository.kt ← sumber data tunggal
├── ui/
│   ├── navigation/NavGraph.kt     ← routing 2 layar
│   ├── screen/HomeScreen.kt       ← layar 1
│   ├── screen/GameDetailScreen.kt ← layar 2
│   ├── theme/{Color,Type,Theme}.kt ← Material Design 3
│   └── viewmodel/GameViewModel.kt ← state & logika
└── util/{Constants,Format}.kt     ← API key & formatter
```
Katakan: *"pemisahan folder ini mencerminkan MVVM: `data` = Model, `ui/viewmodel` = ViewModel, `ui/screen` = View."*

---

## 1. `util/Constants.kt` — titik masuk API (±30 detik)

**[TUJUAN]** Menaruh BASE_URL dan API key di satu tempat (DRY, mudah diganti).

**[KODE]**
```kotlin
object Constants {
    const val BASE_URL = "https://api.rawg.io/api/"
    const val API_KEY = "***"
}
```

**[YANG DIKATAKAN]**
> "Pertama, `Constants` — sebuah `object` Kotlin berisi dua konstanta. `BASE_URL` adalah basis endpoint RAWG, dan `API_KEY` wajib disertakan sebagai query parameter `?key=` di setiap request sesuai dokumentasi RAWG. Semua file lain mengambil dari sini, jadi kalau key berganti kita ubah di satu tempat saja."

**Catatan untuk perekam:** sebutkan kalimat penting — *"key ini di-hardcode agar aplikasi langsung jalan setelah di-clone; pada produksi sebaiknya disimpan di `local.properties` lalu di-expose lewat `BuildConfig`."* (menunjukkan paham keamanan)

---

## 2. `data/model/Game.kt` — data class & null safety (±1 menit)

**[TUJUAN]** Memetakan JSON RAWG ke objek Kotlin; di sini letak **data class** dan **null safety** (syarat spek a).

**[KODE]**
```kotlin
data class GameResponse(
    @SerializedName("results") val results: List<Game>?
)
data class Game(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("released") val released: String?,
    @SerializedName("background_image") val backgroundImage: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("description_raw") val descriptionRaw: String?,
    @SerializedName("genres") val genres: List<Genre>?
)
data class Genre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?
)
```

**[YANG DIKATAKAN]**
> "Ini representasi JSON dari API dalam bentuk **data class**. Catatan penting: field yang bertipe `String?` atau `Double?` — dengan tanda tanya — adalah **null safety**. Kenapa? Karena API RAWG tidak menjamin semua field ada: `released` bisa null (game belum diumumkan → kita tampilkan `TBA`), `rating` bisa 0. Dengan tipe nullable, Kotlin **memaksa** kita menangani kasus null saat kompilasi — tidak akan ada `NullPointerException` di runtime.
> `@SerializedName` mencocokkan nama JSON (snake_case, mis. `background_image`) ke nama properti Kotlin (camelCase).
> Endpoint **list** hanya mengembalikan ringkasan; **deskripsi lengkap** (`description_raw`) hanya ada di endpoint detail — itu sebabnya nanti Home Screen tidak menampilkan deskripsi."

---

## 3. `data/remote/RetrofitClient.kt` + `RawgApiService.kt` — networking (±1 menit 30 detik)

**[TUJUAN]** Menyiapkan Retrofit (spek f) dan mendeklarasikan endpoint sebagai interface (bukan class).

**[KODE — RetrofitClient]**
```kotlin
object RetrofitClient {
    val apiService: RawgApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RawgApiService::class.java)
    }
}
```

**[KODE — RawgApiService]**
```kotlin
interface RawgApiService {
    @GET("games")
    suspend fun getGames(
        @Query("key") apiKey: ***        @Query("search") search: String? = null
    ): GameResponse

    @GET("games/{id}")
    suspend fun getGameDetail(
        @Path("id") gameId: Int,
        @Query("key") apiKey: ***
    ): Game
}
```

**[YANG DIKATAKAN]**
> "`RetrofitClient` adalah `object` singleton dengan properti `by lazy` — artinya Retrofit baru dibuat **sekali**, saat pertama kali dipakai, lalu dipakai bersama semua pemanggil. `GsonConverterFactory` otomatis mengubah JSON dari API menjadi data class tadi.
> Di `RawgApiService`, semua fungsi **`suspend`** — inilah yang membuat networking berjalan di **coroutine**, sehingga UI tidak pernah freeze/di-block menunggu internet.
> Dua endpoint saja: `@GET("games")` untuk daftar (dengan `@Query` untuk key dan pencarian), dan `@GET("games/{id}")` untuk detail, di mana `{id}` diisi lewat `@Path` — jadi kalau kita panggil `getGameDetail(3498)`, URL-nya jadi `games/3498?key=...`.
> Fungsi `getGames` punya default value `search = null` — inilah **lambda/default parameter** Kotlin yang memudahkan: tanpa search, fungsi ini tetap dipakai untuk memuat daftar awal."

---

## 4. `data/repository/GameRepository.kt` — lapisan repository (±45 detik)

**[TUJUAN]** Spek f mewajibkan repository; semua ViewModel hanya bicara ke sini, tidak pernah ke Retrofit langsung.

**[KODE]**
```kotlin
class GameRepository {
    private val api = RetrofitClient.apiService

    suspend fun getGames(search: String? = null): List<Game> {
        val response = api.getGames(apiKey = Constants.API_KEY, search = search)
        return response.results ?: emptyList()
    }

    suspend fun getGameDetail(gameId: Int): Game {
        return api.getGameDetail(gameId = gameId, apiKey = Constants.API_KEY)
    }
}
```

**[YANG DIKATAKAN]**
> "Repository adalah satu-satunya pintu keluar data. Keuntungannya: kalau besok kita ganti RAWG ke API lain, cukup ubah file ini — ViewModel dan UI tidak disentuh.
> Perhatikan `response.results ?: emptyList()`: operator **elvis** `?:` — kalau `results` null, kembalikan list kosong, sehingga UI selalu menerima `List<Game>` yang aman. API key juga disisipkan **di sini**, jadi format key tidak bocor ke lapisan UI."

---

## 5. `ui/viewmodel/GameViewModel.kt` — inti MVVM & search (±3 menit)

**[TUJUAN]** Menjelaskan state-driven UI (spek d), sealed interface, debounce, dan antisipasi race condition. Ini bagian **paling penting** — jangan terburu-buru.

### 5a. State sebagai sealed interface
**[KODE]**
```kotlin
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val games: List<Game>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val game: Game) : DetailUiState
    data class Error(val message: String) : DetailUiState
}
```

**[YANG DIKATAKAN]**
> "Setiap layar punya satu state tunggal berupa **sealed interface** — hanya bisa berupa Loading, Success, atau Error, tidak mungkin ada kombinasi yang tidak valid. UI tinggal `when (state)` dan menampilkan layar yang tepat. Ini inti **state-driven UI**: yang dirender ditentukan oleh state, bukan oleh mutasi view. Karena hanya ada sedikit object state, **recomposition** Compose menjadi efisien — recomposition hanya terjadi ketika state benar-benar berganti."

### 5b. Mutable state di ViewModel
**[KODE]**
```kotlin
var homeUiState: HomeUiState by mutableStateOf(HomeUiState.Loading)
    private set
var searchQuery: String by mutableStateOf("")
    private set
```

**[YANG DIKATAKAN]**
> "`by mutableStateOf` membuat state yang diamati oleh Compose — delegate `by` membuatnya bisa dibaca seperti properti biasa. Kunci `private set` menjaga **encapsulation**: layar hanya boleh *membaca* state; perubahan hanya lewat fungsi ViewModel."

### 5c. Debounce search — jawaban spek "Search functionality"
**[KODE]**
```kotlin
private var searchJob: Job? = null
private const val SEARCH_DEBOUNCE_MS = 300L   // di companion object

fun updateSearchQuery(query: String) {
    searchQuery = query            // UI langsung responsif tiap ketikan
    searchJob?.cancel()            // batalkan timer sebelumnya
    searchJob = viewModelScope.launch {
        delay(SEARCH_DEBOUNCE_MS)  // tunggu 300 ms
        fetchGames(query)          // baru kirim request
    }
}

fun fetchGames(query: String? = null) {
    val requestId = ++homeRequestId
    val keyword = query?.trim().orEmpty().ifEmpty { null }
    viewModelScope.launch {
        homeUiState = HomeUiState.Loading
        try {
            val games = repository.getGames(search = keyword)
            if (requestId == homeRequestId)
                homeUiState = HomeUiState.Success(games)
        } catch (e: Exception) {
            if (requestId == homeRequestId)
                homeUiState = HomeUiState.Error(e.localizedMessage ?: "Terjadi kesalahan")
        }
    }
}
```

**[YANG DIKATAKAN]**
> "Ini bagian yang paling sering ditanya penguji. Coba ketik 'minecraft' — ada 9 karakter, padahal cukup **satu** request. Caranya:
> 1. Tiap ketikan menyalakan `searchJob` baru, tapi **`searchJob?.cancel()`** membatalkan timer sebelumnya. Jadi request cuma terkirim kalau user berhenti mengetik **300 ms** — inilah **debounce**. Tanpa ini, 9 ketikan = 9 request = gampang kena rate limit (RAWG gratis cuma ±20 request/menit).
> 2. `searchQuery` tetap di-update **seketika** supaya kotak pencarian terasa responsif — hanya *request API*-nya yang ditunda. Jadi state UI dan network dipisah.
> 3. **Race condition**: kalau user mengetik A lalu cepat B, respons A bisa sampai **lebih lambat** dari respons B dan menimpa hasil yang benar. Solusinya `homeRequestId` — tiap request mengambil nomor urut, dan hanya respons dengan nomor **terakhir** yang boleh menulis state. Respons basi diabaikan.
> `keyword` di-trim dan string kosong dikonversi ke `null`, supaya search kosong = memuat daftar awal, bukan mengirim `search=''`.
> Terakhir, `try-catch` mengubah kegagalan network menjadi `HomeUiState.Error`, sehingga layar menampilkan tombol **'Coba Lagi'** — bukan crash."

### 5d. Detail: reset & stale state
**[KODE]**
```kotlin
fun resetDetailState() { detailUiState = DetailUiState.Loading }
fun fetchGameDetail(gameId: Int) { /* pola requestId yang sama */ }
```

**[YANG DIKATAKAN]**
> "Ada satu bug halus yang saya cegah: ketika user membuka game A lalu kembali lalu buka game B, layar detail sempat menampilkan data A selama satu frame sebelum B selesai dimuat. Maka di `NavGraph`, `resetDetailState()` dipanggil **tepat sebelum navigasi**, mengosongkan state ke Loading — frame pertama selalu spinner, bukan data basi."

---

## 6. `ui/navigation/NavGraph.kt` — dua layar + argument (±1 menit)

**[TUJUAN]** Routing (spek g), sealed class route, dan pengiriman `gameId` antar layar.

**[KODE]**
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{gameId}") {
        fun createRoute(gameId: Int) = "detail/$gameId"
    }
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val viewModel: GameViewModel = viewModel()   // satu ViewModel dipakai bersama

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onGameClick = { gameId ->
                    viewModel.resetDetailState()
                    navController.navigate(Screen.Detail.createRoute(gameId))
                }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("gameId") { type = NavType.IntType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getInt("gameId") ?: 0
            GameDetailScreen(gameId = gameId, viewModel = viewModel,
                onBackClick = { navController.popBackStack() })
        }
    }
}
```

**[YANG DIKATAKAN]**
> "Ada dua route sesuai kebutuhan **minimal dua layar**: `home` dan `detail/{gameId}`. Route detail punya **argument** `gameId` bertipe Int — dideklarasikan di `navArgument`, lalu dibaca dari `backStackEntry`. Fungsi `createRoute(id)` menyusun URL-nya dengan **string template** `"detail/$gameId"`.
> Route dibungkus **sealed class** — kalau salah ketik route, error muncul saat kompilasi, bukan saat aplikasi jalan.
> Satu instance `GameViewModel` dibuat di sini dengan `viewModel()` dan diteruskan ke kedua layar — jadi state search di Home **tetap tersimpan** saat user kembali dari Detail. `resetDetailState()` dipanggil sebelum `navigate` (penjelasan tadi), dan `popBackStack()` menangani tombol kembali."

---

## 7. `ui/screen/HomeScreen.kt` — Lazy layout & kartu game (±1 menit 30 detik)

**[TUJUAN]** LazyVerticalGrid (spek c), recomposition, dan pencarian di UI.

**[KODE — bagian inti]**
```kotlin
@Composable
fun HomeScreen(viewModel: GameViewModel, onGameClick: (Int) -> Unit) {
    val uiState = viewModel.homeUiState
    val searchQuery = viewModel.searchQuery

    Scaffold(topBar = { TopAppBar(title = { Text("Game Explore") }) }) { innerPadding ->
        Column(Modifier.padding(innerPadding).padding(horizontal = 16.dp)) {
            SearchField(query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) })
            when (uiState) {
                is HomeUiState.Loading -> CircularProgressIndicator()
                is HomeUiState.Success -> {
                    if (uiState.games.isEmpty()) EmptyState("Game tidak ditemukan")
                    else LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.games, key = { it.id }) { game ->
                            GameItemCard(game = game,
                                onClick = { onGameClick(game.id) })
                        }
                    }
                }
                is HomeUiState.Error -> EmptyState(uiState.message,
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.fetchGames(viewModel.searchQuery) })
            }
        }
    }
}
```

**[YANG DIKATAKAN]**
> "Layar dibaca atas-ke-bawah: `Scaffold` + `TopAppBar`, lalu `SearchField`, lalu konten yang dipilih oleh `when (uiState)`. Poin-poinnya:
> - **`LazyVerticalGrid` dengan `GridCells.Fixed(2)`** — katalog dua kolom. Kata 'lazy' berarti item baru dibuat saat akan tampil di layar; dari ±900 ribu game di RAWG, hanya 20 yang dimuat dan dirender — inilah fungsi lazy layout.
> - **`key = { it.id }`** — Compose mengenali tiap item secara unik, jadi animasi dan scroll jadi akurat ketika hasil search berganti.
> - **`when` ekhaustif** pada sealed interface — Kotlin menjamin tidak ada state yang terlewat; kalau kita menambah state baru tanpa menanganinya, kode gagal kompilasi.
> - Search bar: `onQueryChange` memanggil `viewModel.updateSearchQuery(it)` — **unidirectional data flow**: event naik ke ViewModel, state turun ke UI, tidak ada state ganda.
> - Empty state dan error state terpisah: 'Game tidak ditemukan' berarti request sukses tapi hasilnya nol; error + 'Coba Lagi' berarti request gagal.
> - Kartu: `AsyncImage` (Coil) memuat `background_image` dengan `ContentScale.Crop`, ditambah overlay gradasi `Brush.verticalGradient` supaya tampilan modern, lalu judul, rating (dari `formatRating`), dan tanggal rilis **ISO 8601** sesuai spek — kalau null tampil 'TBA'."

---

## 8. `ui/screen/GameDetailScreen.kt` — detail & lifecycle (±1 menit 30 detik)

**[TUJUAN]** Layar kedua, `LaunchedEffect` (side effect), dan HTML description.

**[KODE — bagian inti]**
```kotlin
@Composable
fun GameDetailScreen(gameId: Int, viewModel: GameViewModel, onBackClick: () -> Unit) {
    LaunchedEffect(gameId) { viewModel.fetchGameDetail(gameId) }

    when (val uiState = viewModel.detailUiState) {
        is DetailUiState.Loading -> CircularProgressIndicator()
        is DetailUiState.Success -> {
            val game = uiState.game
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AsyncImage(model = game.backgroundImage, /* ... */)
                Text(game.name ?: "Unknown Game", style = headlineSmall)
                // dua kartu: rating  |  tanggal rilis
                // chips genre (FlowRow + SuggestionChip)
                val cleanDesc = Html.fromHtml(
                    game.descriptionRaw ?: game.description ?: "Tidak ada deskripsi.",
                    Html.FROM_HTML_MODE_LEGACY
                ).toString()
                Text(cleanDesc, style = bodyMedium)
            }
        }
        is DetailUiState.Error -> Button(onClick = { viewModel.fetchGameDetail(gameId) })
    }
}
```

**[YANG DIKATAKAN]**
> "`LaunchedEffect(gameId)` adalah **side effect** Compose: kode di dalamnya dijalankan tepat satu kali ketika layar muncul (atau `gameId` berubah), dan otomatis dibatalkan saat layar ditutup — tempat yang benar untuk memanggil pemanggilan data.
> Tiga blok `when`: Loading (spinner), Success (konten scrollable), Error (retry).
> - **Deskripsi**: RAWG mengirim deskripsi dalam bentuk **HTML**. Kalau langsung ditampilkan, tag-nya ikut muncul di layar. `Html.fromHtml(...)` menghapus tag sehingga didapat teks bersih. `descriptionRaw` diprioritaskan karena tanpa format tag; fallback ke `description`, lalu teks default kalau keduanya null.
> - **Genre** ditampilkan sebagai **chip** per genre dalam `FlowRow`, konsisten dengan dua kartu `Surface` berbentuk pil di atasnya (rating dan tanggal), sehingga bahasa desainnya satu.
> - **Rating**: `formatRating` mengubah `4.5` jadi '4.5 / 5' dengan `Locale.ROOT` — angka selalu memakai titik, bukan koma yang mengikuti locale perangkat; `ratingStars` mengubahnya jadi ★★★★★. Keduanya ada di `util/Format.kt` sebagai **fungsi top-level** murni."

---

## 9. `util/Format.kt` — fungsi top-level (±45 detik)

**[KODE]**
```kotlin
fun formatRating(rating: Double?): String =
    rating?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "N/A"

fun ratingStars(rating: Double?): String {
    val filled = rating?.roundToInt()?.coerceIn(0, 5) ?: 0
    return "★".repeat(filled) + "☆".repeat(5 - filled)
}
```

**[YANG DIKATAKAN]**
> "Ini **fungsi top-level** Kotlin — berdiri sendiri tanpa harus jadi anggota class, sehingga bisa dipanggil langsung dari dua screen berbeda tanpa duplikasi kode (DRY). `formatRating` menangani rating null → 'N/A'. `ratingStars` membulatkan ke 5 poin, membatasi 0–5 dengan `coerceIn`, lalu membangun string bintang dengan `repeat`."

---

## 10. `ui/theme/` — Material Design 3 (±1 menit)

**[TUJUAN]** Spek b: Theme + typography.

**[KODE — ringkas Theme.kt]**
```kotlin
@Composable
fun GameExploreTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors   // palet gaming, bukan dynamicColor
    val typography = GameExploreTypography
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
```

**[YANG DIKATAKAN]**
> "Aplikasi memenuhi **Material Design 3** dari `build.gradle`, ditambah dua file kustom:
> - `Color.kt` — palet warna khas aplikasi: ungu `#8B5CF6`, aksen cyan `#22D3EE`, pink `#FF6B9D`, background near-black untuk mode gelap. **`dynamicColor` sengaja dimatikan** agar identitas visual tetap konsisten, tidak diambil dari wallpaper HP.
> - `Type.kt` — **typography** lengkap (headline/title/body/label) bukan hanya `bodyLarge`; judul dibuat tebal dan `bodyMedium` diberi `lineHeight` supaya deskripsi panjang nyaman dibaca.
> Keduanya di-*set* lewat `MaterialTheme` sehingga semua Composable otomatis memakainya — tidak ada warna/font hardcode di layar, dan aplikasi otomatis punya mode gelap-terang mengikuti sistem."

---

## 11. `MainActivity.kt` — jembatan Android & Compose (±30 detik)

**[KODE]**
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameExploreTheme { NavGraph() }
        }
    }
}
```

**[YANG DIKATAKAN]**
> "Simpul masuknya `Activity` tunggal. `enableEdgeToEdge()` membuat konten menembus area status bar. `setContent { ... }` adalah **titik masuk Compose**: alih-alih layout XML, UI dideklarasikan sebagai fungsi Composable — di sini `GameExploreTheme` membungkus `NavGraph`. Jadi susunannya: Activity → Theme → NavGraph → Home/Detail, dengan ViewModel di tengah."

---

## 12. Alur data end-to-end — rangkuman (±1 menit)

**[YANG DIKATAKAN]**
> "Saya rangkum satu alur penuh dari ketikan sampai layar:
> 1. User mengetik di `SearchField` → `onQueryChange` memanggil `viewModel.updateSearchQuery(query)`.
> 2. ViewModel update `searchQuery` (UI langsung menampilkan teks), membatalkan timer lama, lalu setelah 300 ms debounce memanggil `fetchGames`.
> 3. `fetchGames` menaikkan nomor request, lalu `repository.getGames(search)` → `Retrofit` mengirim `GET /games?search=...&key=...` sebagai coroutine.
> 4. Respons JSON di-`Gson`-kan menjadi `List<Game>` (data class).
> 5. Karena requestId sesuai yang terakhir, state berubah `Loading → Success(games)`.
> 6. Compose membaca `homeUiState` dan melakukan **recomposition** — hanya grid yang tampil, layar lain tidak di-render ulang.
>
> Kalau gagal → `Error`, kalau kosong → 'Game tidak ditemukan'. Klik kartu → `navigate("detail/{id}")` → `LaunchedEffect` memuat detail → `description_raw` dibersihkan dari HTML dan tampil.

**Pemetaan ke spek responsi (tampilkan slide/README):**

| Butir spek | Lokasi kode |
|---|---|
| a. Kotlin (data class, null safety, default param) | `Game.kt`, `RawgApiService.kt`, `Format.kt` |
| b. Compose + M3 + theme/typography | `ui/screen/*`, `ui/theme/*` |
| c. Lazy layout (LazyVerticalGrid) | `HomeScreen.kt` |
| d. State-driven UI + search | `GameViewModel.kt` (sealed state, debounce 300 ms) |
| e. Networking RAWG (nama, rating, tanggal ISO 8601, deskripsi) | `RetrofitClient`, `RawgApiService`, `Game.kt` |
| f. MVVM (Retrofit, repository) | `data/*` → `GameViewModel` → `ui/*` |
| g. Dua layar (Home + Detail) | `NavGraph.kt` |
| h. README + video ini | repository root |

---

## 13. Penutup (±20 detik)

**[YANG DIKATAKAN]**
> "Ringkasnya: **MVVM** memisahkan data, logika, dan tampilan; **coroutine + suspend** menjaga UI tetap responsif; **sealed interface** membuat state selalu valid; **debounce dan request ID** membuat search cepat namun aman dari race condition; dan seluruh tampilan memakai **Material Design 3** dengan theme kustom. Terima kasih."

---

## Checklist sebelum rekam
- [ ] Build `assembleDebug` sukses, app terpasang di emulator
- [ ] Siapkan 1 kondisi per layar: home terisi, search mengetik, "Game tidak ditemukan", detail (GTA V sudah teruji), tombol "Coba Lagi"
- [ ] Tutup notifikasi & rapikan status bar emulator
- [ ] Rekam layar minimal 1080p; baca bagian **[YANG DIKATAKAN]** sebagai narasi
- [ ] Pastikan **API key tidak terlihat terlalu lama** di layar (atau blur saat menampilkan `Constants.kt`)
