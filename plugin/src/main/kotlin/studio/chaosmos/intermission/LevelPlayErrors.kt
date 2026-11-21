package studio.chaosmos.intermission

/**
 * LevelPlay's error codes, as named by the constants in mediation-sdk 9.8.0 (`LevelPlayAdError` and
 * `com.ironsource.mediationsdk.logger.IronSourceError`). Any other code stays [AdError.PROVIDER], with the raw
 * code beside it.
 */
internal object LevelPlayErrors {
    private val table: Map<Int, AdError> = buildMap {
        // ERROR_CODE_INIT_FAILED
        put(508, AdError.INIT_FAILED)
        // ERROR_CODE_NO_ADS_TO_SHOW, BN_LOAD_NO_FILL, RV_LOAD_NO_FILL, IS_LOAD_NO_FILL, and the three
        // LOAD_FAILED_NO_CANDIDATES
        for (code in listOf(509, 606, 1058, 1158, 1024, 1035, 1044)) put(code, AdError.NO_FILL)
        // ERROR_NO_INTERNET_CONNECTION
        put(520, AdError.NO_CONNECTION)
        // PLACEMENT_CAPPED, AD_FORMAT_CAPPED, CAPPED_PER_SESSION, AD_UNIT_CAPPED, BN_LOAD_PLACEMENT_CAPPED,
        // RV and IS ALL_SMASHES_SESSION_CAPPED
        for (code in listOf(524, 525, 526, 530, 604, 1042, 1043)) put(code, AdError.CAPPED)
        // LOAD_BEFORE_INIT_SUCCESS_CALLBACK
        put(625, AdError.NOT_INITIALIZED)
        // NO_AD_UNIT_ID_SPECIFIED, INVALID_AD_UNIT_ID
        for (code in listOf(624, 626)) put(code, AdError.INVALID_ARGUMENT)
        // SHOW_BEFORE_LOAD_SUCCESS_CALLBACK, SHOW_WHILE_LOAD, RV_SHOW_CALLED_WRONG_STATE, and the DO_IS and DO_RV
        // SHOW_NO_AVAILABLE_ADS
        for (code in listOf(628, 631, 1023, 1066, 1069)) put(code, AdError.NOT_LOADED)
        // LOAD_WHILE_SHOW, SHOW_WHILE_SHOW, RV and IS SHOW_CALLED_DURING_SHOW, DO_IS and DO_RV SHOW_DURING_SHOW
        for (code in listOf(629, 630, 1022, 1036, 1064, 1067)) put(code, AdError.ALREADY_SHOWING)
        // BN_INSTANCE_LOAD_TIMEOUT, LOAD_FAILED_TIMEOUT, DO_IS_LOAD_TIMED_OUT, DO_RV_LOAD_TIMED_OUT,
        // AUCTION_ERROR_TIMED_OUT
        for (code in listOf(608, 1025, 1052, 1055, 1006)) put(code, AdError.TIMEOUT)
        // RV_EXPIRED_ADS
        put(1057, AdError.EXPIRED)
        // DO_RV, DO_IS and DO_BN LOAD_MISSING_ACTIVITY
        for (code in listOf(1060, 1061, 1062)) put(code, AdError.NO_ACTIVITY)
    }

    fun map(code: Int): AdError = table[code] ?: AdError.PROVIDER
}
