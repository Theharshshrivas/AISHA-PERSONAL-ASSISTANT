package com.harsh.aisha

import android.content.Context

class MemoryStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        "aisha_local_memory",
        Context.MODE_PRIVATE
    )

    fun getAll(): List<String> {
        return preferences
            .getStringSet("notes", emptySet())
            .orEmpty()
            .sorted()
    }

    fun add(note: String): Boolean {
        val cleanedNote = note.trim()

        if (cleanedNote.isBlank()) return false

        val currentNotes = getAll().toMutableSet()
        currentNotes.add(cleanedNote)

        preferences.edit()
            .putStringSet("notes", currentNotes)
            .apply()

        return true
    }

    fun delete(note: String) {
        val currentNotes = getAll().toMutableSet()
        currentNotes.remove(note)

        preferences.edit()
            .putStringSet("notes", currentNotes)
            .apply()
    }

    fun clearAll() {
        preferences.edit()
            .remove("notes")
            .apply()
    }
}
