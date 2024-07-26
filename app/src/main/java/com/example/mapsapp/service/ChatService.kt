import com.example.mapsapp.model.ChatRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import okhttp3.ResponseBody

interface ChatService {
    @Headers("Content-Type: application/json")
    @POST("/chat")
    @Streaming
    fun sendMessage(@Body request: ChatRequest): Call<ResponseBody>

    @POST("/reset")
    fun resetMemory(@Body request: ChatRequest): Call<Void>
}
