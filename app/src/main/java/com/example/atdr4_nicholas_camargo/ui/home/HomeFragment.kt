package com.example.atdr4_nicholas_camargo.ui.home

import android.Manifest
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atdr4_nicholas_camargo.R
import com.example.atdr4_nicholas_camargo.adapter.AdapterGenericoAnotacao
import com.example.atdr4_nicholas_camargo.adapter.ClasseAdapter
import com.example.atdr4_nicholas_camargo.cripto.CriptografadorDeFiles
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.ByteArrayOutputStream
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var mAuth: FirebaseAuth
    private val PICK_FROM_GALLERY = 1
    private var imgBArray: ByteArray? = null

    private var lat: String = ""
    private var lon: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        mAuth = FirebaseAuth.getInstance()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        ////////////////////////////////LE PERMISSIONS E FAZ ALGO BASEADO NISSO/////////////////
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 10
            )
        } else {
            readMyCurrentCoordinates()
        }


        ////////////////////////////SELECIONAR IMAGEM//////////////////////
        imageViewListagem.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivityForResult(intent, PICK_FROM_GALLERY)
            }
        }


        /////////////////////BTN INSERIR A ANOTACAO///////////////////////

        btnInserirAnotaçãoListagem.setOnClickListener {
            //faz a inserção seguindo todos os pedidos do AT
            val titulo = editTextTituloListagem.text.toString()
            val data = SimpleDateFormat("dd_MM_yyyy_HH_mm_ss").format(Date())
            val texto = editTextMultilineListagem.text.toString()

            ///////////////CONFERE PERMISSIONS CASO ALGUM BUG/////////////////////
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                /////////////PEDE PERMISSIONS CASO ALGUM BUG///////////////////////
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), 10
                )
            } else {

                ////////////////CONFERIR SE ESTA TUDO PREENCHIDO E INSERIR OS DADOS//////////////

                if (imgBArray == null || texto.isEmpty() || titulo.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Por favor preencha todos os campos, incluindo a imagem.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    /////////////////ULTIMO CHECK PARA VER SE LAT E LON FORAM ATUALIZADOS/////////////////
                    if (lat.isEmpty() || lon.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Erro em lat/lon",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        ////////////PREPARA TUDO PARA A INSERCAO
                        val prefixFile = "${titulo.toUpperCase(Locale.ROOT)}(${data})"

                        val nomeTxt = "$prefixFile.txt"
                        val nomeImg = "$prefixFile.fig"
                        CriptografadorDeFiles().gravarFile(
                            nomeTxt,
                            requireContext(),
                            listOf(lat, lon, texto)
                        )
                        Log.d("DEBUG DE IMG", imgBArray.toString())
                        CriptografadorDeFiles().gravarByteFile(
                            nomeImg,
                            requireContext(),
                            imgBArray!!
                        )

                        mostrarInserido(nomeTxt, nomeImg)
                    }

                }
            }
        }

    }


    ////////////////////////RCY VW MOSTRAR INSERIDO///////////////////////////


    private fun mostrarInserido(txt: String, img: String) {
        val txtFile = CriptografadorDeFiles().lerFileTxt(txt, requireContext())
        val imgFile = CriptografadorDeFiles().lerImg(img, requireContext())

        val data = txt.split("(")[1].removeSuffix(").txt")
        val texto = txtFile[2]
        val titulo = txt.split("(")[0]

        val bitmap = BitmapFactory.decodeByteArray(imgFile, 0, imgFile.size)

        val dados = ClasseAdapter(titulo, texto, data, bitmap)
        val adapter = AdapterGenericoAnotacao(listOf(dados))

        rcyVwAnotacaoUnica.adapter = adapter
        rcyVwAnotacaoUnica.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }


    ////////////////////////////////////////////REQUEST PERMISSION////////////////////////////


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED && grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    context,
                    "Não podemos continuar com a execução do app, por favor aceite o uso do GPS.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    ///////////////////////////////////////////ACTIVITY RESULT//////////////////////////////////


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_FROM_GALLERY -> {
                //pega os dados da Uri e converte para bitmap para alterar a imagem
                val uri: Uri? = data?.data
                val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                imageViewListagem.setImageBitmap(bitmap)

                //////////se prepara para inserir os dados da imagem////////////

                val stream = ByteArrayOutputStream()

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                imgBArray = byteArray
            }
        }
    }


    ///////////////////////////COORDEDANAS LAT LON///////////////////////
    private fun readMyCurrentCoordinates() {
        val locationManager =
            requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(
            LocationManager.GPS_PROVIDER
        )
        val isNetworkEnabled = locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
        if (!isGPSEnabled && !isNetworkEnabled) {
            Log.d("Permissao", "Ative os serviços necessários")
        } else {
            if (isGPSEnabled) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000L, 0f, locationListener
                    )
                } catch (ex: SecurityException) {
                    Log.d("Permissao", "Security Exception")
                }
            } else if (isNetworkEnabled) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2000L, 0f, locationListener
                    )
                } catch (ex: SecurityException) {
                    Log.d("Permissao", "Security Exception")
                }
            }
        }
    }


    ////////////////////location listener/////////////////////////////
    private val locationListener: LocationListener =
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lat = "${location.latitude}"
                lon = "${location.longitude}"
            }

            override fun onStatusChanged(
                provider: String, status: Int, extras: Bundle
            ) {
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

}
