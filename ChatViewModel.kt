package com.example.myai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = false,
    val error: String? = null,
    val isAnimated: Boolean = false // Simplified to track animation state
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val openRouterApiKey = "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"
    private var currentModel = "mistralai/mistral-7b-instruct"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return

        // Add user message immediately
        addUserMessage(userInput)

        // Set typing state
        _isTyping.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                callOpenRouter(userInput)
            } catch (e: Exception) {
                handleError(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    private fun addUserMessage(text: String) {
        _messages.update { currentMessages ->
            currentMessages + ChatMessage(
                text = text,
                isUser = true,
                isAnimated = true // User messages don't need animation
            )
        }
    }

    private fun callOpenRouter(userInput: String) {
        val messagesArray = buildMessagesArray(userInput)

        val requestBody = JSONObject().apply {
            put("model", currentModel)
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 1000)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $openRouterApiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", context.packageName)
            .addHeader("X-Title", "My AI App")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handleError("Network error: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "No error details"
                        handleError("API error ${response.code}: $errorBody")
                        return
                    }

                    try {
                        val body = response.body?.string() ?: run {
                            handleError("Empty response from server")
                            return
                        }

                        val jsonObject = JSONObject(body)
                        val responseMessage = parseResponse(jsonObject)

                        // Add AI message with isAnimated = false initially
                        addAiMessage(responseMessage)
                    } catch (e: Exception) {
                        handleError("Failed to parse response: ${e.localizedMessage}")
                    } finally {
                        _isTyping.value = false
                    }
                }
            }
        })
    }

    private fun addAiMessage(content: String) {
        println("Adding AI message: $content")
        _messages.update { currentMessages ->
            currentMessages + ChatMessage(
                text = content,
                isUser = false,
                isAnimated = false // Not animated yet
            )
        }
    }

    private fun buildMessagesArray(userInput: String): JSONArray {
        val recentMessages = _messages.value.takeLast(5)

        return JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a helpful AI assistant. Respond concisely and helpfully.")
            })

            recentMessages.forEach { message ->
                put(JSONObject().apply {
                    put("role", if (message.isUser) "user" else "assistant")
                    put("content", message.text)
                })
            }

            put(JSONObject().apply {
                put("role", "user")
                put("content", userInput)
            })
        }
    }

    private fun parseResponse(jsonObject: JSONObject): String {
        val choices = jsonObject.getJSONArray("choices")
        val message = choices.getJSONObject(0).getJSONObject("message")
        return message.getString("content").trim()
    }

    private fun handleError(error: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _isTyping.value = false

            _messages.update { currentMessages ->
                currentMessages + ChatMessage(
                    text = "⚠️ $error",
                    isUser = false,
                    error = error,
                    isAnimated = true // Error messages don't need animation
                )
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun markMessageAsAnimated(messageId: String) {
        _messages.update { currentMessages ->
            currentMessages.map { message ->
                if (message.id == messageId && !message.isAnimated) {
                    message.copy(isAnimated = true)
                } else {
                    message
                }
            }
        }
    }
}