package net.yakijake.share_youtube_title

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.widget.Toolbar
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import com.google.gson.Gson

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // activity_toolbar_sample.xml からToolbar要素を取得
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        // アクションバーにツールバーをセット
        setSupportActionBar(toolbar)
        // ツールバーに戻るボタンを設置
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // val gsonData = gsonData()
        // 設定から取得して画面のSwitchとかを現在の状態にする
        Log.d("APP DEBUG LOG", "Setup settings UI..")
        val settingsData = MainActivity.Settings()

        val simple_switch = findViewById<Switch>(R.id.simple_switch)
        val mention_switch = findViewById<Switch>(R.id.mention_switch)
        val thumb_switch = findViewById<Switch>(R.id.thumb_switch)
        val thumb_type_group = findViewById<RadioGroup>(R.id.thumb_type_group)
        val ch_switch = findViewById<Switch>(R.id.ch_switch)
        val api_switch = findViewById<Switch>(R.id.api_switch)
        val api_key = findViewById<EditText>(R.id.api_key)

        simple_switch.isChecked = settingsData.is_simple
        mention_switch.isChecked = settingsData.is_mention
        thumb_switch.isChecked = settingsData.is_thumbnail
        if (settingsData.thumbnail_type == 0) {
            thumb_type_group.check(R.id.thumb_type_url)
        } else {
            thumb_type_group.check(R.id.thumb_type_img)
        }
        ch_switch.isChecked = settingsData.is_channel
        api_switch.isChecked = settingsData.is_api
        api_key.setText(settingsData.api_key)

        Log.d("APP DEBUG LOG", "Setup UI done")


        val save_button = findViewById<Button>(R.id.save_button)
        save_button.setOnClickListener {
            // 設定を上書きしてファイルにも保存
            Log.d("APP DEBUG LOG", "Saving settings...")

            settingsData.is_simple = simple_switch.isChecked
            settingsData.is_mention = mention_switch.isChecked
            settingsData.is_thumbnail = thumb_switch.isChecked
            if (thumb_type_group.checkedRadioButtonId == R.id.thumb_type_url) {
                settingsData.thumbnail_type = 0
            } else {
                settingsData.thumbnail_type = 1
            }
            settingsData.is_channel = ch_switch.isChecked
            settingsData.is_api = api_switch.isChecked
            settingsData.api_key = api_key.text.toString()

            // ファイルに出力
            Log.d("APP DEBUG LOG", "filesDir:"+applicationContext.filesDir)
            val settings_json = Gson().toJson(settingsData)
            val fileName = "settings.json"
            val file = File(applicationContext.filesDir, fileName)
            try {
                FileWriter(file).use { writer -> writer.write(settings_json) }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Log.d("APP DEBUG LOG", "Save settings done")
            Toast.makeText(applicationContext, "設定を変更しました!", Toast.LENGTH_SHORT).show()
            // アニメーションしながらfinish
            finishAfterTransition()
        }
    }
}