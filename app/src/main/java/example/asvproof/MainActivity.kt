package example.asvproof

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import example.asvproof.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    // 创建一个用于更新UI的Handler
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.button)
        val textView: TextView = findViewById(R.id.textView)
        button.setOnClickListener {
            sendRequest(textView)
        }
    }

    private fun sendRequest(textView: TextView) {
        val request = Request.Builder()
            .url("https://www.baidu.com")
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 将错误信息显示在TextView上
                handler.post {
                    textView.text = "请求失败: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    // 将响应的主体显示在TextView上
                    handler.post {
                        textView.text = "请求成功: $bodyString"
                    }
                } else {
                    // 将错误信息显示在TextView上
                    handler.post {
                        textView.text = "服务器错误: ${response.code}"
                    }
                }
            }
        })
    }
}

