package io.github.takusan23.tatimidroid.nicovideo.adapter

import android.widget.Filter

// キャッシュの並び替えのFilter（わからん）。AutoCompleteTextViewで全て表示させるために参考にした：https://qiita.com/wa2c/items/2bf9172543ca29af76bc
class AllShowDropDownMenuFilter : Filter() {
    override fun performFiltering(constraint: CharSequence?): FilterResults {
        return FilterResults()
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {

    }
}