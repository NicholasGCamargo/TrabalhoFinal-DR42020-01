package com.example.atdr4_nicholas_camargo.cripto

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.*

class CriptografadorDeFiles {

    private fun getEncFile(nome: String, context: Context): EncryptedFile {
        val masterKeyAlias: String =
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val file: File =
            File(context.filesDir, nome)
        return EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        )
            .build()
    }

    fun gravarByteFile(nome: String, context: Context, img: ByteArray) {
        val encryptedOut: FileOutputStream =
            getEncFile(nome, context).openFileOutput()
        val pw = PrintWriter(encryptedOut)
        pw.println(img)
        pw.flush()
        encryptedOut.close()
    }


    fun gravarFile(nome: String, context: Context, texto: List<String>) {
        val encryptedOut: FileOutputStream =
            getEncFile(nome, context).openFileOutput()
        val pw = PrintWriter(encryptedOut)
        texto.forEach {
            pw.println(it)
        }
        pw.flush()
        encryptedOut.close()
    }

    fun lerImg(nome: String, context: Context): ByteArray {
        val lista: List<String> = lerFileTxt(nome, context)

        var bigString = ""
        lista.forEach {
            Log.d("Check", it)
            bigString += it
        }

        return Base64.decode(bigString, Base64.DEFAULT)
    }

    fun lerFileTxt(nome: String, context: Context): List<String> {
        val encryptedIn: FileInputStream =
            getEncFile(nome, context).openFileInput()
        val br = BufferedReader(InputStreamReader(encryptedIn))
        val linha = br.lines()

        val result = mutableListOf<String>()

        linha.forEach {
            result.add(it)
        }


        encryptedIn.close()
        return result
    }


}