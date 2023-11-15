/*
 * This file is based on or incorporates material from the projects
 *
 * [WheelPickerCompose](https://github.com/commandiron/WheelPickerCompose)
 * [snapper](https://github.com/chrisbanes/snapper)
 */
package io.agora.flat.ui.compose

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    count: Int,
    rowCount: Int,
    size: DpSize = DpSize(128.dp, 128.dp),
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
    content: @Composable LazyItemScope.(index: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState(startIndex)
    val isScrollInProgress = lazyListState.isScrollInProgress

    val snapperLayoutInfo = rememberLazyListSnapperLayoutInfo(lazyListState = lazyListState)

    LaunchedEffect(isScrollInProgress, count) {
        if (!isScrollInProgress) {
            onScrollFinished(calculateSnappedItemIndex(snapperLayoutInfo) ?: startIndex)?.let {
                lazyListState.scrollToItem(it)
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (selectorProperties.enabled().value) {
            Surface(
                modifier = Modifier
                    .size(size.width, size.height / rowCount),
                shape = selectorProperties.shape().value,
                color = selectorProperties.color().value,
                border = selectorProperties.border().value
            ) {}
        }
        LazyColumn(
            modifier = Modifier
                .width(size.width)
                .height(size.height),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = size.height / rowCount * ((rowCount - 1) / 2)),
            flingBehavior = rememberSnapFlingBehavior(lazyListState)
        ) {
            items(count) { index ->
                val rotationX = calculateAnimatedRotationX(
                    lazyListState = lazyListState,
                    snapperLayoutInfo = snapperLayoutInfo,
                    index = index,
                    rowCount = rowCount
                )
                Box(
                    modifier = Modifier
                        .height(size.height / rowCount)
                        .width(size.width)
                        .alpha(
                            calculateAnimatedAlpha(
                                lazyListState = lazyListState,
                                snapperLayoutInfo = snapperLayoutInfo,
                                index = index,
                                rowCount = rowCount
                            )
                        )
                        .graphicsLayer {
                            this.rotationX = rotationX
                        },
                    contentAlignment = Alignment.Center
                ) {
                    content(index)
                }
            }
        }
    }
}

private fun calculateSnappedItemIndex(snapperLayoutInfo: SnapperLayoutInfo): Int? {
    var currentItemIndex = snapperLayoutInfo.currentItem?.index

    // if (snapperLayoutInfo.currentItem?.offset != 0) {
    //     if (currentItemIndex != null) {
    //         currentItemIndex++
    //     }
    // }
    return currentItemIndex
}

@Composable
private fun calculateAnimatedAlpha(
    lazyListState: LazyListState,
    snapperLayoutInfo: SnapperLayoutInfo,
    index: Int,
    rowCount: Int
): Float {

    val distanceToIndexSnap = snapperLayoutInfo.distanceToIndexSnap(index).absoluteValue
    val layoutInfo = remember { derivedStateOf { lazyListState.layoutInfo } }.value
    val viewPortHeight = layoutInfo.viewportSize.height.toFloat()
    val singleViewPortHeight = viewPortHeight / rowCount

    return if (distanceToIndexSnap in 0..singleViewPortHeight.toInt()) {
        1.2f - (distanceToIndexSnap / singleViewPortHeight)
    } else {
        0.2f
    }
}

@Composable
private fun calculateAnimatedRotationX(
    lazyListState: LazyListState,
    snapperLayoutInfo: SnapperLayoutInfo,
    index: Int,
    rowCount: Int
): Float {

    val distanceToIndexSnap = snapperLayoutInfo.distanceToIndexSnap(index)
    val layoutInfo = remember { derivedStateOf { lazyListState.layoutInfo } }.value
    val viewPortHeight = layoutInfo.viewportSize.height.toFloat()
    val singleViewPortHeight = viewPortHeight / rowCount
    val animatedRotationX = -20f * (distanceToIndexSnap / singleViewPortHeight)

    return if (animatedRotationX.isNaN()) {
        0f
    } else {
        animatedRotationX
    }
}

object WheelPickerDefaults {
    @Composable
    fun selectorProperties(
        enabled: Boolean = true,
        shape: Shape = RoundedCornerShape(16.dp),
        color: Color = MaterialTheme.colors.primary.copy(alpha = 0.2f),
        border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colors.primary),
    ): SelectorProperties = DefaultSelectorProperties(
        enabled = enabled,
        shape = shape,
        color = color,
        border = border
    )
}

