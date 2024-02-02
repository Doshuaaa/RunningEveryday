package com.example.runningeveryday

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.runningeveryday.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

object CheckNetwork {

    private lateinit var networkCallback: NetworkCallback
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var  networkDialog: AlertDialog
    fun registerNetworkCallback(context: Context, view: View) {
        networkDialog = AlertDialog.Builder(context).create()
        networkCallback  = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if(networkDialog.isShowing) {
                    view.post {
                        networkDialog.dismiss()
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                networkDialog.apply {
                    setMessage("네트워크 연결을 확인해주세요.")
                    setCancelable(false)
                }
                view.post {
                    networkDialog.show()
                }
            }
        }

        connectivityManager  = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkBuilder = NetworkRequest.Builder()
        connectivityManager.registerNetworkCallback(networkBuilder.build(), networkCallback)
    }
    fun unregisterNetworkCallback(view: View) {
        if(networkDialog.isShowing) {
            view.post {
                networkDialog.dismiss()
            }
        }
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

class LoginActivity : AppCompatActivity() {

    private var viewBinding: ActivityLoginBinding? = null
    private val binding get() = viewBinding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var mContext: Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        viewBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CheckNetwork.registerNetworkCallback(this, binding.root)

        setResultSingUp()
        auth.signOut()
        if(auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()

        binding.googleSignInButton.setOnClickListener {
            signIn()
        }

        (binding.googleSignInButton.getChildAt(0) as TextView).text = "구글로 로그인 하기"
    }

    private fun signIn() {
        val signIntent: Intent = googleSignInClient.signInIntent
        resultLauncher.launch(signIntent)
    }

    private fun setResultSingUp() {
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                firebaseAuthWithGoogle(task)
            }
        }
    }

    private fun firebaseAuthWithGoogle(task: Task<GoogleSignInAccount>) {
        val account = task.getResult(ApiException::class.java)
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if(it.isSuccessful) {
                initUid()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun initUid() {
        val fireStore = FirebaseFirestore.getInstance()
        var flag = false
        val collectionReference = fireStore.collection("users")
        collectionReference.get().addOnSuccessListener {task ->
            for(document in task.documents) {
                if(document.id == auth.uid) {
                    flag = true
                    break
                }
            }
        }

        if(!flag) {
            val emptyData = hashMapOf<String, Any>()
            collectionReference.document(auth.uid!!).set(emptyData)
        }
    }

}