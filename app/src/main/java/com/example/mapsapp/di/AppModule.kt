package com.example.mapsapp.di

import android.content.Context
import com.example.mapsapp.repository.ChatRepository
import com.example.mapsapp.service.OpenRouterApi
import com.example.mapsapp.util.SecurePreferences
import com.example.mapsapp.webrtc.FirebaseClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Firebase
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    fun provideDatabaseReference(db: FirebaseDatabase): DatabaseReference = db.reference

    @Provides
    @Singleton
    fun provideFirebaseClient(): FirebaseClient = FirebaseClient()

    // Context & Preferences
    @Provides
    fun provideContext(@ApplicationContext context: Context): Context = context.applicationContext

    @Provides
    @Singleton
    fun provideSecurePreferences(@ApplicationContext context: Context): SecurePreferences {
        return SecurePreferences(context)
    }


    @Provides
    @Singleton
    fun provideChatRepository(
        db: FirebaseDatabase
    ): ChatRepository {
        return ChatRepository(db)
    }

    @Provides
    @Singleton
    @Named("GEMINI_API_KEY")
    fun provideGeminiApiKey(): String {
        return "AIzaSyAfGogBMgJpsrhaT6rFTV9gd3V3VlFpUss"
    }

    @Provides
    @Singleton
    fun provideOpenRouterApi(): OpenRouterApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer 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")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(OpenRouterApi::class.java)
    }






}
