package com.example.demonic.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

interface GestureRepository {
    fun getGestures(): List<Gesture>
}

class AssetJsonGestureRepository(private val context: Context) : GestureRepository {

    private val cachedGestures: List<Gesture> by lazy {
        try {
            context.assets.open("gestures.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<GestureRaw>>() {}.type
                    val rawGestures: List<GestureRaw> = Gson().fromJson(reader, type)
                    rawGestures.map { raw ->
                        val rule = raw.rule?.let { r ->
                            when (r.type) {
                                "compound" -> r.components?.let { GestureRule.Compound(it) }
                                "initialized" -> if (r.letter != null && r.base != null) {
                                    GestureRule.Initialized(r.letter, r.base)
                                } else null
                                "fingerspell" -> r.word?.let { GestureRule.Fingerspell(it) }
                                else -> null
                            }
                        }
                        Gesture(
                            id = raw.id,
                            videoPath = raw.video_path,
                            aliases = raw.aliases ?: emptyList(),
                            rule = rule,
                            type = raw.type ?: "single"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun getGestures(): List<Gesture> = cachedGestures

    private data class GestureRaw(
        val id: String,
        val video_path: String?,
        val aliases: List<String>?,
        val rule: RuleRaw?,
        val type: String?
    )

    private data class RuleRaw(
        val type: String?,
        val components: List<String>?,
        val letter: String?,
        val base: String?,
        val word: String?
    )
}
