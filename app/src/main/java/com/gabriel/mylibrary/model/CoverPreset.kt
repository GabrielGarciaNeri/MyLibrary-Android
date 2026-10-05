package com.gabriel.mylibrary.model

import androidx.annotation.StringRes
import com.gabriel.mylibrary.R

enum class CoverPreset(val id: String, @param:StringRes val labelResource: Int) {
    MIDNIGHT_ORBIT("midnight_orbit", R.string.cover_midnight_orbit),
    TERRACOTTA_SUN("terracotta_sun", R.string.cover_terracotta_sun),
    FOREST_PATH("forest_path", R.string.cover_forest_path),
    INDIGO_WAVES("indigo_waves", R.string.cover_indigo_waves),
    ROSE_GEOMETRY("rose_geometry", R.string.cover_rose_geometry),
    GOLDEN_ARCH("golden_arch", R.string.cover_golden_arch),
    TEAL_MOSAIC("teal_mosaic", R.string.cover_teal_mosaic),
    LAVENDER_MOON("lavender_moon", R.string.cover_lavender_moon),
    CORAL_LINES("coral_lines", R.string.cover_coral_lines),
    SAGE_LEAVES("sage_leaves", R.string.cover_sage_leaves),
    BLUE_HORIZON("blue_horizon", R.string.cover_blue_horizon),
    PLUM_STARS("plum_stars", R.string.cover_plum_stars);

    companion object {
        fun fromId(id: String?): CoverPreset? = entries.firstOrNull { it.id == id }
    }
}
