package com.mileway

import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.db.VoucherCategoryConverters
import org.junit.Test
import kotlin.test.assertEquals

/**
 * P3.4: [VoucherCategoryConverters] is the Room [androidx.room.TypeConverter] pair that stores
 * [VoucherCategory] in the `vouchers.category` `TEXT` column via its label — these tests exercise
 * the exact `toLabel`/`fromLabel` functions Room invokes on write/read, proving every enum value
 * round-trips and that an unrecognized/legacy label falls back to [VoucherCategory.OTHER] instead
 * of crashing.
 */
class VoucherCategoryConverterTest {
    @Test
    fun `every VoucherCategory round-trips through the TypeConverter`() {
        VoucherCategory.entries.forEach { category ->
            val stored = VoucherCategoryConverters.toLabel(category)
            val restored = VoucherCategoryConverters.fromLabel(stored)
            assertEquals(category, restored)
        }
    }

    @Test
    fun `existing hardcoded category labels are preserved unchanged`() {
        assertEquals("Travel", VoucherCategoryConverters.toLabel(VoucherCategory.MILEAGE))
        assertEquals("Fuel", VoucherCategoryConverters.toLabel(VoucherCategory.FUEL))
        assertEquals("Maintenance", VoucherCategoryConverters.toLabel(VoucherCategory.MAINTENANCE))
        assertEquals("Other", VoucherCategoryConverters.toLabel(VoucherCategory.OTHER))
    }

    @Test
    fun `an unrecognized stored label falls back to OTHER instead of throwing`() {
        assertEquals(VoucherCategory.OTHER, VoucherCategoryConverters.fromLabel("Some Legacy Value"))
    }
}