interface SelectorProperties {
    @Composable
    fun enabled(): State<Boolean>

    @Composable
    fun shape(): State<Shape>

    @Composable
    fun color(): State<Color>

    @Composable
    fun border(): State<BorderStroke?>
}

@Immutable
internal class DefaultSelectorProperties(
    private val enabled: Boolean,
    private val shape: Shape,
    private val color: Color,
    private val border: BorderStroke?
) : SelectorProperties {

    @Composable
    override fun enabled(): State<Boolean> {
        return rememberUpdatedState(enabled)
    }

    @Composable
    override fun shape(): State<Shape> {
        return rememberUpdatedState(shape)
    }

    @Composable
    override fun color(): State<Color> {
        return rememberUpdatedState(color)
    }

    @Composable
    override fun border(): State<BorderStroke?> {
        return rememberUpdatedState(border)
    }
}

/**
 * Create and remember a [SnapperLayoutInfo] which works with [LazyListState].
 *
 * @param lazyListState The [LazyListState] to update.
 * @param snapOffsetForItem Block which returns which offset the given item should 'snap' to.
 * See [SnapOffsets] for provided values.
 */
@Composable
fun rememberLazyListSnapperLayoutInfo(
    lazyListState: LazyListState,
    snapOffsetForItem: (layoutInfo: SnapperLayoutInfo, item: LazyListItemInfo) -> Int = SnapOffsets.Center,
): LazyListSnapperLayoutInfo = remember(lazyListState, snapOffsetForItem) {
    LazyListSnapperLayoutInfo(
        lazyListState = lazyListState,
        snapOffsetForItem = snapOffsetForItem,
    )
}

object SnapOffsets {
    /**
     * Snap offset which results in the start edge of the item, snapping to the start scrolling
     * edge of the lazy list.
     */
    val Start: (SnapperLayoutInfo, LazyListItemInfo) -> Int =
        { layout, _ -> layout.startScrollOffset }

    /**
     * Snap offset which results in the item snapping in the center of the scrolling viewport
     * of the lazy list.
     */
    val Center: (SnapperLayoutInfo, LazyListItemInfo) -> Int = { layout, item ->
        layout.startScrollOffset + (layout.endScrollOffset - layout.startScrollOffset - item.size) / 2
    }

    /**
     * Snap offset which results in the end edge of the item, snapping to the end scrolling
     * edge of the lazy list.
     */
    val End: (SnapperLayoutInfo, LazyListItemInfo) -> Int = { layout, item ->
        layout.endScrollOffset - item.size
    }
}

/**
 * A [SnapperLayoutInfo] which works with [LazyListState]. Typically this would be remembered
 * using [rememberLazyListSnapperLayoutInfo].
 *
 * @param lazyListState The [LazyListState] to update.
 * @param snapOffsetForItem Block which returns which offset the given item should 'snap' to.
 * See [SnapOffsets] for provided values.
 */
