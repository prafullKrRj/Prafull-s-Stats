package app.prafullkumar.stats.data

import org.json.JSONArray
import org.json.JSONObject

/** Meal slots of the day, in the order they happen. */
enum class Slot(val title: String, val time: String) {
    WAKE("Wake up", "early"),
    BREAKFAST("Breakfast", "morning"),
    MID_MORNING("Mid-morning", "snack"),
    LUNCH("Lunch", "afternoon"),
    PRE_WORKOUT("Pre-workout", "fuel"),
    POST_WORKOUT("Post-workout", "recovery"),
    DINNER("Dinner", "evening"),
    BEDTIME("Before bed", "night");

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: LUNCH
    }
}

/**
 * One item of the daily plan — the food eaten almost every day. Ticked off on
 * the checklist instead of being re-entered. [core] items are the must-haves
 * (usually the protein base) and count toward the day score on their own.
 */
data class FoodItem(
    val id: String,
    val name: String,
    val detail: String,
    val slot: Slot,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val core: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("detail", detail); put("slot", slot.name)
        put("kcal", kcal); put("protein", protein); put("carbs", carbs); put("fat", fat)
        put("fiber", fiber); put("core", core)
    }

    companion object {
        fun fromJson(o: JSONObject) = FoodItem(
            id = o.getString("id"),
            name = o.getString("name"),
            detail = o.optString("detail"),
            slot = Slot.from(o.optString("slot")),
            kcal = o.optInt("kcal"),
            protein = o.optDouble("protein", 0.0),
            carbs = o.optDouble("carbs", 0.0),
            fat = o.optDouble("fat", 0.0),
            fiber = o.optDouble("fiber", 0.0),
            core = o.optBoolean("core")
        )
    }
}

/**
 * Off-plan food. Nutrition is stored per 100 g / 100 ml so logging only asks
 * for the quantity.
 */
data class LibraryFood(
    val id: String,
    val name: String,
    /** "g" or "ml" */
    val unit: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("unit", unit)
        put("kcal100", kcalPer100); put("protein100", proteinPer100)
        put("carbs100", carbsPer100); put("fat100", fatPer100); put("fiber100", fiberPer100)
    }

    companion object {
        fun fromJson(o: JSONObject) = LibraryFood(
            id = o.getString("id"),
            name = o.getString("name"),
            unit = o.optString("unit", "g"),
            kcalPer100 = o.optDouble("kcal100", 0.0),
            proteinPer100 = o.optDouble("protein100", 0.0),
            carbsPer100 = o.optDouble("carbs100", 0.0),
            fatPer100 = o.optDouble("fat100", 0.0),
            fiberPer100 = o.optDouble("fiber100", 0.0)
        )
    }
}

/** How much of a library food was eaten on one day. */
data class Portion(val foodId: String, val qty: Double) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("foodId", foodId); put("qty", qty)
    }

    companion object {
        fun fromJson(o: JSONObject) = Portion(
            foodId = o.getString("foodId"),
            qty = o.optDouble("qty", 0.0)
        )
    }
}

/**
 * A habit to keep a streak on. [target] 1 means a plain checkbox; anything
 * higher is a counter (e.g. 20 pages, 100 push-ups). [days] holds the ISO
 * days of week (1 = Monday) it is scheduled on — other days never break the
 * streak.
 */
data class Habit(
    val id: String,
    val name: String,
    val detail: String = "",
    val target: Int = 1,
    val unit: String = "",
    val days: Set<Int> = ALL_DAYS,
    val createdAt: String,
    val archived: Boolean = false
) {
    val isCounter: Boolean get() = target > 1

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("detail", detail)
        put("target", target); put("unit", unit)
        put("days", JSONArray(days.sorted()))
        put("createdAt", createdAt); put("archived", archived)
    }

    companion object {
        val ALL_DAYS = (1..7).toSet()

        fun fromJson(o: JSONObject) = Habit(
            id = o.getString("id"),
            name = o.getString("name"),
            detail = o.optString("detail"),
            target = o.optInt("target", 1).coerceAtLeast(1),
            unit = o.optString("unit"),
            days = o.optJSONArray("days")?.let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }.toSet()
            }?.takeIf { it.isNotEmpty() } ?: ALL_DAYS,
            createdAt = o.optString("createdAt"),
            archived = o.optBoolean("archived")
        )
    }
}

/** One of the day's top priorities — the "if only this gets done" list. */
data class Priority(val text: String, val done: Boolean = false) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("text", text); put("done", done)
    }

    companion object {
        fun fromJson(o: JSONObject) = Priority(o.getString("text"), o.optBoolean("done"))
    }
}

