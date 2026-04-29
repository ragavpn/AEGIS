package com.aegis.app.ui.viewmodel

import com.aegis.app.data.model.ChatMessage
import com.aegis.app.data.model.ChatResponse
import com.aegis.app.data.model.Conversation
import com.aegis.app.data.repository.ChatRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeChatRepository : ChatRepositoryInterface {
    var shouldFailSendMessage = false
    var shouldFailClearHistory = false
    var responseContent = "Fake response"
    var createdConversationId = "conv-1"

    override suspend fun createConversation(title: String): Conversation {
        return Conversation(id = createdConversationId, title = title, created_at = "now")
    }

    override suspend fun getConversationHistory(conversationId: String): List<ChatMessage> {
        return emptyList()
    }

    override suspend fun sendMessage(conversationId: String, content: String): ChatResponse {
        if (shouldFailSendMessage) throw Exception("Failed to send")
        return ChatResponse(role = "assistant", content = responseContent)
    }

    override suspend fun clearHistory() {
        if (shouldFailClearHistory) throw Exception("Failed to clear")
    }

    override suspend fun registerDeviceToken(userId: String, token: String) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private lateinit var viewModel: ChatViewModel
    private lateinit var fakeRepo: FakeChatRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeChatRepository()
        viewModel = ChatViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendMessage success appends user message then assistant response`() = runTest {
        viewModel.sendMessage("Hello")
        
        // Before advance: User message + Loading placeholder
        val messagesBeforeIdle = viewModel.messages.value
        assertEquals(2, messagesBeforeIdle.size)
        assertEquals("user", messagesBeforeIdle[0].role)
        assertEquals("Hello", messagesBeforeIdle[0].content)
        assertEquals("assistant", messagesBeforeIdle[1].role)
        assertTrue(messagesBeforeIdle[1].isLoading)
        assertTrue(viewModel.isLoading.value)

        testDispatcher.scheduler.advanceUntilIdle()

        // After advance: User message + Real assistant response
        val messagesAfterIdle = viewModel.messages.value
        assertEquals(2, messagesAfterIdle.size)
        assertEquals("assistant", messagesAfterIdle[1].role)
        assertEquals("Fake response", messagesAfterIdle[1].content)
        assertFalse(messagesAfterIdle[1].isLoading)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendMessage failure replaces loading message with error message`() = runTest {
        fakeRepo.shouldFailSendMessage = true
        
        viewModel.sendMessage("Fail me")
        testDispatcher.scheduler.advanceUntilIdle()

        val messages = viewModel.messages.value
        assertEquals(2, messages.size)
        assertEquals("error", messages[1].role)
        // ChatViewModel shows a user-friendly message, not the raw exception text
        assertTrue(messages[1].content.contains("Failed to get response"))
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `clearHistory removes all messages and resets conversation`() = runTest {
        viewModel.sendMessage("Hello")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.messages.value.size)

        viewModel.clearHistory()
        // Allow clearHistory coroutine + re-init coroutine to complete
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.messages.value.size)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `clearHistory failure sets error state`() = runTest {
        fakeRepo.shouldFailClearHistory = true
        viewModel.sendMessage("Hello")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        // Messages should be unchanged and error should be set
        assertEquals(2, viewModel.messages.value.size)
        assertTrue(viewModel.error.value?.contains("Failed to clear") == true)
    }
}
