package com.example.aicollect.presentation.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.aicollect.databinding.ItemCollectionFeedBinding
import com.example.aicollect.databinding.ItemTotalValueHeaderBinding

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_FEED_ITEM = 1

/** Header content for the "TOTAL COLLECTION VALUE" card, computed from real items — see
 * [com.example.aicollect.application.items.PortfolioAnalytics]. */
data class CollectionSummary(
    val totalValueLabel: String,
    val changeLabel: String?,
    val itemCountLabel: String,
)

/** One row of the Home feed: the summary header, or a collection item — a single sealed list so
 * [CollectionFeedAdapter] (a [ListAdapter]) can diff the whole feed in one shot instead of the
 * Fragment rebuilding the adapter from scratch on every state emission. */
sealed interface FeedRow {
    data class Header(val summary: CollectionSummary) : FeedRow
    data class ItemRow(val item: CollectionFeedItem) : FeedRow
}

private object FeedRowDiffCallback : DiffUtil.ItemCallback<FeedRow>() {
    override fun areItemsTheSame(oldItem: FeedRow, newItem: FeedRow): Boolean = when {
        oldItem is FeedRow.Header && newItem is FeedRow.Header -> true
        oldItem is FeedRow.ItemRow && newItem is FeedRow.ItemRow -> oldItem.item.id == newItem.item.id
        else -> false
    }

    override fun areContentsTheSame(oldItem: FeedRow, newItem: FeedRow): Boolean = oldItem == newItem
}

class CollectionFeedAdapter(
    private val onItemClick: (CollectionFeedItem) -> Unit = {},
) : ListAdapter<FeedRow, RecyclerView.ViewHolder>(FeedRowDiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is FeedRow.Header) VIEW_TYPE_HEADER else VIEW_TYPE_FEED_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemTotalValueHeaderBinding.inflate(inflater, parent, false))
        } else {
            FeedItemViewHolder(ItemCollectionFeedBinding.inflate(inflater, parent, false), onItemClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is FeedRow.Header -> (holder as HeaderViewHolder).bind(row.summary)
            is FeedRow.ItemRow -> (holder as FeedItemViewHolder).bind(row.item)
        }
    }

    private class HeaderViewHolder(private val binding: ItemTotalValueHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(summary: CollectionSummary) {
            binding.tvTotalValueAmount.text = summary.totalValueLabel
            binding.tvTotalValueItemCount.text = summary.itemCountLabel
            binding.rowTotalValueChange.visibility = if (summary.changeLabel != null) View.VISIBLE else View.GONE
            binding.tvTotalValueChange.text = summary.changeLabel.orEmpty()
        }
    }

    private class FeedItemViewHolder(
        private val binding: ItemCollectionFeedBinding,
        private val onItemClick: (CollectionFeedItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CollectionFeedItem) {
            binding.tvCategory.text = item.category
            binding.ivItem.load(item.imageUrl)
            binding.tvPrice.text = item.priceLabel
            binding.tvDescription.text = item.description
            binding.tvDate.text = item.dateLabel
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

/** Builds the flat [FeedRow] list an [CollectionFeedAdapter] diffs against — header always first. */
fun buildFeedRows(items: List<CollectionFeedItem>, summary: CollectionSummary): List<FeedRow> =
    listOf(FeedRow.Header(summary)) + items.map { FeedRow.ItemRow(it) }