/** A thought dumped out of the head into the inbox, to be triaged later. */
data class InboxItem(
    val id: String,
    val text: String,
    val createdAt: String,
    val done: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("text", text); put("createdAt", createdAt); put("done", done)
    }

    companion object {
        fun fromJson(o: JSONObject) = InboxItem(
            id = o.getString("id"),
            text = o.getString("text"),
            createdAt = o.optString("createdAt"),
            done = o.optBoolean("done")
        )
    }
}

/** Everything logged for one calendar day. */
data class DayLog(
    val date: String,
    val foods: Set<String> = emptySet(),
    val portions: List<Portion> = emptyList(),
    /** Habit id -> count done that day (1 for a checkbox habit). */
    val habits: Map<String, Int> = emptyMap(),
    val weight: Double? = null,
    val water: Int = 0,
    val sleep: Double? = null,
    /** 1..5, 0 = not set. */
    val mood: Int = 0,
    /** 1..5, 0 = not set. */
    val energy: Int = 0,
    val priorities: List<Priority> = emptyList(),
    val focusMinutes: Int = 0,
    val focusSessions: Int = 0,
    val gratitude: String = "",
    val win: String = "",
    val note: String = ""
) {
    fun habitCount(id: String): Int = habits[id] ?: 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("foods", JSONArray(foods.toList()))
        put("portions", JSONArray(portions.map { it.toJson() }))
        put("habits", JSONObject().apply { habits.forEach { (k, v) -> put(k, v) } })
        weight?.let { put("weight", it) }
        put("water", water)
        sleep?.let { put("sleep", it) }
        put("mood", mood)
        put("energy", energy)
        put("priorities", JSONArray(priorities.map { it.toJson() }))
        put("focusMinutes", focusMinutes)
        put("focusSessions", focusSessions)
        put("gratitude", gratitude)
        put("win", win)
        put("note", note)
    }

    companion object {
        fun fromJson(o: JSONObject): DayLog {
            val foods = o.optJSONArray("foods")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }.toSet()
            }.orEmpty()
            val portions = o.optJSONArray("portions")?.let { arr ->
                (0 until arr.length()).map { Portion.fromJson(arr.getJSONObject(it)) }
            }.orEmpty()
            val habits = o.optJSONObject("habits")?.let { obj ->
                obj.keys().asSequence().associateWith { obj.optInt(it) }
            }.orEmpty()
            val priorities = o.optJSONArray("priorities")?.let { arr ->
                (0 until arr.length()).map { Priority.fromJson(arr.getJSONObject(it)) }
            }.orEmpty()
            return DayLog(
                date = o.getString("date"),
                foods = foods,
                portions = portions,
                habits = habits,
                weight = if (o.has("weight")) o.getDouble("weight") else null,
                water = o.optInt("water"),
                sleep = if (o.has("sleep")) o.getDouble("sleep") else null,
                mood = o.optInt("mood"),
                energy = o.optInt("energy"),
                priorities = priorities,
                focusMinutes = o.optInt("focusMinutes"),
                focusSessions = o.optInt("focusSessions"),
                gratitude = o.optString("gratitude"),
                win = o.optString("win"),
                note = o.optString("note")
            )
        }
    }
}

data class Targets(
    val kcal: Int = 2200,
    val protein: Int = 140,
    val fat: Int = 65,
    val carbs: Int = 230,
    val fiber: Int = 30,
    /** Glasses of ~250 ml. */
    val water: Int = 12,
    val goalWeight: Double = 70.0,
    val sleep: Double = 7.5,
    val focusMinutes: Int = 120,
    /** Day score (0–100) needed for a day to count toward the beast streak. */
    val beastScore: Int = 80
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("kcal", kcal); put("protein", protein); put("fat", fat)
        put("carbs", carbs); put("fiber", fiber); put("water", water)
        put("goalWeight", goalWeight); put("sleep", sleep)
        put("focusMinutes", focusMinutes); put("beastScore", beastScore)
    }

    companion object {
        fun fromJson(o: JSONObject): Targets {
            val d = Targets()
            return Targets(
                kcal = o.optInt("kcal", d.kcal),
                protein = o.optInt("protein", d.protein),
                fat = o.optInt("fat", d.fat),
                carbs = o.optInt("carbs", d.carbs),
                fiber = o.optInt("fiber", d.fiber),
                water = o.optInt("water", d.water),
                goalWeight = o.optDouble("goalWeight", d.goalWeight),
                sleep = o.optDouble("sleep", d.sleep),
                focusMinutes = o.optInt("focusMinutes", d.focusMinutes),
                beastScore = o.optInt("beastScore", d.beastScore)
            )
        }
    }
}

data class DayTotals(
    val kcal: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0
)

/** One scored part of the day, e.g. "Protein 0.8". */
data class ScorePart(val label: String, val value: Float)

data class DayScore(val score: Int, val parts: List<ScorePart>)

data class Streak(val current: Int, val best: Int)
