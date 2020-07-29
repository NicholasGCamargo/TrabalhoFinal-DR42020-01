package com.example.atdr4_nicholas_camargo.ui.dashboard

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.*
import com.example.atdr4_nicholas_camargo.R
import com.example.atdr4_nicholas_camargo.adapter.AdapterGenericoAnotacao
import com.example.atdr4_nicholas_camargo.adapter.ClasseAdapter
import com.example.atdr4_nicholas_camargo.cripto.CriptografadorDeFiles
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class DashboardFragment : Fragment(), BillingClientStateListener, SkuDetailsResponseListener, PurchasesUpdatedListener, ConsumeResponseListener {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var clienteInApp: BillingClient
    private var currentSku = "android.test.purchased"
    private var mapSku = HashMap<String,SkuDetails>()

    private val PREF_FILE  = "PREF_FILE"
    private val ID_USER_BUY = "foiComprado"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        MobileAds.initialize(requireContext())

        clienteInApp = BillingClient.newBuilder(requireContext())
            .enablePendingPurchases()
            .setListener(this)
            .build()

        clienteInApp.startConnection(this)

        mAuth = FirebaseAuth.getInstance()
        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        clienteInApp.endConnection()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adRequest = AdRequest.Builder().build()
        adView2.loadAd(adRequest)

        checkForIsPurchase()

        ////////////BOTAO SKU////////////
        buttonComprarApp.setOnClickListener {
            val skuDetails = mapSku[currentSku]
            val purchaseParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails).build()
            clienteInApp.launchBillingFlow(activity, purchaseParams)
        }
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

    override fun onBillingServiceDisconnected() {
        Log.d("COMPRA>>","Serviço InApp desconectado")
    }

    override fun onBillingSetupFinished(p0: BillingResult?) {
        if(p0?.responseCode == BillingClient.BillingResponseCode.OK){
            Log.d("<COMPROU>", "service inapp conected")
            val skuList = arrayListOf(currentSku)
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuList).setType(
                BillingClient.SkuType.INAPP
            )
            clienteInApp.querySkuDetailsAsync(params.build(), this)
        }
    }

    override fun onSkuDetailsResponse(p0: BillingResult?, p1: MutableList<SkuDetails>?) {
        if(p0?.responseCode == BillingClient.BillingResponseCode.OK){
            mapSku.clear()
            p1?.forEach {
                val preco = it.price
                val descricao = it.description
                mapSku[it.sku] = it

                Log.d("COMPRA>>",
                    "Produto Disponivel ($preco): $descricao")

            }
        }
    }

    override fun onPurchasesUpdated(p0: BillingResult?, p1: MutableList<Purchase>?) {
        if(p0?.responseCode == BillingClient.BillingResponseCode.OK && p1 != null) {
            for (compra in p1) {
                GlobalScope.launch(Dispatchers.IO) {
                    handlePurchase(compra)
                }
            }
        }else if (p0?.responseCode ==
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.d("COMPRA>>","Produto já foi comprado")
        } else if (p0?.responseCode ==
            BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d("COMPRA>>","Usuário cancelou a compra")
        } else {
            Log.d("COMPRA>>",
                "Código de erro desconhecido: ${p0?.responseCode}")
        }

    }

    suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Aqui acessaria a base e concederia o produto ao usuário
            Log.d("COMPRA>>", "Produto obtido com sucesso")

            /////Se eu comprei, insiro no shared pref do usuario////
            val editor: SharedPreferences.Editor =
                requireContext().getSharedPreferences(PREF_FILE, MODE_PRIVATE).edit()
            editor.putBoolean(ID_USER_BUY, true)
            editor.commit()

            adView2.setVisibility(View.GONE)
            buttonComprarApp.setVisibility(View.GONE)

            // Acknowledge -> Obrigatório para confirmação ao Google
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                val ackPurchaseResult = withContext(Dispatchers.IO) {
                    clienteInApp.acknowledgePurchase(
                        acknowledgePurchaseParams.build()
                    )
                }
            }
        }
    }


    override fun onConsumeResponse(p0: BillingResult?, p1: String?) {
        if(p0?.responseCode==
            BillingClient.BillingResponseCode.OK) {
            Log.d("COMPRA>>", "Produto Consumido")
        }
    }

    private fun checkForIsPurchase() {
        val preferences: SharedPreferences =
            requireContext().getSharedPreferences(PREF_FILE, MODE_PRIVATE)
        val isPurchase = preferences.getBoolean(ID_USER_BUY, false)
        if (isPurchase) {
            adView2.setVisibility(View.GONE)
            buttonComprarApp.setVisibility(View.GONE)
        }
    }
}
