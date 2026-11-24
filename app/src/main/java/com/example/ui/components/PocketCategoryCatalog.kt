package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsTransit
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.bentoColors

data class PocketCategoryOption(
    val key: String,
    val label: String,
    val group: String,
    val keywords: String,
    val defaultWeight: Int,
    val icon: ImageVector,
)

val POCKET_CATEGORY_CATALOG: List<PocketCategoryOption> = listOf(
    // Harian & Konsumsi
    PocketCategoryOption(
        key = "FOOD",
        label = "Makan & Minum",
        group = "Harian",
        keywords = "makan minum sarapan makan siang makan malam warteg restoran kuliner",
        defaultWeight = 35,
        icon = Icons.Rounded.Restaurant,
    ),
    PocketCategoryOption(
        key = "GROCERIES",
        label = "Belanja Sembako",
        group = "Harian",
        keywords = "sembako supermarket minimarket sayur buah beras dapur bulanan",
        defaultWeight = 20,
        icon = Icons.Rounded.LocalGroceryStore,
    ),
    PocketCategoryOption(
        key = "COFFEE",
        label = "Kopi & Jajan",
        group = "Harian",
        keywords = "kopi kafe boba snack camilan jajan minuman ringan",
        defaultWeight = 10,
        icon = Icons.Rounded.Coffee,
    ),
    PocketCategoryOption(
        key = "HANGOUT",
        label = "Nongkrong & Traktir",
        group = "Harian",
        keywords = "nongkrong traktir pesta kumpul teman makan bareng",
        defaultWeight = 8,
        icon = Icons.Rounded.Celebration,
    ),

    // Transportasi & Perjalanan
    PocketCategoryOption(
        key = "TRANSPORT",
        label = "Transportasi Umum",
        group = "Transportasi",
        keywords = "kereta krl mrt lrt busway transjakarta ojek gojek grab taksi",
        defaultWeight = 15,
        icon = Icons.Rounded.DirectionsTransit,
    ),
    PocketCategoryOption(
        key = "FUEL",
        label = "Bensin & Kendaraan",
        group = "Transportasi",
        keywords = "bensin pertamax bbm parkir tol servis motor mobil cuci mobil",
        defaultWeight = 12,
        icon = Icons.Rounded.LocalGasStation,
    ),
    PocketCategoryOption(
        key = "TRAVEL",
        label = "Liburan & Travel",
        group = "Transportasi",
        keywords = "liburan travel tiket pesawat hotel penginapan wisata jalan-jalan",
        defaultWeight = 10,
        icon = Icons.Rounded.Flight,
    ),

    // Hiburan & Hobi
    PocketCategoryOption(
        key = "CINEMA",
        label = "Bioskop & Film",
        group = "Hiburan",
        keywords = "bioskop xxi cgv cinepolis film nonton tiket popcorn",
        defaultWeight = 8,
        icon = Icons.Rounded.Movie,
    ),
    PocketCategoryOption(
        key = "ENTERTAINMENT",
        label = "Hiburan & Game",
        group = "Hiburan",
        keywords = "hiburan game topup steam playstation nintendo arcade rekreasi",
        defaultWeight = 10,
        icon = Icons.Rounded.SportsEsports,
    ),
    PocketCategoryOption(
        key = "STREAMING",
        label = "Streaming & Langganan",
        group = "Hiburan",
        keywords = "netflix youtube premium disney spotify viu langganan tv",
        defaultWeight = 6,
        icon = Icons.Rounded.LiveTv,
    ),
    PocketCategoryOption(
        key = "MUSIC",
        label = "Musik & Konser",
        group = "Hiburan",
        keywords = "musik konser festival tiket lagu alat musik audio",
        defaultWeight = 6,
        icon = Icons.Rounded.MusicNote,
    ),
    PocketCategoryOption(
        key = "HOBBY",
        label = "Fotografi & Hobi",
        group = "Hiburan",
        keywords = "hobi kamera fotografi koleksi lukis mainan kerajinan",
        defaultWeight = 8,
        icon = Icons.Rounded.CameraAlt,
    ),

    // Kesehatan & Kebugaran
    PocketCategoryOption(
        key = "HEALTH",
        label = "Kesehatan & Klinik",
        group = "Kesehatan",
        keywords = "kesehatan dokter klinik rumah sakit bpjs asuransi cekup gigi",
        defaultWeight = 12,
        icon = Icons.Rounded.Favorite,
    ),
    PocketCategoryOption(
        key = "PHARMACY",
        label = "Apotek & Obat",
        group = "Kesehatan",
        keywords = "apotek obat vitamin suplemen medis p3k resep",
        defaultWeight = 8,
        icon = Icons.Rounded.MedicalServices,
    ),
    PocketCategoryOption(
        key = "FITNESS",
        label = "Olahraga & Gym",
        group = "Kesehatan",
        keywords = "gym fitness olahraga lari renang futsal badminton yoga",
        defaultWeight = 8,
        icon = Icons.Rounded.FitnessCenter,
    ),
    PocketCategoryOption(
        key = "SKINCARE",
        label = "Kecantikan & Perawatan",
        group = "Kesehatan",
        keywords = "skincare salon barber potong rambut kosmetik perawatan diri",
        defaultWeight = 10,
        icon = Icons.Rounded.Spa,
    ),

    // Tagihan & Rumah
    PocketCategoryOption(
        key = "UTILITIES",
        label = "Listrik, Air & Gas",
        group = "Tagihan",
        keywords = "listrik pln token air pdam gas elpiji utilitas rumah",
        defaultWeight = 14,
        icon = Icons.Rounded.Bolt,
    ),
    PocketCategoryOption(
        key = "INTERNET",
        label = "Internet & Pulsa",
        group = "Tagihan",
        keywords = "internet wifi indihome biznet kuota data pulsa telepon",
        defaultWeight = 10,
        icon = Icons.Rounded.Wifi,
    ),
    PocketCategoryOption(
        key = "BILLS",
        label = "Tagihan & Cicilan",
        group = "Tagihan",
        keywords = "tagihan cicilan kartu kredit paylater pajak iuran rutin",
        defaultWeight = 15,
        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
    ),
    PocketCategoryOption(
        key = "HOUSING",
        label = "Kos & Tempat Tinggal",
        group = "Tagihan",
        keywords = "kos kontrakan sewa rumah apartemen ipl properti",
        defaultWeight = 25,
        icon = Icons.Rounded.Home,
    ),
    PocketCategoryOption(
        key = "HOME_CARE",
        label = "Perawatan & Laundry",
        group = "Tagihan",
        keywords = "laundry cuci setrika bersih rumah perabotan deterjen",
        defaultWeight = 8,
        icon = Icons.Rounded.CleaningServices,
    ),

    // Belanja, Edukasi & Kerja
    PocketCategoryOption(
        key = "SHOPPING",
        label = "Belanja & Fashion",
        group = "Belanja",
        keywords = "belanja baju celana sepatu tas fashion marketplace mall",
        defaultWeight = 12,
        icon = Icons.Rounded.ShoppingBag,
    ),
    PocketCategoryOption(
        key = "TECH",
        label = "Gadget & Elektronik",
        group = "Belanja",
        keywords = "hp smartphone laptop charger aksesoris elektronik servis",
        defaultWeight = 10,
        icon = Icons.Rounded.Devices,
    ),
    PocketCategoryOption(
        key = "EDUCATION",
        label = "Pendidikan & Buku",
        group = "Belanja",
        keywords = "pendidikan kuliah sekolah buku kursus les bootcamp seminar",
        defaultWeight = 12,
        icon = Icons.Rounded.School,
    ),
    PocketCategoryOption(
        key = "WORK",
        label = "Kerja & Produktivitas",
        group = "Belanja",
        keywords = "kerja kantor alat tulis coworking software produktivitas",
        defaultWeight = 10,
        icon = Icons.Rounded.Work,
    ),

    // Keluarga, Sosial & Dana
    PocketCategoryOption(
        key = "FAMILY",
        label = "Keluarga & Anak",
        group = "Keluarga",
        keywords = "keluarga orang tua anak bayi susu popok keperluan rumah",
        defaultWeight = 18,
        icon = Icons.Rounded.FamilyRestroom,
    ),
    PocketCategoryOption(
        key = "PET",
        label = "Hewan Peliharaan",
        group = "Keluarga",
        keywords = "kucing anjing peliharaan petshop pakan pasir dokter hewan",
        defaultWeight = 8,
        icon = Icons.Rounded.Pets,
    ),
    PocketCategoryOption(
        key = "GIFT",
        label = "Hadiah & Perayaan",
        group = "Keluarga",
        keywords = "hadiah kado ulang tahun pernikahan kondangan hampers",
        defaultWeight = 6,
        icon = Icons.Rounded.CardGiftcard,
    ),
    PocketCategoryOption(
        key = "CHARITY",
        label = "Sedekah & Donasi",
        group = "Keluarga",
        keywords = "sedekah zakat infak donasi sosial amal bantu sesama",
        defaultWeight = 8,
        icon = Icons.Rounded.VolunteerActivism,
    ),
    PocketCategoryOption(
        key = "SAVINGS",
        label = "Tabungan & Dana Darurat",
        group = "Dana",
        keywords = "tabungan celengan dana darurat simpanan masa depan",
        defaultWeight = 20,
        icon = Icons.Rounded.Savings,
    ),
    PocketCategoryOption(
        key = "INVESTMENT",
        label = "Investasi & Emas",
        group = "Dana",
        keywords = "investasi saham reksadana emas obligasi deposito",
        defaultWeight = 15,
        icon = Icons.AutoMirrored.Rounded.TrendingUp,
    ),
    PocketCategoryOption(
        key = "SALARY",
        label = "Gaji & Pendapatan",
        group = "Pemasukan",
        keywords = "gaji upah honorarium tunjangan thr pendapatan rutin",
        defaultWeight = 0,
        icon = Icons.Rounded.Payments,
    ),
    PocketCategoryOption(
        key = "FREELANCE",
        label = "Bonus & Freelance",
        group = "Pemasukan",
        keywords = "bonus komisi freelance proyek sampingan insentif",
        defaultWeight = 0,
        icon = Icons.Rounded.WorkspacePremium,
    ),
    PocketCategoryOption(
        key = "BUSINESS",
        label = "Penjualan & Usaha",
        group = "Pemasukan",
        keywords = "penjualan omzet usaha dagang toko laba profit",
        defaultWeight = 0,
        icon = Icons.Rounded.Storefront,
    ),
    PocketCategoryOption(
        key = "REFUND",
        label = "Cashback & Refund",
        group = "Pemasukan",
        keywords = "cashback refund pengembalian dana klaim retur",
        defaultWeight = 0,
        icon = Icons.Rounded.Replay,
    ),
    PocketCategoryOption(
        key = "OTHER",
        label = "Lainnya / Umum",
        group = "Lainnya",
        keywords = "lainnya umum cadangan tak terduga serbaguna",
        defaultWeight = 10,
        icon = Icons.Rounded.TrackChanges,
    ),
)

