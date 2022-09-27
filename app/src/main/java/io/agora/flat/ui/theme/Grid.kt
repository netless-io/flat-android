package io.agora.flat.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GRID_BASE: Dp = 16.dp
val GRID_0_25: Dp = GRID_BASE * 0.25f
val GRID_0_50: Dp = GRID_BASE * 0.50f
val GRID_0_75: Dp = GRID_BASE * 0.75f
val GRID_1_00: Dp = GRID_BASE * 1
val GRID_1_50: Dp = GRID_BASE * 1.5f
val GRID_2_00: Dp = GRID_BASE * 2

@Immutable
class Grids(
    val grid_0_25: Dp = 4.dp,
    val grid_0_50: Dp = 8.dp,
    val grid_0_75: Dp = 12.dp,
    val grid_1_00: Dp = 16.dp,
    val grid_1_50: Dp = 24.dp,
    val grid_2_00: Dp = 32.dp,
) {
    fun copy(
        grid_0_25: Dp = this.grid_0_25,
        grid_0_50: Dp = this.grid_0_50,
        grid_0_75: Dp = this.grid_0_75,
        grid_1_00: Dp = this.grid_1_00,
        grid_1_50: Dp = this.grid_1_50,
        grid_2_00: Dp = this.grid_2_00,
    ): Grids = Grids(
        grid_0_25 = grid_0_25,
        grid_0_50 = grid_0_50,
        grid_0_75 = grid_0_75,
        grid_1_00 = grid_1_00,
        grid_1_50 = grid_1_50,
        grid_2_00 = grid_2_00,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Grids) return false

        if (grid_0_25 != other.grid_0_25) return false
        if (grid_0_50 != other.grid_0_50) return false
        if (grid_0_75 != other.grid_0_75) return false
        if (grid_1_00 != other.grid_1_00) return false
        if (grid_1_50 != other.grid_1_50) return false
        if (grid_2_00 != other.grid_2_00) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid_0_25.hashCode()
        result = 31 * result + grid_0_50.hashCode()
        result = 31 * result + grid_0_75.hashCode()
        result = 31 * result + grid_1_00.hashCode()
        result = 31 * result + grid_1_50.hashCode()
        result = 31 * result + grid_2_00.hashCode()
        return result
    }

    override fun toString(): String {
        return "Grids(grid_0_25=$grid_0_25, grid_0_50=$grid_0_50, grid_0_75=$grid_0_75, grid_1_00=$grid_1_00, grid_1_50=$grid_1_50, grid_2_00=$grid_2_00)"
    }
}

internal val LocalGrids = staticCompositionLocalOf { Grids() }