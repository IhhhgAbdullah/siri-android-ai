package com.example.sirianroid

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class CommandProcessor(private val context: Context) {

    private val appPackages = mapOf(
        "يوتيوب" to "com.google.android.youtube",
        "واتساب" to "com.whatsapp",
        "الخرائط" to "com.google.android.apps.maps",
        "الإعدادات" to "com.android.settings",
        "الاعدادات" to "com.android.settings",
        "الساعة" to "com.google.android.deskclock",
        "المتصفح" to "com.android.chrome",
        "كروم" to "com.android.chrome"
    )

    /** يحوّل النص المسموع إلى إجراء ويعيد نص الرد الذي سيُنطق. */
    fun process(rawText: String): String {
        val text = normalize(rawText)

        return when {
            text.contains("افتح") || text.contains("شغل") -> openApp(text)
            text.contains("منبه") -> setAlarm(text)
            text.contains("مؤقت") || text.contains("تايمر") -> setTimer(text)
            else -> "عذراً، لم أفهم الأمر بعد. المرحلة الثانية ستضيف الذكاء الاصطناعي للإجابة."
        }
    }

    private fun openApp(text: String): String {
        val match = appPackages.entries.firstOrNull { text.contains(normalize(it.key)) }
            ?: return "لم أجد هذا التطبيق. جرّب اسماً مثل يوتيوب أو واتساب."

        val launchIntent = context.packageManager.getLaunchIntentForPackage(match.value)
            ?: return "التطبيق ${match.key} غير مثبت على هذا الجهاز."

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return "تم فتح ${match.key}."
    }

    private fun setAlarm(text: String): String {
        val (hourRaw, minuteRaw) = extractTime(text)
            ?: return "حدد الوقت للمنبه، مثلاً: منبه الساعة 7:30."

        var hour = hourRaw
        if (text.contains("مساء") && hour < 12) hour += 12

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minuteRaw)
            putExtra(AlarmClock.EXTRA_MESSAGE, "منبه المساعد")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) == null) {
            return "لا يوجد تطبيق منبه متاح على هذا الجهاز."
        }
        context.startActivity(intent)
        return "تم ضبط المنبه على الساعة %02d:%02d.".format(hour, minuteRaw)
    }

    private fun setTimer(text: String): String {
        val number = Regex("(\\d+)").find(text)?.value?.toIntOrNull()
            ?: return "حدد مدة المؤقت، مثلاً: مؤقت 5 دقائق."

        val isHours = text.contains("ساع")
        val seconds = if (isHours) number * 3600 else number * 60

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, "مؤقت المساعد")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) == null) {
            return "لا يوجد تطبيق مؤقت متاح على هذا الجهاز."
        }
        context.startActivity(intent)
        return "تم ضبط المؤقت لمدة $number ${if (isHours) "ساعة" else "دقيقة"}."
    }

    /** يستخرج الساعة والدقيقة من نص مثل "7:30". الافتراضي دقيقة 0. */
    private fun extractTime(text: String): Pair<Int, Int>? {
        val match = Regex("(\\d{1,2})(?::(\\d{2}))?").find(text) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    /** توحيد الهمزات والتاء المربوطة والألف المقصورة لتسهيل المقارنة. */
    private fun normalize(text: String): String =
        text.trim()
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
}
