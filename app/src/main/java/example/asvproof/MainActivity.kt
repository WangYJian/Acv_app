package example.asvproof

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okio.BufferedSink
import okio.buffer
import okio.source
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var webSocket: WebSocket? = null
    private var selectedFileUri: Uri? = null
    private val client = OkHttpClient()
    // textView对象
    private val textView: TextView by lazy {
        findViewById(R.id.textView)
    }

    // 用于启动文件选择活动的 ActivityResultLauncher
    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // 当用户选择了一个文件后，这个回调函数会被调用
        if (result.resultCode == Activity.RESULT_OK) {
            // 获取用户选择的文件的Uri
            val uri = result.data?.data
            if (uri != null) {
                // 用户选择了一个文件，保存Uri，然后打开WebSocket连接
                selectedFileUri = uri
                openWebSocketConnection()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            // 启动文件选择活动
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            // 选择所有文件类型
            intent.type = "*/*"
            selectFileLauncher.launch(intent)
        }
    }

    private fun openWebSocketConnection() {
        val request = Request.Builder().url("ws://10.0.2.2:8080/api/connect").build()

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 连接已打开，保存WebSocket实例
                this@MainActivity.webSocket = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val jsonObject = JSONObject(text)
                // 如果收到的消息是服务器的ID，那么就使用这个ID来上传文件
                if (jsonObject.has("id")) {
                    val id = jsonObject.getString("id")
                    if (selectedFileUri != null) {
                        uploadFile(id, selectedFileUri!!)
                    }
                } else {
                    // 如果收到的消息不是服务器的ID，那么就显示这个消息
                    runOnUiThread {
                        val message = jsonObject.getString("message")
                        textView.text = message.toString()
                    }
                }
            }
        }

        // 打开WebSocket连接
        client.newWebSocket(request, webSocketListener)
    }

    private fun uploadFile(id: String, uri: Uri) {
        // 通过ContentResolver和Uri获取文件的输入流
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream != null) {
            // 创建RequestBody
            val requestBody = object : RequestBody() {
                override fun contentType() = null

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.writeAll(inputStream.source())
                }
            }

            // 创建MultipartBody
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", id)
                .addFormDataPart("file", "filename", requestBody)
                .build()

            val request = Request.Builder()
                .url("http://10.0.2.2:8080/api/upload")
                .post(multipartBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    // 文件上传成功，你可以在这里处理响应
                    if (response.isSuccessful) {
                        runOnUiThread {
                            textView.text = "文件上传成功"
                        }
                    } else {
                        runOnUiThread {
                            textView.text = "文件上传失败"
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // 文件上传失败，你可以在这里处理错误
                    runOnUiThread {
                        textView.text = e.message
                    }
                }
            })
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 关闭 WebSocket 连接，以避免内存泄漏
        webSocket?.cancel()
    }
}


