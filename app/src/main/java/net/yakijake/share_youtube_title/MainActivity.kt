package net.yakijake.share_youtube_title

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // activity_toolbar_sample.xml からToolbar要素を取得
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        // アクションバーにツールバーをセット
        setSupportActionBar(toolbar)
        // ツールバーに戻るボタンを設置
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val button = findViewById<Button>(R.id.get_button)
        button.setOnClickListener {
            startGetRequest()
        }

        if (intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                handleSendText(intent) // Handle text being sent
            }
        }
    }

    // ActionBarのボタンが押されたとき
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            Toast.makeText(applicationContext, "アプリ設定を開きます", Toast.LENGTH_SHORT).show()
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@MainActivity).toBundle())
            true
        }
        android.R.id.home -> {
            // Activity終了
            finish()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    // 右上にボタンを表示
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Toast.makeText(applicationContext, "共有されました:$it", Toast.LENGTH_SHORT).show()
        }
    }

    // OkHttpClientを作成
    private val client = OkHttpClient.Builder()
        .connectTimeout(5000.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(5000.toLong(), TimeUnit.MILLISECONDS)
        .build()

    private fun startGetRequest() {
        val videoId = findViewById<EditText>(R.id.textUri).text.toString().replace("https://youtu.be/","")
        // Requestを作成
        val request = Request.Builder()
            .url("https://yakijake.net/products/share_youtube_title/via.php?id=$videoId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // Responseの読み出し
//                val responseBody = response.body?.string().orEmpty()
                // 必要に応じてCallback
                if (response.body == null) {
                    Toast.makeText(applicationContext, "response bodyがnullです", Toast.LENGTH_SHORT).show()
                } else {
                    val jsonData = JSONObject(response.body?.string().toString())
                    val videoTitle: String = jsonData.getString("title")
                    val channelTitle: String = jsonData.getString("channelTitle")
                    val jsonVideoId: String = jsonData.getString("video_id")
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "$videoTitle / $channelTitle\nhttps://youtu.be/$jsonVideoId")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", e.toString())
                // 必要に応じてCallback
            }
        })
    }

    // publicになる
    class Settings {
        // publicのやつの下なら特に定義しなくてもpublicになる。
        var isChannel: Boolean = false
        var isMention: Boolean = false
        var isThumbnail: Boolean = false
        var thumbnailType: Int = 0
        var isApi: Boolean = false
        var apiKey: String = ""
        var isSimple: Boolean = false
    }
}