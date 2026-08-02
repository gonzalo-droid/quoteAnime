package com.gondroid.quoteanime.presentation.routine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Countertops
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Iron
import androidx.compose.material.icons.filled.Kayaking
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.gondroid.quoteanime.R

/** One group of related icons shown together in the full-screen picker. */
data class HabitIconCategory(val titleRes: Int, val keys: List<String>)

/** Icons are stored as stable keys so the domain never depends on Compose. */
object HabitIcons {

    /** Grouping shown by the picker (see [com.gondroid.quoteanime.presentation.routine.HabitIconPickerScreen]).
     *  The first 4 categories and their keys are the original library — never remove or rename
     *  any of those keys, [com.gondroid.quoteanime.domain.model.Habit.iconKey] persists them in Room. */
    val CATEGORIES: List<HabitIconCategory> = listOf(
        HabitIconCategory(
            R.string.icon_category_physical,
            listOf("dumbbell", "running", "directions_walk", "cycling", "swimming", "self_improvement")
        ),
        HabitIconCategory(
            R.string.icon_category_mind,
            listOf("book", "headphones", "bedtime", "wb_sunny", "nights_stay", "water_drop")
        ),
        HabitIconCategory(
            R.string.icon_category_study,
            listOf("school", "edit_note", "laptop", "notebook", "folder", "alarm")
        ),
        HabitIconCategory(
            R.string.icon_category_routine,
            listOf("restaurant", "clean_hands", "cleaning", "bed", "spa", "pets")
        ),
        HabitIconCategory(
            R.string.icon_category_nutrition,
            listOf(
                "local_dining", "breakfast_dining", "lunch_dining", "dinner_dining", "icecream",
                "local_cafe", "egg", "monitor_weight", "medication", "health_and_safety",
                "vaccines", "bloodtype"
            )
        ),
        HabitIconCategory(
            R.string.icon_category_home,
            listOf(
                "local_laundry", "iron", "recycling", "delete_sweep", "shopping_cart",
                "checkroom", "plumbing", "handyman", "chair", "countertops", "kitchen",
                "home_repair_service"
            )
        ),
        HabitIconCategory(
            R.string.icon_category_finance,
            listOf(
                "savings", "account_balance_wallet", "attach_money", "receipt_long",
                "trending_up", "task_alt", "checklist", "assignment_turned_in",
                "calendar_month", "timer", "schedule", "bar_chart"
            )
        ),
        HabitIconCategory(
            R.string.icon_category_creativity,
            listOf(
                "palette", "brush", "music_note", "piano", "camera_alt", "videocam", "mic",
                "extension", "sports_esports", "casino", "design_services", "podcasts"
            )
        ),
        HabitIconCategory(
            R.string.icon_category_social,
            listOf(
                "groups", "people", "favorite", "call", "chat", "celebration", "card_giftcard",
                "handshake", "volunteer_activism", "family_restroom"
            )
        ),
        HabitIconCategory(
            R.string.icon_category_nature,
            listOf(
                "park", "hiking", "terrain", "beach_access", "forest", "grass",
                "local_florist", "waves", "sailing", "kayaking", "wb_twilight", "landscape"
            )
        ),
        HabitIconCategory(
            R.string.icon_category_technology,
            listOf(
                "smartphone", "computer", "tv", "headset", "keyboard", "notifications_off",
                "do_not_disturb", "tablet", "desktop_windows", "videogame_asset"
            )
        ),
        HabitIconCategory(
            R.string.icon_category_selfcare,
            listOf(
                "bathtub", "hot_tub", "mood", "sentiment_very_satisfied", "nightlight_round",
                "psychology", "face", "emoji_emotions", "weekend", "auto_awesome"
            )
        ),
        HabitIconCategory(
            R.string.icon_category_sports,
            listOf(
                "sports_basketball", "sports_soccer", "sports_tennis", "sports_golf",
                "sports_volleyball", "sports_martial_arts", "skateboarding", "surfing",
                "local_fire_department", "emoji_events", "military_tech", "flag"
            )
        )
    )

