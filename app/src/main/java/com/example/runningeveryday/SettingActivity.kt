package com.example.runningeveryday

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.runningeveryday.databinding.ActivitySettingBinding
import com.example.runningeveryday.databinding.DialogModifyAgeBinding
import com.example.runningeveryday.databinding.DialogModifySexBinding
import com.example.runningeveryday.databinding.DialogReallyWithdrawalBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class SettingActivity : AppCompatActivity() {

    private var viewBinding: ActivitySettingBinding? = null
    private val binding get() = viewBinding!!
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val googleAccount by lazy { GoogleSignIn.getLastSignedInAccount(this) }
    private val informationDocRef by lazy{ FirebaseFirestore.getInstance().collection("users")
        .document(firebaseAuth.uid!!).collection("information").document("information")}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setProfile()

        binding.backImageButton.setOnClickListener {
            finish()
        }

        binding.signOutTextView.setOnClickListener {
            signOut()
        }

        binding.modifySexTextView.setOnClickListener {
            ModifySexDialog().show()
        }

        binding.modifyAgeTextView.setOnClickListener {
            ModifyAgeDialog().show()
        }

        binding.accountWithdrawalTextView.setOnClickListener {
            WithdrawalAccountDialog().show()
        }

    }

    private fun signOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        val googleSignIn = GoogleSignIn.getClient(this, gso)
        googleSignIn.signOut()
        firebaseAuth.signOut()
        MainActivity.mainActivity?.finish()

        val intent = Intent(this, LoginActivity::class.java)
        finish()
        startActivity(intent)
    }

    private fun setProfile() {
        Glide.with(this).load(googleAccount?.photoUrl).into(binding.settingProfileImageView)
        binding.settingEmailImageView.text = googleAccount?.email
        binding.sexValueTextView.text = MainActivity.sex
        binding.ageValueTextView.text = MainActivity.age.toString()
    }

    inner class ModifySexDialog: Dialog(this) {

        private var sexViewBinding: DialogModifySexBinding? = null
        private val sexBinding get() = sexViewBinding!!

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            var sex = ""
            sexViewBinding = DialogModifySexBinding.inflate(layoutInflater)
            setContentView(sexBinding.root)

            window?.setBackgroundDrawableResource(R.drawable.round_dialog)
            when(MainActivity.sex) {
                "남" -> sexBinding.sexRadioGroup.check(R.id.man_radio_button)
                "여" -> sexBinding.sexRadioGroup.check(R.id.woman_radio_button)
            }

            sexBinding.modifySexButton.setOnClickListener {
                sex = when(sexBinding.sexRadioGroup.checkedRadioButtonId) {
                    R.id.man_radio_button-> {
                        binding.sexValueTextView.text = "남"
                        "남"
                    }
                    else ->  {
                        binding.sexValueTextView.text = "여"
                        "여"
                    }
                }
                informationDocRef.update(hashMapOf("sex" to sex) as Map<String, Any>)
                MainActivity.sex = sex
                Toast.makeText(context, "성별 정보가 수정되었습니다", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    inner class ModifyAgeDialog : Dialog(this) {

        private var ageViewBinding: DialogModifyAgeBinding? = null
        private val ageBinding get() = ageViewBinding!!
        private val calendar = Calendar.getInstance()
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            ageViewBinding = DialogModifyAgeBinding.inflate(layoutInflater)
            setContentView(ageBinding.root)

            window?.setBackgroundDrawableResource(R.drawable.round_dialog)

            ageBinding.modifyAgeNamePicker.apply {
                maxValue = calendar.get(Calendar.YEAR) - 10
                minValue = (calendar.get(Calendar.YEAR) - MainActivity.age).toInt()
                minValue = calendar.get(Calendar.YEAR) - 100
                wrapSelectorWheel = false
            }

            ageBinding.modifyAgeButton.setOnClickListener {
                val age = calendar.get(Calendar.YEAR) - ageBinding.modifyAgeNamePicker.value
                binding.ageValueTextView.text = age.toString()
                informationDocRef.update(hashMapOf("age" to age) as Map<String, Any>)
                MainActivity.age = age.toLong()
                Toast.makeText(context, "나이 정보가 수정되었습니다", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    inner class WithdrawalAccountDialog : Dialog(this) {

        private var withdrawalViewBinding: DialogReallyWithdrawalBinding? = null
        private val withdrawalBinding get() = withdrawalViewBinding!!

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            withdrawalViewBinding = DialogReallyWithdrawalBinding.inflate(layoutInflater)
            setContentView(withdrawalBinding.root)

            withdrawalBinding.withdrawalButton.setOnClickListener {
                val a  = withdrawalBinding.withdrawalEditText.text
                if(withdrawalBinding.withdrawalEditText.text.toString() == "계정탈퇴") {

                    FirebaseFirestore.getInstance().collection("users").document(firebaseAuth.uid!!).delete().addOnCompleteListener {
                        if(it.isSuccessful) {
                            firebaseAuth.currentUser?.delete()
                            MainActivity.mainActivity?.finish()
                            dismiss()
                            finish()
                            val intent = Intent(context, LoginActivity::class.java)
                            startActivity(intent)
                        }
                    }
                } else {
                    Toast.makeText(context, "텍스트가 일치하지 않아요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}