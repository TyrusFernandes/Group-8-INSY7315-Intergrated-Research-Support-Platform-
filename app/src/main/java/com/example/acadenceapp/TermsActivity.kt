package com.example.acadenceapp

import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TermsActivity : AppCompatActivity() {

    private lateinit var termsContent: TextView

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { LocaleHelper.getSavedLanguage(it) } ?: "English"
        val context = newBase?.let { LocaleHelper.setLocale(it, lang) }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        termsContent = findViewById(R.id.termsContent)

        val lang = LocaleHelper.getSavedLanguage(this)
        if (lang == "Afrikaans") {
            termsContent.text = getString(R.string.terms_text)
        } else {
            termsContent.text = getString(R.string.terms_text)
        }

        val backBtn = findViewById<ImageButton>(R.id.backButton)
        backBtn.setOnClickListener { finish() }
    }
}
