package com.motawfik.voicereminders

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


class MainActivity : AppCompatActivity() {
    private var textToSpeech: TextToSpeech? = null
    private val STT_REQUEST_CODE = 1
    private var listenAfter = false
    private var selectedTime: String = ""
    private var title: String = ""
    private val progressListener = object : UtteranceProgressListener() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onDone(p0: String?) {
            if (listenAfter)
                listen()
            else
                addCalenderEvent()
        }

        override fun onError(p0: String?) {}
        override fun onStart(p0: String?) {}

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech!!.language = Locale.US
                textToSpeech!!.setOnUtteranceProgressListener(progressListener)
            }
        })
        btn_tts.setOnClickListener {
            speak(getString(R.string.get_event))
            listenAfter = true
        }
    }

    private fun speak(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
        } else {
            textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    private fun listen() {
        val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        sttIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.mic_prompt))
        try {
            // Start the intent for a result, and pass in our request code.
            startActivityForResult(sttIntent, STT_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            // Handling error when the service is not available.
            e.printStackTrace()
            Toast.makeText(this, "Your device does not support STT.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // Handle the result for our request code.
            STT_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // Retrieve the result array.
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (!result.isNullOrEmpty()) {
                        // Recognized text is in the first position.
                        val recognizedText = result[0]
                        txtViewText.text = recognizedText
                        txtApi.text = getString(R.string.analyze)

                        val queue = Volley.newRequestQueue(this)
                        val url = "https://api.wit.ai/message?v=20201109&q=" + recognizedText
                            .replace(" ", "%20")

                        val stringRequest = @RequiresApi(Build.VERSION_CODES.O)
                        object : StringRequest(
                            Method.GET, url,
                            Response.Listener<String> { response ->
                                try {
                                    val obj = JSONObject(response)
                                    val entities = obj.getJSONObject("entities")
                                    val dateTime = entities.getJSONArray("wit\$datetime:datetime")
                                        .getJSONObject(0)
                                    val dateTimeBody = dateTime.getString("body")
                                    selectedTime = dateTime.getString("value").dropLast(10)
                                    val entry =
                                        entities.getJSONArray("wit\$agenda_entry:agenda_entry")
                                            .getJSONObject(0)
                                    title = entry.getString("value")
                                    val text =
                                        "$title has been reserved in your calender for $dateTimeBody"
                                    speak(text)
                                    listenAfter = false
                                    txtApi.text = text
                                } catch (e: JSONException) {
                                    Log.d("JSON", e.printStackTrace().toString())
                                    e.printStackTrace()
                                    txtApi.text = getString(R.string.api_wrong_format)
                                }

                            },
                            Response.ErrorListener { error ->
                                txtApi.text = error.message
                            }) {
                            override fun getHeaders(): MutableMap<String, String> {
                                val headers = HashMap<String, String>()
                                headers["Authorization"] = "Bearer ${BuildConfig.WIT_ACCESS_TOKEN}"
                                return headers
                            }
                        }
                        queue.add(stringRequest)

                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addCalenderEvent() {
        if (selectedTime == "" || title == "")
            return
        val localDateTime = LocalDateTime.parse(selectedTime);
        val time = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(Intent.ACTION_EDIT)
        intent.type = "vnd.android.cursor.item/event"
        intent.putExtra("beginTime", time)
        intent.putExtra("title", title)
        startActivity(intent)
    }

    override fun onPause() {
        textToSpeech!!.stop()
        super.onPause()
    }

    override fun onDestroy() {
        textToSpeech!!.shutdown()
        super.onDestroy()
    }
}