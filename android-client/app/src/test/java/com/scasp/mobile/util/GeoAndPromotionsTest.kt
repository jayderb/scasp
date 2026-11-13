package com.scasp.mobile.util

import com.scasp.mobile.data.model.CampusServiceItem
import com.scasp.mobile.data.model.RegistrationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoAndPromotionsTest {
    private val lib = CampusServiceItem("LIB", "Main Library", "", -15.4167, 28.2833)
    private val clinic = CampusServiceItem("CLINIC", "Campus Clinic", "", -15.4180, 28.2820)

    @Test fun haversineDistances() {
        assertEquals(0.0, Geo.distanceMetres(-15.4, 28.3, -15.4, 28.3), 1e-6)
        assertEquals(111_195.0, Geo.distanceMetres(0.0, 0.0, 1.0, 0.0), 50.0)          // one degree of latitude
        assertEquals(200.0, Geo.distanceMetres(lib.latitude, lib.longitude, clinic.latitude, clinic.longitude), 10.0)
    }
    @Test fun nearestServiceAndFormatting() {
        assertEquals("Campus Clinic", Geo.nearest(-15.4181, 28.2819, listOf(lib, clinic))!!.first.name)
        assertNull(Geo.nearest(0.0, 0.0, emptyList()))
        assertEquals("850 m", Geo.formatDistance(850.4)); assertEquals("1.5 km", Geo.formatDistance(1500.0))
    }
    @Test fun geoUriForTheImplicitIntent() {
        assertEquals("geo:-15.4167,28.2833?q=-15.4167,28.2833(Main%20Library)", Geo.geoUri(lib))
        assertEquals("https://www.google.com/maps/search/?api=1&query=-15.4167,28.2833", Geo.webMapUrl(lib))
    }
    @Test fun promotionDetection() {
        val before = listOf(RegistrationItem("HCI308", "WAITLISTED", 1), RegistrationItem("CSC301", "REGISTERED"))
        val after = listOf(RegistrationItem("HCI308", "REGISTERED"), RegistrationItem("CSC301", "REGISTERED"))
        assertEquals(listOf("HCI308"), Promotions.newlyRegistered(before, after))
        assertEquals(emptyList<String>(), Promotions.newlyRegistered(emptyList(), after))
    }
}