    private val BY_KEY: Map<String, ImageVector> = mapOf(
        // Actividad física
        "dumbbell" to Icons.Filled.FitnessCenter,
        "running" to Icons.AutoMirrored.Filled.DirectionsRun,
        "directions_walk" to Icons.Filled.DirectionsWalk,
        "cycling" to Icons.AutoMirrored.Filled.DirectionsBike,
        "swimming" to Icons.Filled.Pool,
        "self_improvement" to Icons.Filled.SelfImprovement,
        // Mente y bienestar
        "book" to Icons.Filled.MenuBook,
        "headphones" to Icons.Filled.Headphones,
        "bedtime" to Icons.Filled.Bedtime,
        "wb_sunny" to Icons.Filled.WbSunny,
        "nights_stay" to Icons.Filled.NightsStay,
        "water_drop" to Icons.Filled.WaterDrop,
        // Estudio y trabajo
        "school" to Icons.Filled.School,
        "edit_note" to Icons.Filled.EditNote,
        "laptop" to Icons.Filled.Laptop,
        "notebook" to Icons.Filled.Description,
        "folder" to Icons.Filled.Folder,
        "alarm" to Icons.Filled.Alarm,
        // Rutina diaria
        "restaurant" to Icons.Filled.Restaurant,
        "clean_hands" to Icons.Filled.CleanHands,
        "cleaning" to Icons.Filled.CleaningServices,
        "bed" to Icons.Filled.Bed,
        "spa" to Icons.Filled.Spa,
        "pets" to Icons.Filled.Pets,
        // Nutrición y salud
        "local_dining" to Icons.Filled.LocalDining,
        "breakfast_dining" to Icons.Filled.BreakfastDining,
        "lunch_dining" to Icons.Filled.LunchDining,
        "dinner_dining" to Icons.Filled.DinnerDining,
        "icecream" to Icons.Filled.Icecream,
        "local_cafe" to Icons.Filled.LocalCafe,
        "egg" to Icons.Filled.Egg,
        "monitor_weight" to Icons.Filled.MonitorWeight,
        "medication" to Icons.Filled.Medication,
        "health_and_safety" to Icons.Filled.HealthAndSafety,
        "vaccines" to Icons.Filled.Vaccines,
        "bloodtype" to Icons.Filled.Bloodtype,
        // Hogar y tareas
        "local_laundry" to Icons.Filled.LocalLaundryService,
        "iron" to Icons.Filled.Iron,
        "recycling" to Icons.Filled.Recycling,
        "delete_sweep" to Icons.Filled.DeleteSweep,
        "shopping_cart" to Icons.Filled.ShoppingCart,
        "checkroom" to Icons.Filled.Checkroom,
        "plumbing" to Icons.Filled.Plumbing,
        "handyman" to Icons.Filled.Handyman,
        "chair" to Icons.Filled.Chair,
        "countertops" to Icons.Filled.Countertops,
        "kitchen" to Icons.Filled.Kitchen,
        "home_repair_service" to Icons.Filled.HomeRepairService,
        // Finanzas y productividad
        "savings" to Icons.Filled.Savings,
        "account_balance_wallet" to Icons.Filled.AccountBalanceWallet,
        "attach_money" to Icons.Filled.AttachMoney,
        "receipt_long" to Icons.AutoMirrored.Filled.ReceiptLong,
        "trending_up" to Icons.AutoMirrored.Filled.TrendingUp,
        "task_alt" to Icons.Filled.TaskAlt,
        "checklist" to Icons.Filled.Checklist,
        "assignment_turned_in" to Icons.Filled.AssignmentTurnedIn,
        "calendar_month" to Icons.Filled.CalendarMonth,
        "timer" to Icons.Filled.Timer,
        "schedule" to Icons.Filled.Schedule,
        "bar_chart" to Icons.Filled.BarChart,
        // Creatividad y hobbies
        "palette" to Icons.Filled.Palette,
        "brush" to Icons.Filled.Brush,
        "music_note" to Icons.Filled.MusicNote,
        "piano" to Icons.Filled.Piano,
        "camera_alt" to Icons.Filled.CameraAlt,
        "videocam" to Icons.Filled.Videocam,
        "mic" to Icons.Filled.Mic,
        "extension" to Icons.Filled.Extension,
        "sports_esports" to Icons.Filled.SportsEsports,
        "casino" to Icons.Filled.Casino,
        "design_services" to Icons.Filled.DesignServices,
        "podcasts" to Icons.Filled.Podcasts,
        // Social y relaciones
        "groups" to Icons.Filled.Groups,
        "people" to Icons.Filled.People,
        "favorite" to Icons.Filled.Favorite,
        "call" to Icons.Filled.Call,
        "chat" to Icons.AutoMirrored.Filled.Chat,
        "celebration" to Icons.Filled.Celebration,
        "card_giftcard" to Icons.Filled.CardGiftcard,
        "handshake" to Icons.Filled.Handshake,
        "volunteer_activism" to Icons.Filled.VolunteerActivism,
        "family_restroom" to Icons.Filled.FamilyRestroom,
        // Naturaleza y aire libre
        "park" to Icons.Filled.Park,
        "hiking" to Icons.Filled.Hiking,
        "terrain" to Icons.Filled.Terrain,
        "beach_access" to Icons.Filled.BeachAccess,
        "forest" to Icons.Filled.Forest,
        "grass" to Icons.Filled.Grass,
        "local_florist" to Icons.Filled.LocalFlorist,
        "waves" to Icons.Filled.Waves,
        "sailing" to Icons.Filled.Sailing,
        "kayaking" to Icons.Filled.Kayaking,
        "wb_twilight" to Icons.Filled.WbTwilight,
        "landscape" to Icons.Filled.Landscape,
        // Tecnología y pantallas
        "smartphone" to Icons.Filled.Smartphone,
        "computer" to Icons.Filled.Computer,
        "tv" to Icons.Filled.Tv,
        "headset" to Icons.Filled.Headset,
        "keyboard" to Icons.Filled.Keyboard,
        "notifications_off" to Icons.Filled.NotificationsOff,
        "do_not_disturb" to Icons.Filled.DoNotDisturbOn,
        "tablet" to Icons.Filled.Tablet,
        "desktop_windows" to Icons.Filled.DesktopWindows,
        "videogame_asset" to Icons.Filled.VideogameAsset,
        // Autocuidado y mindfulness
        "bathtub" to Icons.Filled.Bathtub,
        "hot_tub" to Icons.Filled.HotTub,
        "mood" to Icons.Filled.Mood,
        "sentiment_very_satisfied" to Icons.Filled.SentimentVerySatisfied,
        "nightlight_round" to Icons.Filled.NightlightRound,
        "psychology" to Icons.Filled.Psychology,
        "face" to Icons.Filled.Face,
        "emoji_emotions" to Icons.Filled.EmojiEmotions,
        "weekend" to Icons.Filled.Weekend,
        "auto_awesome" to Icons.Filled.AutoAwesome,
        // Deportes y motivación
        "sports_basketball" to Icons.Filled.SportsBasketball,
        "sports_soccer" to Icons.Filled.SportsSoccer,
        "sports_tennis" to Icons.Filled.SportsTennis,
        "sports_golf" to Icons.Filled.SportsGolf,
        "sports_volleyball" to Icons.Filled.SportsVolleyball,
        "sports_martial_arts" to Icons.Filled.SportsMartialArts,
        "skateboarding" to Icons.Filled.Skateboarding,
        "surfing" to Icons.Filled.Surfing,
        "local_fire_department" to Icons.Filled.LocalFireDepartment,
        "emoji_events" to Icons.Filled.EmojiEvents,
        "military_tech" to Icons.Filled.MilitaryTech,
        "flag" to Icons.Filled.Flag
    )

