package app.prafullkumar.stats.data

/**
 * Ready-made goals. Each step carries its own why, so the reason for doing it
 * is in front of you when it shows up as today's task.
 */
data class GoalTemplate(
    val name: String,
    val area: GoalArea,
    val why: String,
    val metricName: String = "",
    val metricTarget: Double = 0.0,
    val steps: List<Pair<String, String>>
) {
    fun build(today: String, newId: (String) -> String): Goal = Goal(
        id = newId("goal"),
        title = name,
        why = why,
        area = area,
        metricName = metricName,
        metricTarget = metricTarget,
        createdAt = today,
        steps = steps.map { (title, why) -> GoalStep(newId("step"), title, why) }
    )
}

object GoalTemplates {

    val publishAndroidApp = GoalTemplate(
        name = "Publish an Android app on Play Store",
        area = GoalArea.APPS,
        why = "Shipping is the only thing that counts. A live app earns, teaches and compounds — a folder of unfinished projects does none of that.",
        metricName = "Installs",
        metricTarget = 1000.0,
        steps = listOf(
            "Write the one-line pitch and who it is for" to "If it cannot be said in one line, it cannot be marketed.",
            "Check 5 competitors and their 1-star reviews" to "Their complaints are your feature list.",
            "Write a one-page PRD with the MVP scope" to "Scope creep kills more apps than bugs do.",
            "Design the core screens" to "Deciding the flow on paper is 10x cheaper than in code.",
            "Build the MVP — core loop only" to "Real users on a small app beat no users on a big one.",
            "Add crash reporting and basic analytics" to "You cannot fix what you cannot see.",
            "Create the Play Console developer account" to "Verification can take days — start early.",
            "Write and host a privacy policy" to "Play requires it for almost every app.",
            "Fill Data safety, content rating and target audience forms" to "Missing forms block every release.",
            "Prepare the signed release AAB and app signing" to "Lose the upload key and updates get painful.",
            "Make store listing: icon, feature graphic, 4+ screenshots" to "The listing is the ad — most installs are decided here.",
            "Write ASO title and short description with keywords" to "Search is the free acquisition channel.",
            "Closed test with 12+ testers for 14 days" to "New personal accounts must do this before production access.",
            "Apply for production access" to "Unlocks the public release.",
            "Release to production (staged rollout)" to "A staged rollout limits the blast radius of a bad build.",
            "Ask first users for reviews" to "Early ratings decide ranking.",
            "Ship the first update from feedback" to "Fast follow-ups turn installs into fans."
        )
    )

    val sideProjectRevenue = GoalTemplate(
        name = "Reach first ₹10k / month from apps",
        area = GoalArea.MONEY,
        why = "Income that does not need me in the room buys freedom of time.",
        metricName = "₹ per month",
        metricTarget = 10000.0,
        steps = listOf(
            "Pick the monetisation model (ads, subscription, one-time)" to "The model shapes the whole product.",
            "Add paywall / ads to the app with the most users" to "Monetise where the traffic already is.",
            "Set up payments profile and tax info" to "No payout without it.",
            "A/B test the paywall copy" to "Small copy changes move conversion a lot.",
            "Publish a second app in the same niche" to "A portfolio smooths out one app's bad month."
        )
    )

    val bodyRecomp = GoalTemplate(
        name = "Body recomposition",
        area = GoalArea.HEALTH,
        why = "Energy, confidence and long-term health. A strong body makes everything else easier.",
        metricName = "kg body weight",
        steps = listOf(
            "Set calorie and protein targets" to "What gets measured gets managed.",
            "Build the daily diet plan in the app" to "Eating the same base daily removes decisions.",
            "Strength train 4x a week for 12 weeks" to "Muscle is built by consistency, not intensity.",
            "Hit protein 6 days out of 7" to "Protein is the lever for keeping muscle in a deficit.",
            "Sleep 7.5h average for a month" to "Recovery is where progress happens.",
            "Take progress photos every 2 weeks" to "The scale lies; photos don't."
        )
    )

    val learnSkill = GoalTemplate(
        name = "Learn a new skill deeply",
        area = GoalArea.LEARNING,
        why = "Skills compound — every one makes the next easier and opens new doors.",
        metricName = "focused hours",
        metricTarget = 100.0,
        steps = listOf(
            "Pick one solid resource and finish it" to "Switching resources feels like progress but isn't.",
            "Build a small project with it" to "Knowledge only sticks when used.",
            "Write a post explaining what you learned" to "Teaching exposes the gaps.",
            "Build a bigger project end to end" to "Depth comes from finishing hard things."
        )
    )

    val all = listOf(publishAndroidApp, sideProjectRevenue, bodyRecomp, learnSkill)
}
