
package com.example.emergencyfinder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class OverpassHelper {

    private val client = OkHttpClient()

    suspend fun searchPlaces(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        category: String
    ): List<Place> = withContext(Dispatchers.IO) {

        val query = """
            [out:json][timeout:25];
            nwr["amenity"="$category"]
            (around:$radiusMeters,$latitude,$longitude);
            out center tags;
        """.trimIndent()

        val requestBody = FormBody.Builder()
            .add("data", query)
            .build()

        val request = Request.Builder()
            .url("https://overpass-api.de/api/interpreter")
            .post(requestBody)
            .header("User-Agent", "EmergencyFinderAndroidApp")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            response.close()
            throw IOException("OpenStreetMap request failed: ${response.code}")
        }

        val responseText = response.body?.string()
            ?: throw IOException("Empty response from OpenStreetMap")

        response.close()

        val json = JSONObject(responseText)
        val elements = json.getJSONArray("elements")
        val places = mutableListOf<Place>()

        for (i in 0 until elements.length()) {

            val element = elements.getJSONObject(i)
            val tags = element.optJSONObject("tags") ?: continue

            val name = tags.optString("name", "Unnamed place")

            val lat: Double
            val lon: Double

            if (element.has("lat") && element.has("lon")) {
                lat = element.getDouble("lat")
                lon = element.getDouble("lon")
            } else {
                val center = element.optJSONObject("center") ?: continue
                lat = center.optDouble("lat", Double.NaN)
                lon = center.optDouble("lon", Double.NaN)
            }

            if (!lat.isFinite() || !lon.isFinite()) continue

            val phone = tags.optString("phone")
                .takeIf { it.isNotBlank() }
                ?: tags.optString("contact:phone")
                    .takeIf { it.isNotBlank() }

            val elementId = element.optString("type") +
                    "_" + element.optString("id")

            places.add(
                Place(
                    id = elementId,
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    phone = phone,
                    category = category
                )
            )
        }

        places
    }
}
