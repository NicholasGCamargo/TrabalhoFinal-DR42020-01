package com.example.atdr4_nicholas_camargo.cripto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CriptografadorDeFiles {
    val ks: KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getSecretKey(): SecretKey? {
        var chave: SecretKey? = null
        if (ks.containsAlias("chaveCripto")) {
            val entrada = ks.getEntry("chaveCripto", null) as?
                    KeyStore.SecretKeyEntry
            chave = entrada?.secretKey
        } else {
            val builder = KeyGenParameterSpec.Builder(
                "chaveCripto",
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
            val keySpec = builder.setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_PKCS7
                ).build()
            val kg = KeyGenerator.getInstance("AES", "AndroidKeyStore")
            kg.init(keySpec)
            chave = kg.generateKey()
        }
        return chave
    }

    fun cipher(original: String): ByteArray {
        val chave = getSecretKey()
        return cipher(original, chave)
    }

    fun cipher(original: String, chave: SecretKey?): ByteArray {
        if (chave != null) {
            Cipher.getInstance("AES/CBC/PKCS7Padding").run {
                init(Cipher.ENCRYPT_MODE, chave)
                val valorCripto = doFinal(original.toByteArray())
                val ivCripto = ByteArray(16)
                iv.copyInto(ivCripto, 0, 0, 16)
                return ivCripto + valorCripto
            }
        } else return byteArrayOf()
    }

    fun decipher(cripto: ByteArray): ByteArray {
        val chave = getSecretKey()
        return decipher(cripto, chave)
    }

    fun decipher(cripto: ByteArray, chave: SecretKey?): ByteArray {
        if (chave != null) {
            Cipher.getInstance("AES/CBC/PKCS7Padding").run {
                val ivCripto = ByteArray(16)
                val valorCripto = ByteArray(cripto.size - 16)
                cripto.copyInto(ivCripto, 0, 0, 16)
                cripto.copyInto(valorCripto, 0, 16, cripto.size)
                val ivParams = IvParameterSpec(ivCripto)
                init(Cipher.DECRYPT_MODE, chave, ivParams)
                return doFinal(valorCripto)
            }
        } else return byteArrayOf()
    }


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

    fun gravarFile(nome: String, context: Context, texto: List<ByteArray>) {
        val encryptedOut: FileOutputStream =
            getEncFile(nome, context).openFileOutput()
        val pw = PrintWriter(encryptedOut)
        texto.forEach {
            pw.println(it)
        }
        pw.flush()
        encryptedOut.close()
    }

    fun lerByte(nome: String, context: Context): List<ByteArray> {
        val encryptedIn: FileInputStream =
            getEncFile(nome, context).openFileInput()
        val br = BufferedReader(InputStreamReader(encryptedIn))
        val linha = br.readLines()
        encryptedIn.close()

        val result = mutableListOf<ByteArray>()
        linha.forEach {
            result.add(it.toByteArray())
        }
        return result
    }

    fun lerString(nome: String, context: Context): List<String> {
        val strings = lerByte(nome, context)
        val decifrados = mutableListOf<String>()
        strings.forEach {
            decifrados.add(decipher(it).toString())
        }

        return decifrados
    }

}