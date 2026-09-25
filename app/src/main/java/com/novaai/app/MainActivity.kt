package com.novaai.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private lateinit var messages: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText

    private val bg = Color.rgb(13, 17, 28)
    private val panel = Color.rgb(25, 32, 46)
    private val accent = Color.rgb(98, 110, 255)

    private val apiKey: String by lazy { BuildConfig.GEMINI_API_KEY }
    private val apiUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        scrollView.addView(messages)
        root.addView(scrollView)

        val inputBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(panel)
            setPadding(16, 16, 16, 16)
        }

        input = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            hint = "اكتب رسالتك..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(panel)
        }

        val sendBtn = Button(this).apply {
            text = "إرسال"
            setBackgroundColor(accent)
            setTextColor(Color.WHITE)
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    addMessage(text, isUser = true)
                    input.setText("")
                    sendToGemini(text)
                }
            }
        }

        inputBar.addView(input)
        inputBar.addView(sendBtn)
        root.addView(inputBar)

        setContentView(root)

        addMessage("أهلاً بك 👋 أنا نوفا، مساعدك الذكي. كيف أقدر أساعدك اليوم؟", isUser = false)
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val bubble = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setPadding(24, 16, 24, 16)
            setBackgroundColor(if (isUser) accent else panel)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (isUser) Gravity.END else Gravity.START
                bottomMargin = 16
            }
        }
        messages.addView(bubble)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendToGemini(userText: String) {
        Thread {
            try {
                val url = URL("$apiUrl?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = JSONObject().apply {
                    put(
                        "contents", JSONArray().put(
                            JSONObject().put(
                                "parts", JSONArray().put(
                                    JSONObject().put("text", userText)
                                )
                            )
                        )
                    )
                }

                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }

                val reply: String = try {
                    val json = JSONObject(response)
                    json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                } catch (e: Exception) {
                    "صار خطأ بالرد (كود $responseCode). تأكد إن مفتاح الـ API صحيح ومفعّل."
                }

                runOnUiThread { addMessage(reply, isUser = false) }

            } catch (e: Exception) {
                runOnUiThread { addMessage("فشل الاتصال: ${e.message}", isUser = false) }
            }
        }.start()
    }
}
