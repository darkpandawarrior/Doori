package com.mileway

import app.cash.turbine.test
import com.mileway.feature.agent.repository.AgentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentRepositoryTest {

    private fun repo() = AgentRepository(FakeAgentDao(), FakeAgentSessionStore())

    @Test
    fun `popularQuestions has exactly 12 entries`() {
        assertEquals(12, repo().popularQuestions.size)
    }

    @Test
    fun `unansweredQuestions has exactly 4 entries`() {
        assertEquals(4, repo().unansweredQuestions.size)
    }

    @Test
    fun `popular question ask counts are positive`() {
        repo().popularQuestions.forEach { q -> assertTrue(q.askCount > 0) }
    }

    @Test
    fun `trending popular questions count is 5`() {
        assertEquals(5, repo().popularQuestions.count { it.isTrending })
    }

    @Test
    fun `popular questions cover all four modules`() {
        val modules = repo().popularQuestions.map { it.module }.toSet()
        assertTrue("Mileage" in modules)
        assertTrue("Expense" in modules)
        assertTrue("Travel" in modules)
        assertTrue("Approvals" in modules)
    }

    @Test
    fun `mileage module has 4 popular questions`() {
        assertEquals(4, repo().popularQuestions.count { it.module == "Mileage" })
    }

    @Test
    fun `expense module has 4 popular questions`() {
        assertEquals(4, repo().popularQuestions.count { it.module == "Expense" })
    }

    @Test
    fun `unanswered questions have positive ask counts`() {
        repo().unansweredQuestions.forEach { q -> assertTrue(q.askCount > 0) }
    }

    @Test
    fun `seedIfEmpty seeds mock data on first run`() = runTest {
        val repo = repo()
        repo.seedIfEmpty()
        repo.conversationsFlow.test {
            assertEquals(8, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `seedIfEmpty is idempotent on second call`() = runTest {
        val repo = repo()
        repo.seedIfEmpty()
        repo.seedIfEmpty()
        repo.conversationsFlow.test {
            assertEquals(8, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `seeded conversations have unique ids`() = runTest {
        val repo = repo()
        repo.seedIfEmpty()
        repo.conversationsFlow.test {
            val list = awaitItem()
            val ids = list.map { it.id }
            assertEquals(ids.distinct(), ids)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `seeded conversations have non-blank titles`() = runTest {
        val repo = repo()
        repo.seedIfEmpty()
        repo.conversationsFlow.test {
            awaitItem().forEach { conv -> assertTrue(conv.title.isNotBlank()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createThread and appendMessage persist through Room`() = runTest {
        val repo = repo()
        repo.createThread("t1", "Test Thread", 1000L)
        repo.appendMessage("t1", "m1", "Hello", isUser = true, 1001L)
        repo.appendMessage("t1", "m2", "Hi there", isUser = false, 1002L)

        repo.messagesFor("t1").test {
            val msgs = awaitItem()
            assertEquals(2, msgs.size)
            assertEquals("Hello", msgs[0].text)
            assertTrue(msgs[0].isUser)
            assertEquals("Hi there", msgs[1].text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `conversationsFlow emits after createThread`() = runTest {
        val repo = repo()
        repo.conversationsFlow.test {
            assertEquals(0, awaitItem().size)
            repo.createThread("t1", "My Thread", 5000L)
            val after = awaitItem()
            assertEquals(1, after.size)
            assertEquals("My Thread", after.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getResumableThread returns thread id within 5-minute window`() = runTest {
        val repo = repo()
        repo.setActiveThread("t1", 1000L)
        val result = repo.getResumableThread(nowMs = 1000L + 4 * 60 * 1000L) // 4 min later
        assertEquals("t1", result)
    }

    @Test
    fun `getResumableThread returns null after 5-minute window`() = runTest {
        val repo = repo()
        repo.setActiveThread("t1", 1000L)
        val result = repo.getResumableThread(nowMs = 1000L + 6 * 60 * 1000L) // 6 min later
        assertEquals(null, result)
    }

    @Test
    fun `appendMessage updates lastMessageMs on the conversation`() = runTest {
        val repo = repo()
        repo.createThread("t1", "Thread", 100L)
        repo.appendMessage("t1", "m1", "msg", isUser = true, 9999L)

        repo.conversationsFlow.test {
            val conv = awaitItem().first()
            assertEquals(9999L, conv.lastMessageMs)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
