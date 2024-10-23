package com.example.uts_lab_map


data class Attendance(
    val entryImages: List<String> = listOf(),
    val entryTimestamps: List<String> = listOf(),
    val exitImages: List<String> = listOf(),
    val exitTimestamps: List<String> = listOf()
)