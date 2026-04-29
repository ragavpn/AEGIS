package com.aegis.app.ui.viewmodel

import com.aegis.app.data.model.Notification
import com.aegis.app.data.repository.NotificationRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeNotificationRepository : NotificationRepositoryInterface {
    var notificationsToReturn = listOf<Notification>()
    var shouldFailLoad = false
    val markedAsRead = mutableListOf<String>()

    override suspend fun getNotifications(): List<Notification> {
        if (shouldFailLoad) throw Exception("Failed to load")
        return notificationsToReturn
    }

    override suspend fun markAsRead(notificationId: String) {
        markedAsRead.add(notificationId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
    private lateinit var viewModel: NotificationViewModel
    private lateinit var fakeRepo: FakeNotificationRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeNotificationRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadNotifications success sets listState to Success`() = runTest {
        val mockNotes = listOf(
            Notification(id = "1", title = "T1", body = "B1", createdAt = "now"),
            Notification(id = "2", title = "T2", body = "B2", createdAt = "then")
        )
        fakeRepo.notificationsToReturn = mockNotes

        viewModel = NotificationViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.listState.value
        assertTrue(state is NotificationListState.Success)
        assertEquals(mockNotes, (state as NotificationListState.Success).notifications)
    }

    @Test
    fun `loadNotifications error sets listState to Error`() = runTest {
        fakeRepo.shouldFailLoad = true

        viewModel = NotificationViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.listState.value
        assertTrue(state is NotificationListState.Error)
        assertEquals("Failed to load", (state as NotificationListState.Error).message)
    }

    @Test
    fun `markAsRead updates repository and local state`() = runTest {
        val mockNotes = listOf(
            Notification(id = "1", title = "T1", body = "B1", createdAt = "now", isRead = false)
        )
        fakeRepo.notificationsToReturn = mockNotes

        viewModel = NotificationViewModel(fakeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.markAsRead("1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepo.markedAsRead.contains("1"))
        
        val state = viewModel.listState.value
        assertTrue(state is NotificationListState.Success)
        val updatedNotes = (state as NotificationListState.Success).notifications
        assertTrue(updatedNotes[0].isRead)
    }
}
