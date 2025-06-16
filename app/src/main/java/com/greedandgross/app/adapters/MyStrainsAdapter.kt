package com.greedandgross.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greedandgross.app.R
import com.greedandgross.app.models.SavedStrain

class MyStrainsAdapter(
    private val strains: List<SavedStrain>,
    private val onStrainClick: (SavedStrain) -> Unit
) : RecyclerView.Adapter<MyStrainsAdapter.StrainViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StrainViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_strain, parent, false)
        return StrainViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: StrainViewHolder, position: Int) {
        holder.bind(strains[position])
    }
    
    override fun getItemCount() = strains.size
    
    inner class StrainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val strainImage: ImageView = itemView.findViewById(R.id.strainImage)
        private val strainName: TextView = itemView.findViewById(R.id.strainName)
        private val strainParents: TextView = itemView.findViewById(R.id.strainParents)
        private val strainType: TextView = itemView.findViewById(R.id.strainType)
        
        fun bind(strain: SavedStrain) {
            strainName.text = strain.name.ifEmpty { "Strain Personalizzato" }
            strainParents.text = if (strain.parentStrain1.isNotEmpty() && strain.parentStrain2.isNotEmpty()) {
                "${strain.parentStrain1} × ${strain.parentStrain2}"
            } else {
                "Ibrido personalizzato"
            }
            strainType.text = strain.phenotype.ifEmpty { "Hybrid" }
            
            // Carica immagine strain
            if (strain.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(strain.imageUrl)
                    .placeholder(R.drawable.placeholder_strain)
                    .error(R.drawable.placeholder_strain)
                    .into(strainImage)
            } else {
                strainImage.setImageResource(R.drawable.placeholder_strain)
            }
            
            // Click listener con animazione
            cardView.setOnClickListener {
                cardView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        cardView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .withEndAction {
                                onStrainClick(strain)
                            }
                            .start()
                    }
                    .start()
            }
        }
    }
}