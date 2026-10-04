package com.mileway

import android.content.Context
import com.mileway.feature.tracking.manager.LocationTrackingController
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class LocationTrackingControllerTest {
    @Test
    fun `blank START tokens never dispatch a service`() {
        val context = mockk<Context>()
        val controller = LocationTrackingController(context)

        controller.start("")
        controller.start(" \t\n")

        verify { context wasNot Called }
    }
}
