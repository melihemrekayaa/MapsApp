import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.40.1.248:5000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Bağlantı süresi aşımı (30 saniye)
        .writeTimeout(30, TimeUnit.SECONDS)    // Yazma süresi aşımı (30 saniye)
        .readTimeout(30, TimeUnit.SECONDS)     // Okuma süresi aşımı (30 saniye)
        .build()

    val instance: ChatService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ChatService::class.java)
    }
}
