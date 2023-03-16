package net.yakijake.share_youtube_title

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial
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
        // MainActivity側が現在の設定値を渡してくれるので、して取得して画面を現在の設定の状態にする
        Log.d("SettingsActivity", "Setup settings UI..")
        val simpleSwitch = findViewById<SwitchMaterial>(R.id.simple_switch)
        val mentionSwitch = findViewById<SwitchMaterial>(R.id.mention_switch)
        val thumbSwitch = findViewById<SwitchMaterial>(R.id.thumb_switch)
        val chSwitch = findViewById<SwitchMaterial>(R.id.ch_switch)
        val apiSwitch = findViewById<SwitchMaterial>(R.id.api_switch)
        val apiKey = findViewById<EditText>(R.id.api_key)

        simpleSwitch.isChecked = intent.getBooleanExtra("isSimple", false)
        mentionSwitch.isChecked = intent.getBooleanExtra("isMention", false)
        thumbSwitch.isChecked = intent.getBooleanExtra("isThumbnail", false)
        chSwitch.isChecked = intent.getBooleanExtra("isChannel", false)
        apiSwitch.isChecked = intent.getBooleanExtra("isApi", false)
        apiKey.setText(intent.getStringExtra("apiKey"))

        Log.d("SettingsActivity", "Setup UI done")


        val saveButton = findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            // 設定をファイルに保存
            Log.d("SettingsActivity", "Saving settings...")

            val settingsData = MainActivity.Settings()
            settingsData.isSimple = simpleSwitch.isChecked
            settingsData.isMention = mentionSwitch.isChecked
            settingsData.isThumbnail = thumbSwitch.isChecked
            settingsData.isChannel = chSwitch.isChecked
            settingsData.isApi = apiSwitch.isChecked
            settingsData.apiKey = apiKey.text.toString()

            // ファイルに出力
            Log.d("SettingsActivity", "filesDir:"+applicationContext.filesDir)
            val settingsJson = Gson().toJson(settingsData)
            val fileName = "settings.json"
            val file = File(applicationContext.filesDir, fileName)
            try {
                FileWriter(file).use { writer -> writer.write(settingsJson) }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Log.d("SettingsActivity", "Save settings done")
            Toast.makeText(applicationContext, "設定を変更しました!", Toast.LENGTH_SHORT).show()

            val intent = Intent()
            // OKだよーっていう値を入れてアクティビティを終了
            setResult(Activity.RESULT_OK, intent)

            // アニメーションしながらfinish
            finishAfterTransition()
        }
    }
}