package com.example.runningeveryday

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
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

    fun initNetworkLostDialog(context: Context)  {
        networkDialog = AlertDialog.Builder(context).create()
        networkDialog.apply {
            setMessage("네트워크 연결을 확인해주세요.")
            setCancelable(false)
        }
    }

    fun checkNetworkState(context: Context) : Boolean {

        val connectivityManager: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val actNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            actNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    fun showNetworkLostDialog(view: View) {

        if(!networkDialog.isShowing) {
            view.post {
                networkDialog.show()
            }
        }
    }

    fun registerActivityNetworkCallback(activity: Activity, view: View) {
        networkCallback  = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if(networkDialog.isShowing) {
                    view.post {
                        networkDialog.dismiss()
                    }
                    activity.finish()
                    activity.baseContext.startActivity(activity.intent)
                }
            }
        }

        connectivityManager  = activity.baseContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkBuilder = NetworkRequest.Builder()
        connectivityManager.registerNetworkCallback(networkBuilder.build(), networkCallback)
    }

    fun registerFragmentNetworkCallback(fragment: Fragment, view: View) {
        networkCallback  = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if(networkDialog.isShowing) {
                    view.post {
                        networkDialog.dismiss()
                        fragment.activity?.supportFragmentManager?.beginTransaction()?.detach(fragment)?.commit()
                        fragment.activity?.supportFragmentManager?.beginTransaction()?.attach(fragment)?.commit()
                        //MainActivity.setFragment(fragment)
                    }
                    //MainActivity.setFragment(fragment)
                }
            }
        }

        connectivityManager  = fragment.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        CheckNetwork.initNetworkLostDialog(this)

        if(!CheckNetwork.checkNetworkState(this)) {
            CheckNetwork.showNetworkLostDialog(binding.root)
        }
        CheckNetwork.registerActivityNetworkCallback(this, binding.root)

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

    override fun onDestroy() {
        super.onDestroy()
        //CheckNetwork.unregisterNetworkCallback(binding.root)
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
                finish()
                startActivity(intent)

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