package com.example.atdr4_nicholas_camargo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.atdr4_nicholas_camargo.R
import kotlinx.android.synthetic.main.rc_layout_generico.view.*

class AdapterGenericoAnotacao(val lista: List<ClasseAdapter>) :
    RecyclerView.Adapter<AdapterGenericoAnotacao.DadoViewHolder>() {
    class DadoViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val campoTitulo = v.txtVwTituloGenerico
        val campoTexto = v.txtVwTextoGenerico
        val campoImg = v.imgVwImgGenerico
        val campoData = v.txtVwDataGenerico
    }

    override fun getItemCount(): Int {
        return lista.size
    }

    override fun onBindViewHolder(holder: DadoViewHolder, position: Int) {
        val info = lista[position]
        holder.campoData.text = info.data
        holder.campoTexto.text = info.texto
        holder.campoTitulo.text = info.titulo
        holder.campoImg.setImageBitmap(info.img)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DadoViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.rc_layout_generico, parent, false)

        return DadoViewHolder(v)
    }
}