class LazyListSnapperLayoutInfo(
    private val lazyListState: LazyListState,
    private val snapOffsetForItem: (layoutInfo: SnapperLayoutInfo, item: LazyListItemInfo) -> Int,
) : SnapperLayoutInfo() {

    /**
     * Lazy lists always use 0 as the start scroll offset (within content padding)
     */
    override val startScrollOffset: Int = 0

    /**
     * viewportEndOffset is the last visible offset, so we need to remove any end content padding
     * to get the end of the scroll range
     */
    override val endScrollOffset: Int
        get() = lazyListState.layoutInfo.let { it.viewportEndOffset - it.afterContentPadding }

    private val itemCount: Int get() = lazyListState.layoutInfo.totalItemsCount

    override val totalItemsCount: Int
        get() = lazyListState.layoutInfo.totalItemsCount

    override val currentItem: LazyListItemInfo? by derivedStateOf {
        visibleItems.lastOrNull { it.offset <= snapOffsetForItem(this, it) }
    }

    override val visibleItems: Sequence<LazyListItemInfo>
        get() = lazyListState.layoutInfo.visibleItemsInfo.asSequence()

    override fun distanceToIndexSnap(index: Int): Int {
        val itemInfo = visibleItems.firstOrNull { it.index == index }
        if (itemInfo != null) {
            // If we have the item visible, we can calculate using the offset. Woop.
            return itemInfo.offset - snapOffsetForItem(this, itemInfo)
        }

        // Otherwise we need to guesstimate, using the current item snap point and
        // multiplying distancePerItem by the index delta
        val currentItem = currentItem ?: return 0 // TODO: throw?
        return ((index - currentItem.index) * estimateDistancePerItem()).roundToInt() +
                currentItem.offset -
                snapOffsetForItem(this, currentItem)
    }

    override fun canScrollTowardsStart(): Boolean {
        return lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
            it.index > 0 || it.offset < startScrollOffset
        } ?: false
    }

    override fun canScrollTowardsEnd(): Boolean {
        return lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.let {
            it.index < itemCount - 1 || (it.offset + it.size) > endScrollOffset
        } ?: false
    }

    override fun determineTargetIndex(
        velocity: Float,
        decayAnimationSpec: DecayAnimationSpec<Float>,
        maximumFlingDistance: Float,
    ): Int {
        val curr = currentItem ?: return -1

        val distancePerItem = estimateDistancePerItem()
        if (distancePerItem <= 0) {
            // If we don't have a valid distance, return the current item
            return curr.index
        }

        val distanceToCurrent = distanceToIndexSnap(curr.index)
        val distanceToNext = distanceToIndexSnap(curr.index + 1)

        if (abs(velocity) < 0.5f) {
            // If we don't have a velocity, target whichever item is closer
            return when {
                distanceToCurrent.absoluteValue < distanceToNext.absoluteValue -> curr.index
                else -> curr.index + 1
            }.coerceIn(0, itemCount - 1)
        }

        // Otherwise we calculate using the velocity
        val flingDistance = decayAnimationSpec.calculateTargetValue(0f, velocity)
            .coerceIn(-maximumFlingDistance, maximumFlingDistance)
            .let { distance ->
                // It's likely that the user has already scrolled an amount before the fling
                // has been started. We compensate for that by removing the scrolled distance
                // from the calculated fling distance. This is necessary so that we don't fling
                // past the max fling distance.
                if (velocity < 0) {
                    (distance + distanceToNext).coerceAtMost(0f)
                } else {
                    (distance + distanceToCurrent).coerceAtLeast(0f)
                }
            }

        val flingIndexDelta = flingDistance / distancePerItem.toDouble()
        val currentItemOffsetRatio = distanceToCurrent / distancePerItem.toDouble()

        // The index offset from the current index. We round this value which results in
        // flings rounding towards the (relative) infinity. The key use case for this is to
        // support short + fast flings. These could result in a fling distance of ~70% of the
        // item distance (example). The rounding ensures that we target the next page.
        val indexOffset = (flingIndexDelta - currentItemOffsetRatio).roundToInt()

        return (curr.index + indexOffset).coerceIn(0, itemCount - 1)
            .also { result ->
                // SnapperLog.d {
                //     "determineTargetIndex. " +
                //             "result: $result, " +
                //             "current item: $curr, " +
                //             "current item offset: ${"%.3f".format(currentItemOffsetRatio)}, " +
                //             "distancePerItem: $distancePerItem, " +
                //             "maximumFlingDistance: ${"%.3f".format(maximumFlingDistance)}, " +
                //             "flingDistance: ${"%.3f".format(flingDistance)}, " +
                //             "flingIndexDelta: ${"%.3f".format(flingIndexDelta)}"
                // }
            }
    }

    /**
     * This attempts to calculate the item spacing for the layout, by looking at the distance
     * between the visible items. If there's only 1 visible item available, it returns 0.
     */
    private fun calculateItemSpacing(): Int = with(lazyListState.layoutInfo) {
        if (visibleItemsInfo.size >= 2) {
            val first = visibleItemsInfo[0]
            val second = visibleItemsInfo[1]
            second.offset - (first.size + first.offset)
        } else 0
    }

    /**
     * Computes an average pixel value to pass a single child.
     *
     * Returns a negative value if it cannot be calculated.
     *
     * @return A float value that is the average number of pixels needed to scroll by one view in
     * the relevant direction.
     */
    private fun estimateDistancePerItem(): Float = with(lazyListState.layoutInfo) {
        if (visibleItemsInfo.isEmpty()) return -1f

        val minPosView = visibleItemsInfo.minByOrNull { it.offset } ?: return -1f
        val maxPosView = visibleItemsInfo.maxByOrNull { it.offset + it.size } ?: return -1f

        val start = min(minPosView.offset, maxPosView.offset)
        val end = max(minPosView.offset + minPosView.size, maxPosView.offset + maxPosView.size)

        // We add an extra `itemSpacing` onto the calculated total distance. This ensures that
        // the calculated mean contains an item spacing for each visible item
        // (not just spacing between items)
        return when (val distance = end - start) {
            0 -> -1f // If we don't have a distance, return -1
            else -> (distance + calculateItemSpacing()) / visibleItemsInfo.size.toFloat()
        }
    }
}

