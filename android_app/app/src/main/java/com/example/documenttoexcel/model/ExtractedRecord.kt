package com.example.documenttoexcel.model

data class ExtractedRecord(
    val fileReference: String,
    val date: String,
    val particulars: String,
    val amount: Double
)
