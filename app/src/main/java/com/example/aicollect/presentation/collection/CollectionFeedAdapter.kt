package com.example.aicollect.presentation.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class CollectionFeedAdapter(
    private val items: List<CollectionFeedItem>,
    private val summary: CollectionSummary,
    private val onItemClick: (CollectionFeedItem) -> Unit = {},
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int =
        if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_FEED_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemTotalValueHeaderBinding.inflate(inflater, parent, false))
        } else {
            FeedItemViewHolder(ItemCollectionFeedBinding.inflate(inflater, parent, false), onItemClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.bind(summary)
        } else if (holder is FeedItemViewHolder) {
            holder.bind(items[position - 1])
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
