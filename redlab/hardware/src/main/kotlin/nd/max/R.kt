package nd.max

/**
 * Compatibility seam for MaxManager's generated `R` class.
 *
 * The ported engine only reads string resource ids to *label* privilege levels; nothing inflates a
 * view from them. Keeping the same `R.string.*` names means the ported files stay untouched, and a
 * later move to real resources is a one-file change.
 */
object R {
    object string {
        const val max_privilege_feat_appops: Int = 1
        const val max_privilege_feat_battery_read: Int = 2
        const val max_privilege_feat_cpu: Int = 3
        const val max_privilege_feat_density: Int = 4
        const val max_privilege_feat_device_info: Int = 5
        const val max_privilege_feat_dex2oat: Int = 6
        const val max_privilege_feat_doze: Int = 7
        const val max_privilege_feat_framestats: Int = 8
        const val max_privilege_feat_freeze: Int = 9
        const val max_privilege_feat_logs: Int = 10
        const val max_privilege_feat_module: Int = 11
        const val max_privilege_feat_netpolicy: Int = 12
        const val max_privilege_feat_theme: Int = 13
        const val max_privilege_feat_thermal: Int = 14
        const val max_privilege_feat_zram: Int = 15
        const val max_privilege_level_none: Int = 16
        const val max_privilege_level_root: Int = 17
        const val max_privilege_level_shizuku: Int = 18
        const val max_privilege_tier_none: Int = 19
        const val max_privilege_tier_root: Int = 20
        const val max_privilege_tier_shizuku: Int = 21
    }
}