    /** Human-readable description for each icon key, used as a screen-reader label and as the
     *  searchable text in the icon picker. */
    private val DESCRIPTION_RES_BY_KEY: Map<String, Int> = mapOf(
        "dumbbell" to R.string.icon_dumbbell,
        "book" to R.string.icon_book,
        "self_improvement" to R.string.icon_self_improvement,
        "water_drop" to R.string.icon_water_drop,
        "bedtime" to R.string.icon_bedtime,
        "school" to R.string.icon_school,
        "edit_note" to R.string.icon_edit_note,
        "directions_walk" to R.string.icon_directions_walk,
        "running" to R.string.icon_running,
        "cycling" to R.string.icon_cycling,
        "swimming" to R.string.icon_swimming,
        "headphones" to R.string.icon_headphones,
        "wb_sunny" to R.string.icon_wb_sunny,
        "nights_stay" to R.string.icon_nights_stay,
        "laptop" to R.string.icon_laptop,
        "notebook" to R.string.icon_notebook,
        "folder" to R.string.icon_folder,
        "alarm" to R.string.icon_alarm,
        "restaurant" to R.string.icon_restaurant,
        "clean_hands" to R.string.icon_clean_hands,
        "cleaning" to R.string.icon_cleaning,
        "bed" to R.string.icon_bed,
        "spa" to R.string.icon_spa,
        "pets" to R.string.icon_pets,
        "local_dining" to R.string.icon_local_dining,
        "breakfast_dining" to R.string.icon_breakfast_dining,
        "lunch_dining" to R.string.icon_lunch_dining,
        "dinner_dining" to R.string.icon_dinner_dining,
        "icecream" to R.string.icon_icecream,
        "local_cafe" to R.string.icon_local_cafe,
        "egg" to R.string.icon_egg,
        "monitor_weight" to R.string.icon_monitor_weight,
        "medication" to R.string.icon_medication,
        "health_and_safety" to R.string.icon_health_and_safety,
        "vaccines" to R.string.icon_vaccines,
        "bloodtype" to R.string.icon_bloodtype,
        "local_laundry" to R.string.icon_local_laundry,
        "iron" to R.string.icon_iron,
        "recycling" to R.string.icon_recycling,
        "delete_sweep" to R.string.icon_delete_sweep,
        "shopping_cart" to R.string.icon_shopping_cart,
        "checkroom" to R.string.icon_checkroom,
        "plumbing" to R.string.icon_plumbing,
        "handyman" to R.string.icon_handyman,
        "chair" to R.string.icon_chair,
        "countertops" to R.string.icon_countertops,
        "kitchen" to R.string.icon_kitchen,
        "home_repair_service" to R.string.icon_home_repair_service,
        "savings" to R.string.icon_savings,
        "account_balance_wallet" to R.string.icon_account_balance_wallet,
        "attach_money" to R.string.icon_attach_money,
        "receipt_long" to R.string.icon_receipt_long,
        "trending_up" to R.string.icon_trending_up,
        "task_alt" to R.string.icon_task_alt,
        "checklist" to R.string.icon_checklist,
        "assignment_turned_in" to R.string.icon_assignment_turned_in,
        "calendar_month" to R.string.icon_calendar_month,
        "timer" to R.string.icon_timer,
        "schedule" to R.string.icon_schedule,
        "bar_chart" to R.string.icon_bar_chart,
        "palette" to R.string.icon_palette,
        "brush" to R.string.icon_brush,
        "music_note" to R.string.icon_music_note,
        "piano" to R.string.icon_piano,
        "camera_alt" to R.string.icon_camera_alt,
        "videocam" to R.string.icon_videocam,
        "mic" to R.string.icon_mic,
        "extension" to R.string.icon_extension,
        "sports_esports" to R.string.icon_sports_esports,
        "casino" to R.string.icon_casino,
        "design_services" to R.string.icon_design_services,
        "podcasts" to R.string.icon_podcasts,
        "groups" to R.string.icon_groups,
        "people" to R.string.icon_people,
        "favorite" to R.string.icon_favorite,
        "call" to R.string.icon_call,
        "chat" to R.string.icon_chat,
        "celebration" to R.string.icon_celebration,
        "card_giftcard" to R.string.icon_card_giftcard,
        "handshake" to R.string.icon_handshake,
        "volunteer_activism" to R.string.icon_volunteer_activism,
        "family_restroom" to R.string.icon_family_restroom,
        "park" to R.string.icon_park,
        "hiking" to R.string.icon_hiking,
        "terrain" to R.string.icon_terrain,
        "beach_access" to R.string.icon_beach_access,
        "forest" to R.string.icon_forest,
        "grass" to R.string.icon_grass,
        "local_florist" to R.string.icon_local_florist,
        "waves" to R.string.icon_waves,
        "sailing" to R.string.icon_sailing,
        "kayaking" to R.string.icon_kayaking,
        "wb_twilight" to R.string.icon_wb_twilight,
        "landscape" to R.string.icon_landscape,
        "smartphone" to R.string.icon_smartphone,
        "computer" to R.string.icon_computer,
        "tv" to R.string.icon_tv,
        "headset" to R.string.icon_headset,
        "keyboard" to R.string.icon_keyboard,
        "notifications_off" to R.string.icon_notifications_off,
        "do_not_disturb" to R.string.icon_do_not_disturb,
        "tablet" to R.string.icon_tablet,
        "desktop_windows" to R.string.icon_desktop_windows,
        "videogame_asset" to R.string.icon_videogame_asset,
        "bathtub" to R.string.icon_bathtub,
        "hot_tub" to R.string.icon_hot_tub,
        "mood" to R.string.icon_mood,
        "sentiment_very_satisfied" to R.string.icon_sentiment_very_satisfied,
        "nightlight_round" to R.string.icon_nightlight_round,
        "psychology" to R.string.icon_psychology,
        "face" to R.string.icon_face,
        "emoji_emotions" to R.string.icon_emoji_emotions,
        "weekend" to R.string.icon_weekend,
        "auto_awesome" to R.string.icon_auto_awesome,
        "sports_basketball" to R.string.icon_sports_basketball,
        "sports_soccer" to R.string.icon_sports_soccer,
        "sports_tennis" to R.string.icon_sports_tennis,
        "sports_golf" to R.string.icon_sports_golf,
        "sports_volleyball" to R.string.icon_sports_volleyball,
        "sports_martial_arts" to R.string.icon_sports_martial_arts,
        "skateboarding" to R.string.icon_skateboarding,
        "surfing" to R.string.icon_surfing,
        "local_fire_department" to R.string.icon_local_fire_department,
        "emoji_events" to R.string.icon_emoji_events,
        "military_tech" to R.string.icon_military_tech,
        "flag" to R.string.icon_flag
    )

