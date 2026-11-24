package com.example.data.wallet

import com.example.data.local.WalletType

enum class WalletLogoMotif {
    CASH_BANKNOTE,
    BRI_STRIPE,
    MANDIRI_RIBBON,
    BNI_ACCENT,
    BTN_ROOF,
    BSI_CRESCENT,
    BCA_WING,
    JAGO_SUN,
    SEABANK_WAVE,
    BLU_DROPLET,
    JENIUS_SPARK,
    NEOBANK_COIN,
    CIMB_DIAMOND,
    PERMATA_GEM,
    HSBC_HEXAGON,
    UOB_BARS,
    CITIBANK_ARC,
    STANCHART_HELIX,
    OCBC_COMPASS,
    GOPAY_WALLET,
    OVO_RING,
    DANA_RIBBON,
    SHOPEEPAY_BAG,
    LINKAJA_BADGE,
    EMONEY_CHIP,
    BANK_ARCH,
    EWALLET_BOLT,
}

data class WalletBrandStyle(
    val badgeText: String,
    val primaryHex: Long,
    val accentHex: Long,
    val onPrimaryHex: Long = 0xFFFFFFFFL,
    val motif: WalletLogoMotif = WalletLogoMotif.BANK_ARCH,
)

data class WalletProviderOption(
    val code: String,
    val shortName: String,
    val companyName: String,
    val walletType: WalletType,
    val categoryGroup: String,
    val keywords: String = "",
) {
    /**
     * Exact preview string format including legal entity + Bank/E-Wallet name:
     * - Banks: "PT. Bank Rakyat Indonesia, Bank: BRI"
     * - E-Wallets: "PT. Dompet Anak Bangsa, E-Wallet: GoPay"
     */
    val selectionPreviewLabel: String
        get() = when (walletType) {
            WalletType.BANK -> "$companyName, Bank: $shortName"
            WalletType.EWALLET -> "$companyName, E-Wallet: $shortName"
            WalletType.CASH -> shortName
        }

    val compactTypeNameLabel: String
        get() = when (walletType) {
            WalletType.BANK -> "Bank: $shortName"
            WalletType.EWALLET -> "E-Wallet: $shortName"
            WalletType.CASH -> "Uang Tunai"
        }

    val defaultWalletName: String
        get() = when (walletType) {
            WalletType.CASH -> "Tunai"
            else -> shortName
        }
}

object IndonesianWalletCatalog {

    val cashOption = WalletProviderOption(
        code = "CASH_IDR",
        shortName = "Tunai / Kas Fisik",
        companyName = "Dompet Uang Tunai",
        walletType = WalletType.CASH,
        categoryGroup = "Tunai",
        keywords = "tunai cash uang kertas dompet fisik saku kas",
    )

    val allEWalletProviders: List<WalletProviderOption> = listOf(
        WalletProviderOption("EW_GOPAY", "GoPay", "PT. Dompet Anak Bangsa", WalletType.EWALLET, "Populer", "gojek goto gopay qris"),
        WalletProviderOption("EW_OVO", "OVO", "PT. Visionet Internasional", WalletType.EWALLET, "Populer", "ovo grab tokped qris"),
        WalletProviderOption("EW_DANA", "DANA", "PT. Espay Debit Indonesia Koe", WalletType.EWALLET, "Populer", "dana qris dompet digital"),
        WalletProviderOption("EW_SHOPEEPAY", "ShopeePay", "PT. Airpay International Indonesia", WalletType.EWALLET, "Populer", "shopee spay qris"),
        WalletProviderOption("EW_LINKAJA", "LinkAja", "PT. Fintek Karya Nusantara", WalletType.EWALLET, "BUMN", "linkaja telkomsel bumn tcash"),
        WalletProviderOption("EW_ASTRA_PAY", "AstraPay", "PT. Astra Digital Arta", WalletType.EWALLET, "Lainnya", "astrapay astra fif maucash"),
        WalletProviderOption("EW_ISAKU", "i.saku", "PT. Inti Dunia Sukses", WalletType.EWALLET, "Ritel", "isaku indomaret"),
        WalletProviderOption("EW_SAKUKU", "Sakuku", "PT. Bank Central Asia Tbk.", WalletType.EWALLET, "Bank", "sakuku bca"),
        WalletProviderOption("EW_KASPRO", "KasPro", "PT. Solusi Pasti Indonesia", WalletType.EWALLET, "Lainnya", "kaspro digiasia"),
        WalletProviderOption("EW_OTTOCASH", "OttoCash", "PT. Transaksi Artha Gemilang", WalletType.EWALLET, "Lainnya", "ottocash ottopay"),
        WalletProviderOption("EW_MOTIONPAY", "MotionPay", "PT. MNC Teknologi Nusantara", WalletType.EWALLET, "Lainnya", "motionpay mnc spin"),
        WalletProviderOption("EW_DOKU", "DOKU Wallet", "PT. Nusa Satu Inti Artha", WalletType.EWALLET, "Lainnya", "doku wallet"),
        WalletProviderOption("EW_TRUEMONEY", "TrueMoney", "PT. Witami Tunai Mandiri", WalletType.EWALLET, "Lainnya", "truemoney alfamart"),
        WalletProviderOption("EW_SPEEDCASH", "SpeedCash", "PT. Bimasakti Multi Sinergi", WalletType.EWALLET, "Lainnya", "speedcash fastpay"),
        WalletProviderOption("EW_VIRUKU", "Virgo", "PT. Capital Net Indonesia", WalletType.EWALLET, "Ritel", "virgo alfamart kembalian"),
        WalletProviderOption("EW_JAKONE", "JakOne Pay", "PT. Bank DKI", WalletType.EWALLET, "Daerah", "jakone bank dki"),
        WalletProviderOption("EW_DIPAY", "Dipay", "PT. Mareco Prima Mandiri", WalletType.EWALLET, "Lainnya", "dipay"),
        WalletProviderOption("EW_FINPAY", "Finpay Money", "PT. Finnet Indonesia", WalletType.EWALLET, "BUMN", "finpay finnet telkom"),
        WalletProviderOption("EW_PAYTREN", "Paytren", "PT. Paytren Aset Manajemen", WalletType.EWALLET, "Lainnya", "paytren"),
        WalletProviderOption("EW_EMONEY", "Mandiri e-Money", "PT. Bank Mandiri (Persero) Tbk.", WalletType.EWALLET, "Kartu Tol", "emoney mandiri tol e-toll"),
        WalletProviderOption("EW_FLAZZ", "Flazz BCA", "PT. Bank Central Asia Tbk.", WalletType.EWALLET, "Kartu Tol", "flazz bca kartu tol"),
        WalletProviderOption("EW_BRIZZI", "BRIZZI", "PT. Bank Rakyat Indonesia", WalletType.EWALLET, "Kartu Tol", "brizzi bri tol"),
        WalletProviderOption("EW_TAPCASH", "TapCash BNI", "PT. Bank Negara Indonesia (Persero) Tbk.", WalletType.EWALLET, "Kartu Tol", "tapcash bni tol"),
        WalletProviderOption("EW_NETZME", "Netzme", "PT. Netzme Kreasi Indonesia", WalletType.EWALLET, "Lainnya", "netzme qris"),
    )

