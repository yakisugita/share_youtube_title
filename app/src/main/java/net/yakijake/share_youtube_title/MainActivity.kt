package net.yakijake.share_youtube_title

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var settingsData = Settings()

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                Log.d("MainActivity", "RESULT_OK")
                // 設定を変更してたら再読み込み
                loadSettings()
            } else {
                Log.d("MainActivity", "NOT RESULT_OK")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // activity_toolbar_sample.xml からToolbar要素を取得
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        // アクションバーにツールバーをセット
        setSupportActionBar(toolbar)
        // ツールバーに戻るボタンを設置
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 設定読み込み
        loadSettings()

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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // intentが更新されたら
        if (intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                setIntent(intent)
                handleSendText(intent) // Handle text being sent
            }
        }
    }

    // ActionBarのボタンが押されたとき
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
//            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@MainActivity).toBundle())


            intent.putExtra("isSimple", settingsData.isSimple)
            intent.putExtra("isMention", settingsData.isMention)
            intent.putExtra("isChannel", settingsData.isChannel)
            intent.putExtra("isThumbnail", settingsData.isThumbnail)
            intent.putExtra("thumbnailType", settingsData.thumbnailType)
            intent.putExtra("isApi", settingsData.isApi)
            intent.putExtra("apiKey", settingsData.apiKey)

            getResult.launch(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this@MainActivity))

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
            findViewById<EditText>(R.id.textUri).setText(it)
            startGetRequest()
        }
    }

    // OkHttpClientを作成
    private val client = OkHttpClient.Builder()
        .connectTimeout(5000.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(5000.toLong(), TimeUnit.MILLISECONDS)
        .build()

    private fun startGetRequest() {
        // サブスレッドじゃなくてメインスレッドで実行できるようにするやつ
        val handler = Handler(Looper.getMainLooper())
        val videoId = findViewById<EditText>(R.id.textUri).text.toString().replace("https://youtu.be/","")
        val requestUrl :String = if (settingsData.isApi) {
            "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=$videoId&key=${settingsData.apiKey}"
        } else {
            "https://yakijake.net/products/share_youtube_title/via.php?id=$videoId"
        }
        // Requestを作成
        val request = Request.Builder()
            .url(requestUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // Responseの読み出し
//                val responseBody = response.body?.string().orEmpty()
                try {
                    if (response.code == 200) {
                        if (response.body != null) {
                            val jsonData = JSONObject(response.body?.string().toString())
                            val videoTitle: String
                            val channelTitle: String
                            val jsonVideoId: String
                            val thumbnailUrl: String
                            if (settingsData.isApi) {
                                val jsonItems = jsonData.getJSONArray("items")
                                val jsonItem = jsonItems.getJSONObject(0)
                                val jsonSnippet = jsonItem.getJSONObject("snippet")
                                videoTitle = jsonSnippet.getString("title")
                                channelTitle = jsonSnippet.getString("channelTitle")
                                jsonVideoId = jsonItem.getString("id")
                                thumbnailUrl = jsonSnippet.getJSONObject("thumbnails").getJSONObject("maxres").getString("url")
                            } else {
                                videoTitle = jsonData.getString("title")
                                channelTitle = jsonData.getString("channelTitle")
                                jsonVideoId = jsonData.getString("video_id")
                                thumbnailUrl = jsonData.getString("thumbnailMaxresUrl")
                            }
                            var shareText: String = videoTitle
                            if (settingsData.isChannel) {
                                shareText+=" / $channelTitle"
                            }
                            if (settingsData.isMention) {
                                shareText+=" @YouTubeより"
                            }
                            if (settingsData.isThumbnail) {
                                // サムネイル
                                if (settingsData.thumbnailType == 1) {
                                    //
                                } else {
                                    // URLとして共有
                                    shareText+="\n$thumbnailUrl"
                                }
                            }
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "$shareText\nhttps://youtu.be/$jsonVideoId")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)
                        } else {
                            Log.d("MainActivity", "response body is null")
                            handler.post {
                                Toast.makeText(applicationContext, "エラー:response bodyがnullです", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Log.d("MainActivity", "response code isn't 200")
                        // 「失敗したときに簡易動作にする」がONなら
                        if (settingsData.isSimple) {
                            try {
                                if (intent.action == Intent.ACTION_SEND) {
                                    if ("text/plain" == intent.type) {
                                        val videoTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                                        val videoUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
                                        Log.d("MainActivity", "Simple mode:Text:$videoTitle URL:$videoUrl")
                                        handler.post {
                                            Toast.makeText(applicationContext, "簡易動作しています", Toast.LENGTH_SHORT).show()
                                        }
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "$videoTitle\n$videoUrl")
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        startActivity(shareIntent)
                                    }
                                }
                            } catch (e:Exception) {
                                Log.d("MainActivity", "Simple mode error")
                                handler.post {
                                    Toast.makeText(applicationContext, "簡易動作を試しましたが失敗しました", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Log.d("MainActivity", "not simple mode")
                            handler.post {
                                Toast.makeText(applicationContext, "情報を取得できませんでした", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e:Exception) {
                    e.printStackTrace()
                    Log.d("MainActivity", "Response parse error!")
                    handler.post {
                        Toast.makeText(applicationContext, "エラーが発生しました(例外)", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("Error", e.toString())
                // 必要に応じてCallback
            }
        })


    }

    private fun loadSettings() {
        // ファイルから設定を読み込む
        var settingsJson: String?
        val fileName = "settings.json"
        val file = File(applicationContext.filesDir, fileName)
        try {
            BufferedReader(FileReader(file)).use { br -> settingsJson = br.readLine() }
            settingsData = Gson().fromJson<Settings>(settingsJson, settingsData::class.java) as Settings
            Log.d("MainActivity",
                "Load settings complete!\nSimple:${settingsData.isSimple}\nCh:${settingsData.isChannel}\nMention:${settingsData.isMention}\nThumb:${settingsData.isThumbnail}(Type:${settingsData.thumbnailType})\nApi:${settingsData.isApi}(key:${settingsData.apiKey})")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("MainActivity",
                "Load settings error!default:\nSimple:${settingsData.isSimple}\nCh:${settingsData.isChannel}\nMention:${settingsData.isMention}\nThumb:${settingsData.isThumbnail}(Type:${settingsData.thumbnailType})\nApi:${settingsData.isApi}(key:${settingsData.apiKey})")
        }
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