    val ALL_KEYS: List<String> = BY_KEY.keys.toList()

    fun iconFor(key: String): ImageVector = BY_KEY[key] ?: Icons.Filled.CheckCircle

    /** Resolves the string-resource id for an icon key's accessibility description, if mapped. */
    fun descriptionResFor(key: String): Int? = DESCRIPTION_RES_BY_KEY[key]
}

/**
 * Bundled templates' title keys (see [com.gondroid.quoteanime.domain.model.DefaultHabitTemplates])
 * mapped at compile time so [resolveTemplateTitle] never needs reflection — R8/resource
 * shrinking can't see a `getIdentifier()` lookup and may strip the referenced strings.
 */
private val TEMPLATE_TITLE_RES_BY_KEY: Map<String, Int> = mapOf(
    "template_train" to R.string.template_train,
    "template_read" to R.string.template_read,
    "template_meditate" to R.string.template_meditate,
    "template_water" to R.string.template_water,
    "template_sleep_early" to R.string.template_sleep_early,
    "template_study" to R.string.template_study,
    "template_write" to R.string.template_write,
    "template_walk" to R.string.template_walk,
    "template_theme_ninja" to R.string.template_theme_ninja,
    "template_theme_one_piece" to R.string.template_theme_one_piece,
    "template_theme_saiyan" to R.string.template_theme_saiyan,
    "template_theme_pokemon" to R.string.template_theme_pokemon,
    "template_theme_black_clover" to R.string.template_theme_black_clover
)

