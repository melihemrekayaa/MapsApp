import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import okhttp3.ResponseBody

data class Message(val role: String, val content: String)
data class ChatRequest(val messages: List<Message>)

interface ChatService {
    @Headers("Content-Type: application/json")
    @POST("/chat")
    @Streaming
    fun sendMessage(@Body request: ChatRequest): Call<ResponseBody>
}
