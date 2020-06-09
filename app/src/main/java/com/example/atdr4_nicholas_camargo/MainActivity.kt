package com.example.atdr4_nicholas_camargo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()

        if (mAuth.currentUser != null) {
            //usuario esta logado
            startActivity(Intent(this, ActivityPrincipal::class.java))
        }
    }

    override fun onStart() {
        super.onStart()


        //usuario nao esta logado ainda
        btnCriarContaMain.setOnClickListener {
            val email: String = editTextEmailMain.text.toString()
            val password: String = editTextSenhaMain.text.toString()
            editTextSenhaMain.setText("")
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information
                        Log.d("Firebase", "createUserWithEmail:success")
                        Toast.makeText(
                            this, "Conta Criada.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else { // If sign in fails, display a message to the user.
                        Log.w(
                            "Firebase",
                            "createUserWithEmail:failure",
                            task.exception
                        )
                        Toast.makeText(
                            this, "Autenticação falhou.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        btnLoginMain.setOnClickListener {
            val email: String = editTextEmailMain.text.toString()
            val password: String = editTextSenhaMain.text.toString()
            editTextSenhaMain.setText("")
            editTextEmailMain.setText("")
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) { // Sign in success, update UI with the signed-in user's information
                        Log.d("Firebase", "signInWithEmail:success")
                        startActivity(Intent(this, ActivityPrincipal::class.java))
                    } else { // If sign in fails, display a message to the user.
                        Log.w(
                            "Firebase",
                            "signInWithEmail:failure",
                            task.exception
                        )
                        Toast.makeText(
                            this, "Autenticação falhou.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

        }
    }
}
