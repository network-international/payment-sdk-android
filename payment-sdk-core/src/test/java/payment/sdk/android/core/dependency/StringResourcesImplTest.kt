package payment.sdk.android.core.dependency

import android.content.Context
import com.flextrade.jfixture.JFixture
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class StringResourcesImplTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var sut: StringResourcesImpl

    private val fixture = JFixture()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = StringResourcesImpl(mockContext)
    }

    @Test
    fun getString() {
        val fixtResourceId = fixture.create(Int::class.java)
        val fixtValue = fixture.create(String::class.java)
        whenever(mockContext.getString(fixtResourceId)).thenReturn(fixtValue)

        val actual = sut.getString(fixtResourceId)

        assertEquals(fixtValue, actual)
    }

    @Test
    fun getFormattedString() {
        val fixtResourceId = fixture.create(Int::class.java)
        val fixtFormat = fixture.create(String::class.java)
        val fixtValue = fixture.create(String::class.java)
        whenever(mockContext.getString(fixtResourceId, fixtFormat)).thenReturn(fixtValue)

        val actual = sut.getString(fixtResourceId, fixtFormat)

        assertEquals(fixtValue, actual)
    }
}