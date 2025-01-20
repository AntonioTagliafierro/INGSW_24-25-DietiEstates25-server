package com.security.state

object StateStore {

    // Una mappa per archiviare temporaneamente gli stati
    private val stateMap: MutableMap<String, Long> = mutableMapOf()

    // Salva uno stato con il timestamp corrente
    fun save(state: String) {
        stateMap[state] = System.currentTimeMillis()
    }

    // Recupera e verifica uno stato
    fun get(state: String?): Boolean {
        if (state == null) return false

        val timestamp = stateMap[state] ?: return false

        // Controlla se lo stato è ancora valido
        val isValid = System.currentTimeMillis() - timestamp <= 5 * 60 * 1000L

        if (isValid) {
            // Rimuove lo stato per evitare riutilizzi
            stateMap.remove(state)
        }

        return isValid
    }

    // Pulisce stati scaduti dalla mappa
    fun cleanupExpiredStates() {
        val currentTime = System.currentTimeMillis()
        stateMap.entries.removeIf { currentTime - it.value > 5 * 60 * 1000L }
    }
}