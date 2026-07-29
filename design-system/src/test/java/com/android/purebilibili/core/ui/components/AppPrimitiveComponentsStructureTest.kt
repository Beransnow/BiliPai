package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AppPrimitiveComponentsStructureTest {

    @Test
    fun neutralPrimitiveApisDelegateToMaterialRenderersInsideDesignSystem() {
        val source = loadSource()

        assertTrue(source.contains("fun AppSurface("))
        assertTrue(source.contains(") = Surface("))
        assertTrue(source.contains("onClick = onClick"))
        assertTrue(source.contains("fun AppButton("))
        assertTrue(source.contains(") = Button("))
        assertTrue(source.contains("colors: ButtonColors"))
        assertTrue(source.contains("fun AppIconButton("))
        assertTrue(source.contains(") = IconButton("))
        assertTrue(source.contains("fun AppTextButton("))
        assertTrue(source.contains(") = TextButton("))
        assertTrue(source.contains("fun AppOutlinedTextField("))
        assertTrue(source.contains(") = OutlinedTextField("))
        assertTrue(source.contains("fun AppDropdownMenu("))
        assertTrue(source.contains("fun AppModalNavigationDrawer("))
        assertTrue(source.contains("fun AppNavigationDrawerItem("))
        assertTrue(source.contains(") = NavigationDrawerItem("))
        assertTrue(source.contains("fun AppCircularProgressIndicator("))
        assertTrue(source.contains(") = CircularProgressIndicator("))
        assertTrue(source.contains("fun AppLinearProgressIndicator("))
        assertTrue(source.contains(") = LinearProgressIndicator("))
        assertTrue(source.contains("fun AppCheckbox("))
        assertTrue(source.contains(") = Checkbox("))
        assertTrue(source.contains("fun AppSwitch("))
        assertTrue(source.contains(") = Switch("))
        assertTrue(source.contains("fun AppRadioButton("))
        assertTrue(source.contains(") = RadioButton("))
        assertTrue(source.contains("fun AppSlider("))
        assertTrue(source.contains(") = Slider("))
        assertTrue(source.contains("fun AppOutlinedButton("))
        assertTrue(source.contains(") = OutlinedButton("))
        assertTrue(source.contains("fun AppCard("))
        assertTrue(source.contains("content: @Composable ColumnScope.() -> Unit"))
        assertTrue(source.contains(") = Card("))
    }

    private fun loadSource(): String {
        val path = "src/main/java/com/android/purebilibili/core/ui/components/AppPrimitiveComponents.kt"
        return listOf(
            File(path),
            File("design-system/$path"),
        ).firstOrNull(File::exists)?.readText()
            ?: error("Cannot locate AppPrimitiveComponents.kt from ${File(".").absolutePath}")
    }
}