/** Thematic filler text shown/prefilled when a themed template ([HabitTemplate.themeKey]) is selected. */
private val THEME_DESCRIPTION_RES_BY_KEY: Map<String, Int> = mapOf(
    "ninja" to R.string.habit_theme_description_ninja,
    "one_piece" to R.string.habit_theme_description_one_piece,
    "saiyan" to R.string.habit_theme_description_saiyan,
    "pokemon" to R.string.habit_theme_description_pokemon,
    "black_clover" to R.string.habit_theme_description_black_clover
)

@Composable
fun resolveThemeDescription(themeKey: String?): String? {
    val resId = themeKey?.let { THEME_DESCRIPTION_RES_BY_KEY[it] } ?: return null
    return stringResource(resId)
}

/**
 * Resolves a human-readable accessibility description for an icon key. Falls back to the
 * raw key for any future icon that doesn't yet have a mapped string resource.
 */
@Composable
fun describeIcon(key: String): String {
    val resId = HabitIcons.descriptionResFor(key)
    return if (resId != null) stringResource(resId) else key
}

/**
 * Bundled templates carry a string-resource key (e.g. "template_train") as their title;
 * remote/custom ones carry literal text. Resolve the key to localized display text here,
 * shared by both the onboarding habit picker and the habit editor's template chips.
 */
@Composable
fun resolveTemplateTitle(title: String): String {
    val resId = TEMPLATE_TITLE_RES_BY_KEY[title]
    return if (resId != null) stringResource(resId) else title
}

/**
 * A template chip that's aware of premium locking, shared by the habit editor and the
 * onboarding habit picker so both stay in sync as this gets tuned. Locked chips show a
 * small lock leading-icon and route the tap to [onPremiumTapped] (the paywall) instead of
 * [onSelected] — they can still render as visually distinct from unlocked ones without a
 * separate disabled state, since tapping them is a valid, deliberate action.
 */
@Composable
fun TemplateFilterChip(
    template: HabitTemplate,
    label: String,
    selected: Boolean,
    isPremium: Boolean,
    onSelected: () -> Unit,
    onPremiumTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locked = template.isPremiumOnly && !isPremium
    FilterChip(
        selected = selected,
        onClick = { if (locked) onPremiumTapped() else onSelected() },
        label = { Text(label) },
        leadingIcon = if (locked) {
            {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize - 2.dp)
                )
            }
        } else null,
        modifier = modifier
    )
}