/**
 * Contains the necessary information about the scrolling layout for [SnapperFlingBehavior]
 * to determine how to fling.
 */
abstract class SnapperLayoutInfo {
    /**
     * The start offset of where items can be scrolled to. This value should only include
     * scrollable regions. For example this should not include fixed content padding.
     * For most layouts, this will be 0.
     */
    abstract val startScrollOffset: Int

    /**
     * The end offset of where items can be scrolled to. This value should only include
     * scrollable regions. For example this should not include fixed content padding.
     * For most layouts, this will the width of the container, minus content padding.
     */
    abstract val endScrollOffset: Int

    /**
     * A sequence containing the currently visible items in the layout.
     */
    abstract val visibleItems: Sequence<LazyListItemInfo>

    /**
     * The current item which covers the desired snap point, or null if there is no item.
     * The item returned may not yet currently be snapped into the final position.
     */
    abstract val currentItem: LazyListItemInfo?

    /**
     * The total count of items attached to the layout.
     */
    abstract val totalItemsCount: Int

    /**
     * Calculate the desired target which should be scrolled to for the given [velocity].
     *
     * @param velocity Velocity of the fling. This can be 0.
     * @param decayAnimationSpec The decay fling animation spec.
     * @param maximumFlingDistance The maximum distance in pixels which should be scrolled.
     */
    abstract fun determineTargetIndex(
        velocity: Float,
        decayAnimationSpec: DecayAnimationSpec<Float>,
        maximumFlingDistance: Float,
    ): Int

    /**
     * Calculate the distance in pixels needed to scroll to the given [index]. The value returned
     * signifies which direction to scroll in:
     *
     * - Positive values indicate to scroll towards the end.
     * - Negative values indicate to scroll towards the start.
     *
     * If a precise calculation can not be found, a realistic estimate is acceptable.
     */
    abstract fun distanceToIndexSnap(index: Int): Int

    /**
     * Returns true if the layout has some scroll range remaining to scroll towards the start.
     */
    abstract fun canScrollTowardsStart(): Boolean

    /**
     * Returns true if the layout has some scroll range remaining to scroll towards the end.
     */
    abstract fun canScrollTowardsEnd(): Boolean
}

@Composable
@Preview
private fun WheelPickerPreview() {
    FlatPage {
        Box() {
            WheelPicker(count = 100, rowCount = 7) {
                Text(text = "Item $it")
            }
        }
    }
}