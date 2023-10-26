package com.example.rema

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.*
import java.lang.Boolean
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.Array
import kotlin.ByteArray
import kotlin.Exception
import kotlin.Float
import kotlin.FloatArray
import kotlin.Int
import kotlin.IntArray
import kotlin.String
import kotlin.Throws
import kotlin.arrayOf

class MainActivity : AppCompatActivity() {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var recogText: TextView? = null
    val RecordAudioRequestCode = 1
    var file: File? = null




    // Name of TFLite model ( in /assets folder ).
    private val MODEL_ASSETS_PATH = "model_h5.tflite"

    // Max Length of input sequence. The input shape for the model will be ( None , INPUT_MAXLEN ).
    private val INPUT_MAXLEN = 5

    private var tfLiteInterpreter : Interpreter? = null

    // Init the classifier.
    val classifier = Classifier( this , "tokenizer.json" , INPUT_MAXLEN )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var stringbuffer:Button=findViewById(R.id.stringbufferactivity)
        var button:Button=findViewById(R.id.button)
        var stop:Button=findViewById(R.id.stop)
        var returnedText:TextView = findViewById(R.id.text);
        var textbar:TextView=findViewById(R.id.textView2)
        var swipe:ConstraintLayout=findViewById(R.id.swipelayer)
        var result_text:TextView=findViewById(R.id.output_score)
        mute() //mute audios from notifications
        //unmute();

        // splitting file
        file = File(
            applicationContext.filesDir,
            "buffer.txt"
        )


