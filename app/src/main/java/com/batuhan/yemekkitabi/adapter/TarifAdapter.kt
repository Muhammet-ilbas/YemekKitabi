package com.batuhan.yemekkitabi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.batuhan.yemekkitabi.databinding.RescyleRowBinding
import com.batuhan.yemekkitabi.model.Tarif
import com.batuhan.yemekkitabi.view.ListeFragmentDirections

class TarifAdapter(val tarifListi: List<Tarif>) : RecyclerView.Adapter<TarifAdapter.TarifHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarifHolder {
        val recyclerRowBinding: RescyleRowBinding = RescyleRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TarifHolder(recyclerRowBinding)
    }

    override fun onBindViewHolder(holder: TarifHolder, position: Int) {
        holder.recyclerRowBinding.recyclerViewTextView.text =  tarifListi[position].isim
        holder.itemView.setOnClickListener {
            val action = ListeFragmentDirections.actionListeFragmentToTarifFragment(bilgi = "eski", id = tarifListi[position].id)
            Navigation.findNavController(it).navigate(action)
        }
    }

    override fun getItemCount(): Int {
        return tarifListi.size
    }

    class TarifHolder(val recyclerRowBinding: RescyleRowBinding) : RecyclerView.ViewHolder(recyclerRowBinding.root) {

    }
}