fun pocketCategoryFor(key: String): PocketCategoryOption =
    POCKET_CATEGORY_CATALOG.firstOrNull { it.key == key }
        ?: PocketCategoryOption(
            key = key,
            label = "Lainnya / Umum",
            group = "Lainnya",
            keywords = "",
            defaultWeight = 10,
            icon = Icons.Rounded.AccountBalanceWallet,
        )

@Composable
fun CategorySelectField(
    selectedKey: String,
    onClick: () -> Unit,
    label: String = "Kategori & Ikon Kantong",
    helperText: String? = null,
    testTag: String = "pocket_category_select_field",
    modifier: Modifier = Modifier,
) {
    val bento = MaterialTheme.bentoColors
    val selected = pocketCategoryFor(selectedKey)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = bento.nestedCardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bento.accentTile),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = selected.icon,
                        contentDescription = selected.label,
                        tint = bento.onAccentTile,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = helperText ?: "Grup: ${selected.group} • Ketuk untuk cari kategori",
                        style = MaterialTheme.typography.labelSmall,
                        color = bento.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Pilih Kategori",
                    tint = bento.mutedText,
                )
            }
        }
    }
}

@Composable
fun CategorySearchPickerDialog(
    selectedKey: String?,
    excludedKeys: Set<String> = emptySet(),
    title: String = "Pilih Kategori & Ikon",
    onDismiss: () -> Unit,
    onSelect: (PocketCategoryOption) -> Unit,
) {
    val bento = MaterialTheme.bentoColors
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("Semua") }

    val groups = remember {
        listOf("Semua") + POCKET_CATEGORY_CATALOG.map { it.group }.distinct()
    }

    val filtered = remember(searchQuery, selectedGroup, excludedKeys) {
        val q = searchQuery.trim().lowercase()
        POCKET_CATEGORY_CATALOG.filter { item ->
            val groupOk = selectedGroup == "Semua" || item.group == selectedGroup
            val queryOk = q.isBlank() ||
                item.label.lowercase().contains(q) ||
                item.group.lowercase().contains(q) ||
                item.keywords.lowercase().contains(q)
            val notExcluded = !excludedKeys.contains(item.key) || item.key == selectedKey
            groupOk && queryOk && notExcluded
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 560.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(26.dp),
            color = bento.cardSurface,
            border = BorderStroke(1.dp, bento.borderSubtle),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${POCKET_CATEGORY_CATALOG.size} kategori lengkap tersedia",
                            style = MaterialTheme.typography.labelMedium,
                            color = bento.mutedText,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup",
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari kategori (mis: bioskop, obat, listrik, kopi...)") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_search_input"),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            label = { Text(group) },
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Kategori \"$searchQuery\" tidak ditemukan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = bento.mutedText,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.key }) { option ->
                            val isSelected = option.key == selectedKey
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_option_${option.key.lowercase()}")
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelect(option) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) bento.accentTile.copy(alpha = 0.16f) else bento.nestedCardSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) bento.highlightLabel else bento.borderSubtle.copy(alpha = 0.5f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) bento.accentTile else bento.cardSurface),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = option.label,
                                            tint = if (isSelected) bento.onAccentTile else bento.highlightLabel,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(bento.cardSurface)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                            ) {
                                                Text(
                                                    text = option.group,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = bento.mutedText,
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = option.keywords,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = bento.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Terpilih",
                                            tint = bento.highlightLabel,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