        //swipe
        swipe.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
            @Override
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Toast.makeText(this@MainActivity, "Swipe up gesture detected", Toast.LENGTH_SHORT)
                    .show()
                startActivity(Intent(this@MainActivity,loadreminders::class.java))

            }

        })

        try {
            tfLiteInterpreter = Interpreter( loadModelFile() )
        } catch (e: Exception) {
        }
        classifier.processVocab( object: Classifier.VocabCallback {
            override fun onVocabProcessed() {
                Log.d("tag","done")
            }
        })




        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission()
        }

        //editText = findViewById(R.id.textView);
        recogText = findViewById(R.id.textView2)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        textToSpeech = TextToSpeech(
            this
        ) { textToSpeech!!.speak("HELLO MORTAL", TextToSpeech.QUEUE_FLUSH, null, null) }
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        button.setOnClickListener{
            speechRecognizer?.startListening(speechRecognizerIntent)
            button.isVisible=false
            button.isClickable=false
            stop.isVisible=true
            stop.isClickable=true
        }

        stop.setOnClickListener{
            speechRecognizer?.destroy()
            returnedText.text = "Tap to start Again"
            stop.isVisible=false
            stop.isClickable=false
            button.isVisible=true
            button.isClickable=true
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {}
            override fun onBeginningOfSpeech() {
                Log.i("logTag", "onBeginningOfSpeech")
                returnedText.setText("")
                returnedText.hint = "Listening..."
            }
            override fun onRmsChanged(v: Float) {
                //  if (v>0)Toast.makeText(MainActivity.this, "..........new sentence......"+v, Toast.LENGTH_SHORT).show();
            }

            override fun onBufferReceived(bytes: ByteArray) {}
//            override fun onEndOfSpeech() {
//                val handler = Handler()
//                handler.postDelayed(Runnable { restart() }, 200)
//            }

            override fun onEndOfSpeech() {
                Handler().postDelayed({
                    speechRecognizer?.startListening(speechRecognizerIntent)
                }, 200)

            }

            override fun onError(error: Int) {

                speechRecognizer?.startListening(speechRecognizerIntent)
            }

//            private fun getErrorText(error: Int): String {
//                var message = ""
//                message = when (error) {
//                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
//                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
//                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
//                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
//                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
//                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
//                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "listening"
//                    SpeechRecognizer.ERROR_SERVER -> "error from server"
//                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
//                    else -> "Didn't understand, please try again."
//                }
//                return message
//            }


            override fun onResults(bundle: Bundle?) {
                Log.i("logTag", "onResults")
                val matches = bundle!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val message = java.lang.String.join("",matches!![0]).toLowerCase().trim()
                var text =stringSplitter(message)
                if ( !TextUtils.isEmpty( message ) ) {
                    // Tokenize and pad the given input text.
                    val tokenizedMessage =classifier.tokenize(text)
                    val paddedMessage = classifier.padSequence(tokenizedMessage)

                    val results = classifySequence(paddedMessage)
                    val class1 = results[0]
                    val class2 = results[1]
                    var z = class2
                    Log.d("val", "$z")
                    if (z > 0.3) {
                        write(message)
                        Log.d("done", "called")


                    } else {
                        Log.d("Invalid", "trashed")
                    }

                    // = "the sentence is ${message}\nyes : $class2\nno  : $class1 "
                } else {
                    Toast.makeText(this@MainActivity, "Please enter a message.", Toast.LENGTH_LONG)
                        .show();
                }



            }

//            override fun onResults(bundle: Bundle) {
//                // micButton.setImageResource(R.drawable.ic_mic_black_off);
//                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                recogText?.setText(data!![0])
//
//                /* if(data.toString().contains("hello")){
//                    textToSpeech.speak("What is wrong with you? I already said hello",TextToSpeech.QUEUE_FLUSH,null,null);
//                }*/Toast.makeText(this@MainActivity, "Stop", Toast.LENGTH_SHORT).show()
//            }


            override fun onPartialResults(partialResults: Bundle?) {
                Log.i("logTag", "onResults")
                val matches = partialResults!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//        var text = ""
//        for (result in matches) text = """
//      $result
//      """.trimIndent()
                var text =java.lang.String.join("",matches!![0])
                textbar.setText(text)
                Log.d("taggg",text)
            }
            override fun onEvent(i: Int, bundle: Bundle) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer!!.destroy()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RecordAudioRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RecordAudioRequestCode && grantResults.size > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(
                this,
                "Permission Granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

//    fun restart() {
//        speechRecognizer!!.destroy()
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
//        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
//            override fun onReadyForSpeech(bundle: Bundle) {}
//            override fun onBeginningOfSpeech() {}
//            override fun onRmsChanged(v: Float) {
//                //  if (v>0)Toast.makeText(MainActivity.this, "..........new sentence......"+v, Toast.LENGTH_SHORT).show();
//            }
//
//            override fun onBufferReceived(bytes: ByteArray) {}
//
//            override fun onEndOfSpeech() {
//                Handler().postDelayed({
//                  restart()
//                }, 200)
//
//            }
//
//
////            override fun onEndOfSpeech() {
////                val handler = Handler()
////                handler.postDelayed(Runnable { restart() }, 200)
////            }
//
//            override fun onError(i: Int) {}
//            override fun onResults(bundle: Bundle) {
//                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                recogText!!.text = data!![0]
//                Toast.makeText(this@MainActivity, "Stop", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onPartialResults(bundle: Bundle) {
//
//
//                //ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//                // editText.setText(data.get(0));
//                //Toast.makeText(MainActivity.this, data.toString(), Toast.LENGTH_SHORT).show();
//            }
//
//            override fun onEvent(i: Int, bundle: Bundle) {}
//        })
//        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//        speechRecognizerIntent.putExtra(
//            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
//        )
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
//        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
//        speechRecognizer?.startListening(speechRecognizerIntent)
//    }

    private fun mute() {
        //mute audio
        val amanager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
    }

    private fun unmute() {
        //mute audio
        val amanager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
    }


    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd(MODEL_ASSETS_PATH)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Perform inference, given the input sequence.
    private fun classifySequence (sequence : IntArray ): FloatArray {
        // Input shape -> ( 1 , INPUT_MAXLEN )
        val inputs : Array<IntArray> = arrayOf( sequence.map { it}.toIntArray() )
        // Output shape -> ( 1 , 2 ) ( as numClasses = 2 )
        val outputs : Array<FloatArray> = arrayOf( FloatArray( 2 ) )
        tfLiteInterpreter?.run( inputs , outputs )
        return outputs[0]
    }

    companion object {
        const val RecordAudioRequestCode = 1
    }

    fun write(s: String?) {
        val data = JSONObject()
        try {
            data.put("name", s)
            data.put("yes?", Boolean.TRUE)
            data.put("date_time", System.currentTimeMillis())
            Log.d("data entered","done")
            Toast.makeText(this, "Reminder added", Toast.LENGTH_SHORT).show()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        try {
            val userString = data.toString()
            val file = File(applicationContext.filesDir, "" + System.currentTimeMillis())
            val fileWriter = FileWriter(file)
            val bufferedWriter = BufferedWriter(fileWriter)
            bufferedWriter.write(userString)
            Toast.makeText(this, "Reminder added", Toast.LENGTH_SHORT).show()
            bufferedWriter.close()
        } catch (e: Exception) {
        }
    }

    fun stringSplitter(data: String):String //function that string to 5 words and puts in buffer.txt
    {
        var wordcount = 0
        var i: Int
        var partial = ""

        i = 0
        while (i < data.length) {
            partial = partial + "" + data[i]
            if (data[i] == ' ') {
                wordcount++
                if (wordcount == 5) {
                    break
                }
            }
            i++
        }
        return partial
    }


}