    val allIndonesianBanks: List<WalletProviderOption> = listOf(
        // Bank BUMN (Himbara)
        WalletProviderOption("BANK_BRI", "BRI", "PT. Bank Rakyat Indonesia", WalletType.BANK, "BUMN", "bri brimo himbara bumn"),
        WalletProviderOption("BANK_MANDIRI", "Mandiri", "PT. Bank Mandiri (Persero) Tbk.", WalletType.BANK, "BUMN", "mandiri livin himbara bumn"),
        WalletProviderOption("BANK_BNI", "BNI", "PT. Bank Negara Indonesia (Persero) Tbk.", WalletType.BANK, "BUMN", "bni wondr mobile banking himbara bumn"),
        WalletProviderOption("BANK_BTN", "BTN", "PT. Bank Tabungan Negara (Persero) Tbk.", WalletType.BANK, "BUMN", "btn bale kpr bumn"),
        WalletProviderOption("BANK_BSI", "BSI", "PT. Bank Syariah Indonesia Tbk.", WalletType.BANK, "Syariah", "bsi byond syariah bumn"),

        // Bank Digital Indonesia (termasuk Bank Jago!)
        WalletProviderOption("BANK_JAGO", "Bank Jago", "PT. Bank Jago Tbk.", WalletType.BANK, "Digital", "jago bank jago artos kantong digital"),
        WalletProviderOption("BANK_SEABANK", "SeaBank", "PT. Bank Seabank Indonesia", WalletType.BANK, "Digital", "seabank shopee koin"),
        WalletProviderOption("BANK_BLU", "blu by BCA", "PT. Bank Digital BCA", WalletType.BANK, "Digital", "blu bca digital"),
        WalletProviderOption("BANK_JENIUS", "Jenius", "PT. Bank SMBC Indonesia Tbk.", WalletType.BANK, "Digital", "jenius btpn smbc"),
        WalletProviderOption("BANK_NEOBANK", "Neobank (BNC)", "PT. Bank Neo Commerce Tbk.", WalletType.BANK, "Digital", "neo neobank bnc akulaku"),
        WalletProviderOption("BANK_ALLO", "Allo Bank", "PT. Allo Bank Indonesia Tbk.", WalletType.BANK, "Digital", "allo ct corp"),
        WalletProviderOption("BANK_LINE", "LINE Bank", "PT. Bank KEB Hana Indonesia", WalletType.BANK, "Digital", "line bank hana"),
        WalletProviderOption("BANK_KROM", "Krom Bank", "PT. Krom Bank Indonesia Tbk.", WalletType.BANK, "Digital", "krom kredivo"),
        WalletProviderOption("BANK_SUPERBANK", "Superbank", "PT. Super Bank Indonesia", WalletType.BANK, "Digital", "superbank grab emtek"),
        WalletProviderOption("BANK_HIBANK", "hibank", "PT. Bank Hibank Indonesia", WalletType.BANK, "Digital", "hibank bni mayora"),
        WalletProviderOption("BANK_AMAR", "Bank Amar", "PT. Bank Amar Indonesia Tbk.", WalletType.BANK, "Digital", "amar tunaiku"),
        WalletProviderOption("BANK_RAYA", "Bank Raya", "PT. Bank Raya Indonesia Tbk.", WalletType.BANK, "Digital", "raya bri agro"),
        WalletProviderOption("BANK_ALADIN", "Bank Aladin", "PT. Bank Aladin Syariah Tbk.", WalletType.BANK, "Syariah", "aladin syariah alfamart"),
        WalletProviderOption("BANK_SAQU", "Bank Saqu", "PT. Bank Jasa Jakarta", WalletType.BANK, "Digital", "saqu astra wepay bjj"),

        // Bank Swasta Nasional Utama & Menengah
        WalletProviderOption("BANK_BCA", "BCA", "PT. Bank Central Asia Tbk.", WalletType.BANK, "Swasta", "bca mybca klikbca tahapan"),
        WalletProviderOption("BANK_CIMB", "CIMB Niaga", "PT. Bank CIMB Niaga Tbk.", WalletType.BANK, "Swasta", "cimb niaga octo"),
        WalletProviderOption("BANK_DANAMON", "Danamon", "PT. Bank Danamon Indonesia Tbk.", WalletType.BANK, "Swasta", "danamon d-bank"),
        WalletProviderOption("BANK_PERMATA", "PermataBank", "PT. Bank Permata Tbk.", WalletType.BANK, "Swasta", "permata permata me"),
        WalletProviderOption("BANK_PANIN", "Panin Bank", "PT. Bank Pan Indonesia Tbk.", WalletType.BANK, "Swasta", "panin"),
        WalletProviderOption("BANK_OCBC", "OCBC", "PT. Bank OCBC NISP Tbk.", WalletType.BANK, "Swasta", "ocbc nisp nyala"),
        WalletProviderOption("BANK_MAYBANK", "Maybank", "PT. Bank Maybank Indonesia Tbk.", WalletType.BANK, "Swasta", "maybank m2u bii"),
        WalletProviderOption("BANK_MEGA", "Bank Mega", "PT. Bank Mega Tbk.", WalletType.BANK, "Swasta", "mega m-smile"),
        WalletProviderOption("BANK_SINARMAS", "Sinarmas", "PT. Bank Sinarmas Tbk.", WalletType.BANK, "Swasta", "sinarmas simobi"),
        WalletProviderOption("BANK_UOB", "UOB", "PT. Bank UOB Indonesia", WalletType.BANK, "Swasta", "uob tmrw"),
        WalletProviderOption("BANK_DBS", "DBS", "PT. Bank DBS Indonesia", WalletType.BANK, "Swasta", "dbs digibank"),
        WalletProviderOption("BANK_HSBC", "HSBC", "PT. Bank HSBC Indonesia", WalletType.BANK, "Asing", "hsbc"),
        WalletProviderOption("BANK_BTPN_WOW", "SMBC / BTPN", "PT. Bank SMBC Indonesia Tbk.", WalletType.BANK, "Swasta", "btpn smbc"),
        WalletProviderOption("BANK_MUAMALAT", "Muamalat", "PT. Bank Muamalat Indonesia Tbk.", WalletType.BANK, "Syariah", "muamalat syariah"),
        WalletProviderOption("BANK_BCA_SYARIAH", "BCA Syariah", "PT. Bank BCA Syariah", WalletType.BANK, "Syariah", "bca syariah bsya"),
        WalletProviderOption("BANK_MEGA_SYARIAH", "Mega Syariah", "PT. Bank Mega Syariah", WalletType.BANK, "Syariah", "mega syariah m-syariah"),
        WalletProviderOption("BANK_KB_BUKOPIN", "KB Bank", "PT. Bank KB Bukopin Tbk.", WalletType.BANK, "Swasta", "kb bukopin"),
        WalletProviderOption("BANK_KB_SYARIAH", "KB Syariah", "PT. Bank KB Bukopin Syariah", WalletType.BANK, "Syariah", "kb bukopin syariah"),
        WalletProviderOption("BANK_PANIN_SYARIAH", "Panin Syariah", "PT. Bank Panin Dubai Syariah Tbk.", WalletType.BANK, "Syariah", "panin dubai syariah"),
        WalletProviderOption("BANK_BTPN_SYARIAH", "BTPN Syariah", "PT. Bank BTPN Syariah Tbk.", WalletType.BANK, "Syariah", "btpn syariah tepat"),
        WalletProviderOption("BANK_VICTORIA_SYARIAH", "Victoria Syariah", "PT. Bank Victoria Syariah", WalletType.BANK, "Syariah", "victoria syariah"),
        WalletProviderOption("BANK_BJB_SYARIAH", "bjb syariah", "PT. Bank Jabar Banten Syariah", WalletType.BANK, "Syariah", "bjb syariah"),
        WalletProviderOption("BANK_NANO", "Nanobank", "PT. Bank Nano Syariah", WalletType.BANK, "Syariah", "nanobank sinarmas syariah"),
        WalletProviderOption("BANK_MESTIKA", "Bank Mestika", "PT. Bank Mestika Dharma Tbk.", WalletType.BANK, "Swasta", "mestika"),
        WalletProviderOption("BANK_MASPION", "Bank Maspion", "PT. Bank Maspion Indonesia Tbk.", WalletType.BANK, "Swasta", "maspion kasikorn"),
        WalletProviderOption("BANK_GANESHA", "Bank Ganesha", "PT. Bank Ganesha Tbk.", WalletType.BANK, "Swasta", "ganesha"),
        WalletProviderOption("BANK_INA", "Bank Ina", "PT. Bank Ina Perdana Tbk.", WalletType.BANK, "Swasta", "ina salim"),
        WalletProviderOption("BANK_NOBU", "Bank Nobu", "PT. Bank Nationalnobu Tbk.", WalletType.BANK, "Swasta", "nobu lippo"),
        WalletProviderOption("BANK_CAPITAL", "Bank Capital", "PT. Bank Capital Indonesia Tbk.", WalletType.BANK, "Swasta", "capital"),
        WalletProviderOption("BANK_INDEX", "Bank Index", "PT. Bank Index Selindo", WalletType.BANK, "Swasta", "index selindo"),
        WalletProviderOption("BANK_VICTORIA", "Bank Victoria", "PT. Bank Victoria International Tbk.", WalletType.BANK, "Swasta", "victoria"),
        WalletProviderOption("BANK_SAHABAT_SAMPOERNA", "Bank Sampoerna", "PT. Bank Sahabat Sampoerna", WalletType.BANK, "Swasta", "sampoerna sahabat"),
        WalletProviderOption("BANK_MAYAPADA", "Bank Mayapada", "PT. Bank Mayapada International Tbk.", WalletType.BANK, "Swasta", "mayapada"),
        WalletProviderOption("BANK_ARTHA_GRAHA", "Artha Graha", "PT. Bank Artha Graha Internasional Tbk.", WalletType.BANK, "Swasta", "artha graha"),
        WalletProviderOption("BANK_BUMI_ARTA", "Bumi Arta", "PT. Bank Bumi Arta Tbk.", WalletType.BANK, "Swasta", "bumi arta"),
        WalletProviderOption("BANK_BWS", "Woori Saudara", "PT. Bank Woori Saudara Indonesia 1906 Tbk.", WalletType.BANK, "Swasta", "woori saudara bws"),
        WalletProviderOption("BANK_MNC", "MotionBank", "PT. Bank MNC Internasional Tbk.", WalletType.BANK, "Swasta", "mnc motionbank"),
        WalletProviderOption("BANK_MAYORA_HIBANK", "Bank Oke", "PT. Bank Oke Indonesia Tbk.", WalletType.BANK, "Swasta", "oke bank ok"),
        WalletProviderOption("BANK_SHINHAN", "Shinhan Bank", "PT. Bank Shinhan Indonesia", WalletType.BANK, "Asing", "shinhan"),
        WalletProviderOption("BANK_IBK", "Bank IBK", "PT. Bank IBK Indonesia Tbk.", WalletType.BANK, "Asing", "ibk agris"),
        WalletProviderOption("BANK_CCB", "CCB Indonesia", "PT. Bank China Construction Bank Indonesia Tbk.", WalletType.BANK, "Asing", "ccb windu"),
        WalletProviderOption("BANK_ICBC", "ICBC", "PT. Bank ICBC Indonesia", WalletType.BANK, "Asing", "icbc"),
        WalletProviderOption("BANK_BOC", "Bank of China", "Bank of China (Hong Kong) Cabang Jakarta", WalletType.BANK, "Asing", "bank of china"),
        WalletProviderOption("BANK_MIZUHO", "Mizuho", "PT. Bank Mizuho Indonesia", WalletType.BANK, "Asing", "mizuho"),
        WalletProviderOption("BANK_RESONA", "Resona Perdania", "PT. Bank Resona Perdania", WalletType.BANK, "Asing", "resona perdania"),
        WalletProviderOption("BANK_QNB", "QNB Indonesia", "PT. Bank QNB Indonesia Tbk.", WalletType.BANK, "Asing", "qnb kesawan"),
        WalletProviderOption("BANK_CTBC", "CTBC", "PT. Bank CTBC Indonesia", WalletType.BANK, "Asing", "ctbc"),
        WalletProviderOption("BANK_SBI", "SBI Indonesia", "PT. Bank SBI Indonesia", WalletType.BANK, "Asing", "sbi state bank india"),
        WalletProviderOption("BANK_ANZ", "ANZ Indonesia", "PT. Bank ANZ Indonesia", WalletType.BANK, "Asing", "anz"),
        WalletProviderOption("BANK_BNP", "BNP Paribas", "PT. Bank BNP Paribas Indonesia", WalletType.BANK, "Asing", "bnp paribas"),
        WalletProviderOption("BANK_STANCHART", "StanChart", "Standard Chartered Bank Indonesia", WalletType.BANK, "Asing", "standard chartered scb"),
        WalletProviderOption("BANK_CITIBANK", "Citibank", "Citibank N.A. Indonesia", WalletType.BANK, "Asing", "citi citibank"),
        WalletProviderOption("BANK_JPMORGAN", "JPMorgan", "JPMorgan Chase Bank, N.A.", WalletType.BANK, "Asing", "jpmorgan chase"),
        WalletProviderOption("BANK_MUFG", "MUFG Bank", "MUFG Bank, Ltd.", WalletType.BANK, "Asing", "mufg tokyo mitsubishi"),
        WalletProviderOption("BANK_BANK_OF_AMERICA", "Bank of America", "Bank of America, N.A.", WalletType.BANK, "Asing", "boa bank of america"),
        WalletProviderOption("BANK_DEUTSCHE", "Deutsche Bank", "Deutsche Bank AG", WalletType.BANK, "Asing", "deutsche bank"),
        WalletProviderOption("BANK_MAS", "Bank MAS", "PT. Bank Multiarta Sentosa Tbk.", WalletType.BANK, "Swasta", "mas multiarta"),
        WalletProviderOption("BANK_MANTAP", "Mandiri Taspen", "PT. Bank Mandiri Taspen", WalletType.BANK, "Swasta", "mantap mandiri taspen"),
        WalletProviderOption("BANK_FAMA", "Superbank", "PT. Super Bank Indonesia", WalletType.BANK, "Digital", "fama"),

        // Bank Pembangunan Daerah (BPD) Seluruh Indonesia
        WalletProviderOption("BPD_DKI", "Bank DKI", "PT. Bank DKI", WalletType.BANK, "BPD", "dki jakarta jakone"),
        WalletProviderOption("BPD_BJB", "bank bjb", "PT. Bank BJB Tbk.", WalletType.BANK, "BPD", "bjb jabar banten"),
        WalletProviderOption("BPD_JATENG", "Bank Jateng", "PT. Bank Jateng", WalletType.BANK, "BPD", "jateng jawa tengah"),
        WalletProviderOption("BPD_JATIM", "Bank Jatim", "PT. Bank Jatim Tbk.", WalletType.BANK, "BPD", "jatim jawa timur"),
        WalletProviderOption("BPD_DIY", "BPD DIY", "PT. Bank BPD DIY", WalletType.BANK, "BPD", "diy yogyakarta jogja"),
        WalletProviderOption("BPD_BANTEN", "Bank Banten", "PT. Bank Banten Tbk.", WalletType.BANK, "BPD", "banten"),
        WalletProviderOption("BPD_BALI", "BPD Bali", "PT. Bank BPD Bali", WalletType.BANK, "BPD", "bali"),
        WalletProviderOption("BPD_ACEH", "Aceh Syariah", "PT. Bank Aceh Syariah", WalletType.BANK, "BPD", "aceh syariah"),
        WalletProviderOption("BPD_SUMUT", "Bank Sumut", "PT. Bank Sumut", WalletType.BANK, "BPD", "sumut medan"),
        WalletProviderOption("BPD_NAGARI", "Bank Nagari", "PT. Bank Nagari (Sumbar)", WalletType.BANK, "BPD", "nagari sumbar padang"),
        WalletProviderOption("BPD_RIAU_KEPRI", "BRK Syariah", "PT. Bank Riau Kepri Syariah", WalletType.BANK, "BPD", "riau kepri brk syariah"),
        WalletProviderOption("BPD_JAMBI", "Bank Jambi", "PT. Bank Jambi", WalletType.BANK, "BPD", "jambi"),
        WalletProviderOption("BPD_SUMSEL_BABEL", "Sumsel Babel", "PT. Bank Sumsel Babel", WalletType.BANK, "BPD", "sumsel babel palembang"),
        WalletProviderOption("BPD_BENGKULU", "Bank Bengkulu", "PT. Bank Bengkulu", WalletType.BANK, "BPD", "bengkulu"),
        WalletProviderOption("BPD_LAMPUNG", "Bank Lampung", "PT. Bank Lampung", WalletType.BANK, "BPD", "lampung"),
        WalletProviderOption("BPD_KALBAR", "Bank Kalbar", "PT. Bank Kalbar", WalletType.BANK, "BPD", "kalbar pontianak"),
        WalletProviderOption("BPD_KALTENG", "Bank Kalteng", "PT. Bank Kalteng", WalletType.BANK, "BPD", "kalteng palangkaraya"),
        WalletProviderOption("BPD_KALSEL", "Bank Kalsel", "PT. Bank Kalsel", WalletType.BANK, "BPD", "kalsel banjarmasin"),
        WalletProviderOption("BPD_KALTIMTARA", "Bankaltimtara", "PT. Bankaltimtara", WalletType.BANK, "BPD", "kaltimtara samarinda"),
        WalletProviderOption("BPD_SULSELBAR", "Bank Sulselbar", "PT. Bank Sulselbar", WalletType.BANK, "BPD", "sulselbar makassar"),
        WalletProviderOption("BPD_SULUTGO", "Bank BSG", "PT. Bank SulutGo", WalletType.BANK, "BPD", "sulutgo bsg manado gorontalo"),
        WalletProviderOption("BPD_SULTENG", "Bank Sulteng", "PT. Bank Sulteng", WalletType.BANK, "BPD", "sulteng palu"),
        WalletProviderOption("BPD_SULTRA", "Bank Sultra", "PT. Bank Sultra", WalletType.BANK, "BPD", "sultra kendari"),
        WalletProviderOption("BPD_NTB", "NTB Syariah", "PT. Bank NTB Syariah", WalletType.BANK, "BPD", "ntb syariah lombok mataram"),
        WalletProviderOption("BPD_NTT", "Bank NTT", "PT. Bank NTT", WalletType.BANK, "BPD", "ntt kupang"),
        WalletProviderOption("BPD_MALUKU", "Maluku Malut", "PT. Bank Maluku Malut", WalletType.BANK, "BPD", "maluku malut ambon"),
        WalletProviderOption("BPD_PAPUA", "Bank Papua", "PT. Bank Papua", WalletType.BANK, "BPD", "papua jayapura"),
    )

