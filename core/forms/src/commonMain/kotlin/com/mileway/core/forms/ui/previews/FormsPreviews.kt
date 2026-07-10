package com.mileway.core.forms.ui.previews

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.forms.FieldId
import com.mileway.core.forms.FormFieldValue
import com.mileway.core.forms.MockFormCatalog
import com.mileway.core.forms.defaultFormValues
import com.mileway.core.forms.ui.FormRenderer
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewSurface

/**
 * Renders the canned [MockFormCatalog.CONTEXT_EXPENSE] schema (text/number/currency/declaration +
 * the GST auto-calc row) through [FormRenderer] — a live sanity check that visibility/editability/
 * computed-fields wiring actually recomposes, without needing full Compose UI test infra.
 */
@PreviewLightDark
@Composable
private fun FormRendererExpensePreview() {
    val schema = MockFormCatalog.schemas.getValue(MockFormCatalog.CONTEXT_EXPENSE)
    var values by remember { mutableStateOf(defaultFormValues(schema)) }

    PreviewSurface {
        FormRenderer(
            schema = schema,
            values = values,
            onValueChange = { key: FieldId, value: FormFieldValue -> values = values + (key to value) },
            onReset = { values = defaultFormValues(schema) },
            modifier = Modifier.padding(16.dp),
        )
    }
}
