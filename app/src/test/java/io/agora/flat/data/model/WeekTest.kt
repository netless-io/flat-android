package io.agora.flat.data.model

import com.google.gson.Gson
import io.agora.flat.data.model.Week
import junit.framework.TestCase
import org.junit.Assert.assertArrayEquals

class WeekTest : TestCase() {

    data class WeekList(val weeks: List<Week>)

    fun testDeserialization() {
        val json = "{\"weeks\": [\n" +
                "                0,\n" +
                "                1,\n" +
                "                2,\n" +
                "                3,\n" +
                "                4,\n" +
                "                5,\n" +
                "                6\n" +
                "            ]}"
        val data = Gson().fromJson(json, WeekList::class.java)
        assertArrayEquals(
            arrayOf(
                Week.Sunday,
                Week.Monday,
                Week.Tuesday,
                Week.Wednesday,
                Week.Thursday,
                Week.Friday,
                Week.Saturday
            ), data.weeks.toTypedArray()
        );
    }
}