    private val brandStyleCatalog = mapOf(
        // Cash
        "CASH_IDR" to WalletBrandStyle("TUNAI", 0xFF15803DL, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.CASH_BANKNOTE),

        // E-Wallets
        "EW_GOPAY" to WalletBrandStyle("GoPay", 0xFF00AED6L, 0xFF0077B6L, 0xFFFFFFFFL, WalletLogoMotif.GOPAY_WALLET),
        "EW_OVO" to WalletBrandStyle("OVO", 0xFF4C3494L, 0xFFA855F7L, 0xFFFFFFFFL, WalletLogoMotif.OVO_RING),
        "EW_DANA" to WalletBrandStyle("DANA", 0xFF118EE9L, 0xFF38BDF8L, 0xFFFFFFFFL, WalletLogoMotif.DANA_RIBBON),
        "EW_SHOPEEPAY" to WalletBrandStyle("SPay", 0xFFEE4D2DL, 0xFFFF8C69L, 0xFFFFFFFFL, WalletLogoMotif.SHOPEEPAY_BAG),
        "EW_LINKAJA" to WalletBrandStyle("Link!", 0xFFE31E25L, 0xFFFCA5A5L, 0xFFFFFFFFL, WalletLogoMotif.LINKAJA_BADGE),
        "EW_ASTRA_PAY" to WalletBrandStyle("Astra", 0xFF005BACL, 0xFF38BDF8L, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_ISAKU" to WalletBrandStyle("i.saku", 0xFFD32F2FL, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_SAKUKU" to WalletBrandStyle("Saku", 0xFF0060AFL, 0xFF22D3EEL, 0xFFFFFFFFL, WalletLogoMotif.BCA_WING),
        "EW_KASPRO" to WalletBrandStyle("KasPro", 0xFF1D4ED8L, 0xFF60A5FAL, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_OTTOCASH" to WalletBrandStyle("Otto", 0xFFEA580CL, 0xFFFDE047L, 0xFFFFFFFFL, WalletLogoMotif.OVO_RING),
        "EW_MOTIONPAY" to WalletBrandStyle("Motion", 0xFF1E3A8AL, 0xFFEF4444L, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_DOKU" to WalletBrandStyle("DOKU", 0xFFD91E2AL, 0xFFF87171L, 0xFFFFFFFFL, WalletLogoMotif.LINKAJA_BADGE),
        "EW_TRUEMONEY" to WalletBrandStyle("True", 0xFFEA580CL, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_SPEEDCASH" to WalletBrandStyle("Speed", 0xFF0F766EL, 0xFF2DD4BFL, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_VIRUKU" to WalletBrandStyle("Virgo", 0xFF7C3AEDL, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.OVO_RING),
        "EW_JAKONE" to WalletBrandStyle("JakOne", 0xFFE64A19L, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.DANA_RIBBON),
        "EW_DIPAY" to WalletBrandStyle("Dipay", 0xFF0284C7L, 0xFF7DD3FCL, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_FINPAY" to WalletBrandStyle("Finpay", 0xFFDC2626L, 0xFF1E293BL, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_PAYTREN" to WalletBrandStyle("PayTn", 0xFF0D9488L, 0xFF5EEAD4L, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),
        "EW_EMONEY" to WalletBrandStyle("e-Mny", 0xFF003D79L, 0xFFFFB81CL, 0xFFFFFFFFL, WalletLogoMotif.EMONEY_CHIP),
        "EW_FLAZZ" to WalletBrandStyle("Flazz", 0xFF0060AFL, 0xFF38BDF8L, 0xFFFFFFFFL, WalletLogoMotif.EMONEY_CHIP),
        "EW_BRIZZI" to WalletBrandStyle("BRIZZI", 0xFF00529CL, 0xFFF37021L, 0xFFFFFFFFL, WalletLogoMotif.EMONEY_CHIP),
        "EW_TAPCASH" to WalletBrandStyle("TapCsh", 0xFF005E6AL, 0xFFF15A23L, 0xFFFFFFFFL, WalletLogoMotif.EMONEY_CHIP),
        "EW_NETZME" to WalletBrandStyle("Netz", 0xFF4338CAL, 0xFF34D399L, 0xFFFFFFFFL, WalletLogoMotif.EWALLET_BOLT),

        // Himbara & Syariah
        "BANK_BRI" to WalletBrandStyle("BRI", 0xFF00529CL, 0xFFF37021L, 0xFFFFFFFFL, WalletLogoMotif.BRI_STRIPE),
        "BANK_MANDIRI" to WalletBrandStyle("Mandiri", 0xFF003D79L, 0xFFFFB81CL, 0xFFFFFFFFL, WalletLogoMotif.MANDIRI_RIBBON),
        "BANK_BNI" to WalletBrandStyle("BNI", 0xFF005E6AL, 0xFFF15A23L, 0xFFFFFFFFL, WalletLogoMotif.BNI_ACCENT),
        "BANK_BTN" to WalletBrandStyle("BTN", 0xFF005BAAL, 0xFFFDB813L, 0xFFFFFFFFL, WalletLogoMotif.BTN_ROOF),
        "BANK_BSI" to WalletBrandStyle("BSI", 0xFF00A39DL, 0xFFF7A823L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),

        // Digital Banks
        "BANK_JAGO" to WalletBrandStyle("JAGO", 0xFFFDC800L, 0xFF2D1B4EL, 0xFF1F1638L, WalletLogoMotif.JAGO_SUN),
        "BANK_SEABANK" to WalletBrandStyle("Sea", 0xFFEE4D2DL, 0xFFFF9800L, 0xFFFFFFFFL, WalletLogoMotif.SEABANK_WAVE),
        "BANK_BLU" to WalletBrandStyle("blu", 0xFF00A8E8L, 0xFF7DD3FCL, 0xFFFFFFFFL, WalletLogoMotif.BLU_DROPLET),
        "BANK_JENIUS" to WalletBrandStyle("Jenius", 0xFF009FE3L, 0xFFF43F5EL, 0xFFFFFFFFL, WalletLogoMotif.JENIUS_SPARK),
        "BANK_NEOBANK" to WalletBrandStyle("neo", 0xFFFFD600L, 0xFF1E293BL, 0xFF111827L, WalletLogoMotif.NEOBANK_COIN),
        "BANK_ALLO" to WalletBrandStyle("allo", 0xFF6E2594L, 0xFFEC4899L, 0xFFFFFFFFL, WalletLogoMotif.OVO_RING),
        "BANK_LINE" to WalletBrandStyle("LINE", 0xFF06C755L, 0xFFBBF7D0L, 0xFFFFFFFFL, WalletLogoMotif.GOPAY_WALLET),
        "BANK_KROM" to WalletBrandStyle("Krom", 0xFF2E3192L, 0xFF22D3EEL, 0xFFFFFFFFL, WalletLogoMotif.JENIUS_SPARK),
        "BANK_SUPERBANK" to WalletBrandStyle("Super", 0xFFB8E600L, 0xFF14532DL, 0xFF14532DL, WalletLogoMotif.NEOBANK_COIN),
        "BANK_HIBANK" to WalletBrandStyle("hi!", 0xFF5E35B1L, 0xFFFF7043L, 0xFFFFFFFFL, WalletLogoMotif.JENIUS_SPARK),
        "BANK_AMAR" to WalletBrandStyle("Amar", 0xFF00838FL, 0xFF4DD0E1L, 0xFFFFFFFFL, WalletLogoMotif.BLU_DROPLET),
        "BANK_RAYA" to WalletBrandStyle("Raya", 0xFF0054A6L, 0xFFA3E635L, 0xFFFFFFFFL, WalletLogoMotif.BRI_STRIPE),
        "BANK_ALADIN" to WalletBrandStyle("Aladin", 0xFF00897BL, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BANK_SAQU" to WalletBrandStyle("Saqu", 0xFFFF6D00L, 0xFFFFD180L, 0xFFFFFFFFL, WalletLogoMotif.SEABANK_WAVE),

        // Swasta & Asing
        "BANK_BCA" to WalletBrandStyle("BCA", 0xFF0060AFL, 0xFFD4AF37L, 0xFFFFFFFFL, WalletLogoMotif.BCA_WING),
        "BANK_CIMB" to WalletBrandStyle("CIMB", 0xFF790008L, 0xFFEC1C24L, 0xFFFFFFFFL, WalletLogoMotif.CIMB_DIAMOND),
        "BANK_DANAMON" to WalletBrandStyle("Dnm", 0xFFF37021L, 0xFF0F2C59L, 0xFFFFFFFFL, WalletLogoMotif.BRI_STRIPE),
        "BANK_PERMATA" to WalletBrandStyle("Prmt", 0xFF4C9A2AL, 0xFF0F3D5EL, 0xFFFFFFFFL, WalletLogoMotif.PERMATA_GEM),
        "BANK_PANIN" to WalletBrandStyle("Panin", 0xFF003366L, 0xFFDC2626L, 0xFFFFFFFFL, WalletLogoMotif.MANDIRI_RIBBON),
        "BANK_OCBC" to WalletBrandStyle("OCBC", 0xFFDA291CL, 0xFFFCA5A5L, 0xFFFFFFFFL, WalletLogoMotif.OCBC_COMPASS),
        "BANK_MAYBANK" to WalletBrandStyle("Mybk", 0xFFFFC72CL, 0xFF18181BL, 0xFF18181BL, WalletLogoMotif.BRI_STRIPE),
        "BANK_MEGA" to WalletBrandStyle("Mega", 0xFFF58220L, 0xFFFDE047L, 0xFFFFFFFFL, WalletLogoMotif.MANDIRI_RIBBON),
        "BANK_SINARMAS" to WalletBrandStyle("Sinar", 0xFFD32F2FL, 0xFFFCA5A5L, 0xFFFFFFFFL, WalletLogoMotif.CIMB_DIAMOND),
        "BANK_UOB" to WalletBrandStyle("UOB", 0xFF0B3B84L, 0xFFE11D48L, 0xFFFFFFFFL, WalletLogoMotif.UOB_BARS),
        "BANK_DBS" to WalletBrandStyle("DBS", 0xFFE60012L, 0xFF111827L, 0xFFFFFFFFL, WalletLogoMotif.PERMATA_GEM),
        "BANK_HSBC" to WalletBrandStyle("HSBC", 0xFFDB0011L, 0xFFFFFFFFL, 0xFFFFFFFFL, WalletLogoMotif.HSBC_HEXAGON),
        "BANK_BTPN_WOW" to WalletBrandStyle("SMBC", 0xFF004B36L, 0xFFA3E635L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BANK_MUAMALAT" to WalletBrandStyle("Mmlt", 0xFF5C2D91L, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BANK_BCA_SYARIAH" to WalletBrandStyle("BCAS", 0xFF007A87L, 0xFFD4AF37L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BANK_MEGA_SYARIAH" to WalletBrandStyle("MegaS", 0xFF059669L, 0xFFF58220L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BANK_KB_BUKOPIN" to WalletBrandStyle("KB", 0xFFFFBC00L, 0xFF5D4037L, 0xFF3E2723L, WalletLogoMotif.JENIUS_SPARK),
        "BANK_KB_SYARIAH" to WalletBrandStyle("KBS", 0xFF047857L, 0xFFFFBC00L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BANK_PANIN_SYARIAH" to WalletBrandStyle("PDS", 0xFF005B4FL, 0xFFDC2626L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BANK_BTPN_SYARIAH" to WalletBrandStyle("BTPNS", 0xFFEA580CL, 0xFF15803DL, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BANK_SHINHAN" to WalletBrandStyle("Shinh", 0xFF0046FFL, 0xFF93C5FDL, 0xFFFFFFFFL, WalletLogoMotif.OCBC_COMPASS),
        "BANK_CITIBANK" to WalletBrandStyle("citi", 0xFF003B70L, 0xFFEE1C25L, 0xFFFFFFFFL, WalletLogoMotif.CITIBANK_ARC),
        "BANK_STANCHART" to WalletBrandStyle("SCB", 0xFF0072AAL, 0xFF21AA47L, 0xFFFFFFFFL, WalletLogoMotif.STANCHART_HELIX),
        "BANK_JPMORGAN" to WalletBrandStyle("JPM", 0xFF3E2723L, 0xFFD7CCC8L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BANK_MUFG" to WalletBrandStyle("MUFG", 0xFFDA291CL, 0xFF9CA3AFL, 0xFFFFFFFFL, WalletLogoMotif.OCBC_COMPASS),

        // BPDs
        "BPD_DKI" to WalletBrandStyle("DKI", 0xFFB91C1CL, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.BTN_ROOF),
        "BPD_BJB" to WalletBrandStyle("bjb", 0xFF0072BCL, 0xFF38BDF8L, 0xFFFFFFFFL, WalletLogoMotif.MANDIRI_RIBBON),
        "BPD_JATENG" to WalletBrandStyle("Jtg", 0xFF1D4ED8L, 0xFFEF4444L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_JATIM" to WalletBrandStyle("Jtm", 0xFFDC2626L, 0xFFFDE047L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_DIY" to WalletBrandStyle("DIY", 0xFF0369A1L, 0xFFF59E0BL, 0xFFFFFFFFL, WalletLogoMotif.BTN_ROOF),
        "BPD_BANTEN" to WalletBrandStyle("Bntn", 0xFF0F766EL, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_BALI" to WalletBrandStyle("Bali", 0xFFC2410CL, 0xFFFBBF24L, 0xFFFFFFFFL, WalletLogoMotif.BTN_ROOF),
        "BPD_ACEH" to WalletBrandStyle("Aceh", 0xFF047857L, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BPD_SUMUT" to WalletBrandStyle("Sumut", 0xFF1E40AFL, 0xFFF97316L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_NAGARI" to WalletBrandStyle("Ngri", 0xFF1D4ED8L, 0xFFEF4444L, 0xFFFFFFFFL, WalletLogoMotif.BTN_ROOF),
        "BPD_RIAU_KEPRI" to WalletBrandStyle("BRK", 0xFF15803DL, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BPD_JAMBI" to WalletBrandStyle("Jmb", 0xFF0284C7L, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_SUMSEL_BABEL" to WalletBrandStyle("BSB", 0xFF0369A1L, 0xFFF97316L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_KALBAR" to WalletBrandStyle("Klbr", 0xFF047857L, 0xFF38BDF8L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_KALTIMTARA" to WalletBrandStyle("Kltm", 0xFF0D9488L, 0xFFFACC15L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_SULSELBAR" to WalletBrandStyle("Ssb", 0xFF0284C7L, 0xFFEF4444L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_SULUTGO" to WalletBrandStyle("BSG", 0xFFDC2626L, 0xFFFBBF24L, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
        "BPD_NTB" to WalletBrandStyle("NTBS", 0xFF059669L, 0xFFF59E0BL, 0xFFFFFFFFL, WalletLogoMotif.BSI_CRESCENT),
        "BPD_PAPUA" to WalletBrandStyle("Papua", 0xFF1D4ED8L, 0xFFF59E0BL, 0xFFFFFFFFL, WalletLogoMotif.BANK_ARCH),
    )

    private val bpdPalettePool = listOf(
        0xFF1E3A8AL to 0xFFF59E0BL,
        0xFF0F766EL to 0xFFFDE047L,
        0xFF991B1BL to 0xFFFBBF24L,
        0xFF1D4ED8L to 0xFF38BDF8L,
    )

    fun brandStyleFor(
        providerCode: String,
        walletType: WalletType,
        shortName: String = "",
    ): WalletBrandStyle {
        brandStyleCatalog[providerCode]?.let { return it }

        if (walletType == WalletType.CASH) {
            return brandStyleCatalog["CASH_IDR"]!!
        }

        val resolvedShort = shortName.ifBlank {
            findByCode(providerCode)?.shortName ?: "DOM"
        }
        val compactCode = resolvedShort
            .replace("Bank ", "", ignoreCase = true)
            .replace("PT. ", "", ignoreCase = true)
            .trim()
            .let { cleaned ->
                if (cleaned.length <= 5) cleaned
                else cleaned.split(" ").filter { it.isNotBlank() }.let { words ->
                    if (words.size >= 2) words.take(3).joinToString("") { it.first().uppercaseChar().toString() }
                    else cleaned.take(4).uppercase()
                }
            }

        val hashIdx = kotlin.math.abs(providerCode.hashCode()) % bpdPalettePool.size
        val (prim, acc) = bpdPalettePool[hashIdx]
        val motif = when (walletType) {
            WalletType.EWALLET -> WalletLogoMotif.EWALLET_BOLT
            WalletType.CASH -> WalletLogoMotif.CASH_BANKNOTE
            WalletType.BANK -> if (resolvedShort.contains("syariah", ignoreCase = true)) {
                WalletLogoMotif.BSI_CRESCENT
            } else {
                WalletLogoMotif.BANK_ARCH
            }
        }
        return WalletBrandStyle(
            badgeText = compactCode,
            primaryHex = prim,
            accentHex = acc,
            onPrimaryHex = 0xFFFFFFFFL,
            motif = motif,
        )
    }

    fun providersFor(type: WalletType): List<WalletProviderOption> = when (type) {
        WalletType.CASH -> listOf(cashOption)
        WalletType.EWALLET -> allEWalletProviders
        WalletType.BANK -> allIndonesianBanks.distinctBy { it.code }.filterNot { it.code == "BANK_FAMA" }
    }

    fun findByCode(code: String): WalletProviderOption? =
        (allIndonesianBanks + allEWalletProviders + cashOption).firstOrNull { it.code == code }
}
