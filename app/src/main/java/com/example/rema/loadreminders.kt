package com.example.rema

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.Boolean
import kotlin.Array
import kotlin.Exception
import kotlin.String

class loadreminders : AppCompatActivity() {
    var dir: File? = null
    var dir2: File? = null
    lateinit var list: Array<File>
    lateinit var list2: Array<File>
    var addremfloat: Button? = null
    var addcard: CardView? = null
    var addbutton: Button? = null
    var remtext: TextView? = null
    var remiderlayout: LinearLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loadreminders)
        remiderlayout = findViewById(R.id.reminderlinearlayout)
        addbutton = findViewById(R.id.addbutton)
        remtext = findViewById(R.id.remtext)
        addcard = findViewById(R.id.addcard)
        addcard?.setVisibility(View.GONE)
        addremfloat = findViewById(R.id.addremfloatbutton)
        addremfloat?.setOnClickListener(View.OnClickListener {
            if (addcard?.getVisibility() == View.GONE) {
                addcard?.setVisibility(View.VISIBLE)
                remtext?.setText("")
            } else {
                addcard?.setVisibility(View.GONE)
            }
        })
        addbutton?.setOnClickListener(View.OnClickListener {
            if (remtext?.getText().toString().isEmpty()) {
                Toast.makeText(this@loadreminders, "Cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                write(remtext?.getText().toString())
                Toast.makeText(this@loadreminders, "Reminder added", Toast.LENGTH_SHORT).show()
                read()
                addcard?.setVisibility(View.GONE)
            }
        })
        dir = applicationContext.filesDir
        list = dir?.listFiles() as Array<File>
        if (list.size != 0) {
            read()
        } else {
            Toast.makeText(this, "nofiles", Toast.LENGTH_SHORT).show()
        }


        // write();
    }

    fun write(s: String?) {
        val data = JSONObject()
        try {
            data.put("name", s)
            data.put("yes?", Boolean.TRUE)
            data.put("date_time", System.currentTimeMillis())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        try {
            val userString = data.toString()
            val file = File(applicationContext.filesDir, "" + System.currentTimeMillis())
            val fileWriter = FileWriter(file)
            val bufferedWriter = BufferedWriter(fileWriter)
            bufferedWriter.write(userString)
            bufferedWriter.close()
        } catch (e: Exception) {
        }
    }

    fun read() {
        dir = applicationContext.filesDir
        list = dir?.listFiles() as Array<File>
        remiderlayout!!.removeAllViews()
        val response = ArrayList<String>()
        for (i in list.indices) {
            try {
                val file = File(list[i].toString())
                val fileReader = FileReader(file)
                val bufferedReader = BufferedReader(fileReader)
                val stringBuilder = StringBuilder()
                var line = bufferedReader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append("\n")
                    line = bufferedReader.readLine()
                }
                bufferedReader.close()
                // This responce will have Json Format String
                response.add(stringBuilder.toString())
            } catch (e: Exception) {
            }
        }
        for (i in list.indices) {
            try {
                var jsonObject: JSONObject
                jsonObject = JSONObject(response[i])
                val child: View = layoutInflater.inflate(R.layout.reminderblocks, null)
                val t = child.findViewById<TextView>(R.id.remindername)
                t.text = jsonObject["name"].toString()
                val name = jsonObject["name"].toString()
                val filename = list[i].toString()
                child.setOnLongClickListener { // Toast.makeText(loadreminders.this, "U just long pressed"+name, Toast.LENGTH_SHORT).show();
                    deleterem(filename)
                    false
                }
                remiderlayout!!.addView(child)
            } catch (e: Exception) {
            }
        }
    }

    fun deleterem(name: String?) {
        dir2 = applicationContext.filesDir
        list2 = dir2?.listFiles() as Array<File>
        val file = File(name)
        file.delete()
        val handler = Handler()
        handler.postDelayed({ read() }, 10)
        //
    }
}