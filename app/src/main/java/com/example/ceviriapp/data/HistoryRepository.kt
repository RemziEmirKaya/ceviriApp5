package com.example.ceviriapp.data

import android.content.Context
import com.example.ceviriapp.HistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("translation_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getHistory(): List<HistoryItem> {
        val json = sharedPreferences.getString(HISTORY_KEY, null)
        return if (json != null) {
            // JSON string'ini HistoryItem listesine çevir
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Eğer kayıt yoksa boş liste döndür
            emptyList()
        }
    }

    fun saveHistory(history: List<HistoryItem>) {
        // Mevcut geçmiş listesini JSON string'ine çevir
        val json = gson.toJson(history)
        // SharedPreferences'e kaydet
        sharedPreferences.edit().putString(HISTORY_KEY, json).apply()
    }

    companion object {
        private const val HISTORY_KEY = "history_list_json"
    }
}
