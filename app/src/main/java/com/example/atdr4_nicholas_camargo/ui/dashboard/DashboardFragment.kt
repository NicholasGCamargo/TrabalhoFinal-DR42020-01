package com.example.atdr4_nicholas_camargo.ui.dashboard

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atdr4_nicholas_camargo.R
import com.example.atdr4_nicholas_camargo.adapter.AdapterGenericoAnotacao
import com.example.atdr4_nicholas_camargo.adapter.ClasseAdapter
import com.example.atdr4_nicholas_camargo.cripto.CriptografadorDeFiles
import kotlinx.android.synthetic.main.fragment_dashboard.*
import java.io.File

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        return root
    }

    override fun onStart() {
        super.onStart()

        rcyVwListagemTotal.adapter = AdapterGenericoAnotacao(atualizarRecycle())
        rcyVwListagemTotal.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    fun atualizarRecycle(): List<ClasseAdapter> {
        val location = File(requireContext().filesDir.toURI())
        var prefix = ""
        val dados = mutableListOf<ClasseAdapter>()
        val files = location.listFiles()

        files?.forEach {
            if ("$prefix.txt" != it.name && "$prefix.fig" != it.name) {
                prefix = it.name.removeSuffix(".txt")
                prefix = prefix.removeSuffix(".fig")

                dados.add(getDadosFile(prefix))
            }
        }

        return dados
    }

    fun getDadosFile(prefix: String): ClasseAdapter {
        var newPrefix = prefix.removeSuffix(".fig")
        newPrefix = newPrefix.removeSuffix(".txt")
        val imagem: ByteArray = CriptografadorDeFiles().lerImg("$newPrefix.fig", requireContext())
        val text: String = CriptografadorDeFiles().lerFileTxt("$newPrefix.txt", requireContext())[2]

        val bitmap = BitmapFactory.decodeByteArray(imagem, 0, imagem.size)

        return ClasseAdapter(
            prefix.split("(")[0],
            text,
            prefix.split("(")[1].removeSuffix(")"),
            bitmap
        )
    }
}
