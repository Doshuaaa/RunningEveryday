package com.run.runningeveryday.fragment

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.run.runningeveryday.LoginActivity
import com.run.runningeveryday.MainActivity
import com.run.runningeveryday.R
import com.run.runningeveryday.databinding.DialogModifyAgeBinding
import com.run.runningeveryday.databinding.DialogModifySexBinding
import com.run.runningeveryday.databinding.DialogReallyWithdrawalBinding
import com.run.runningeveryday.databinding.FragmentSettingBinding
import com.run.runningeveryday.service.MeasureService
import java.util.Calendar

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var viewBinding: FragmentSettingBinding? = null
    private val binding get() = viewBinding!!
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val googleAccount by lazy { GoogleSignIn.getLastSignedInAccount(requireContext()) }
    private val informationDocRef by lazy{ FirebaseFirestore.getInstance().collection("users")
        .document(firebaseAuth.uid!!).collection("information").document("information")}
    private val fireStore = FirebaseFirestore.getInstance()

    private val deleteRefList = listOf(
        fireStore.collection("users").document(firebaseAuth.uid!!).collection("information"),
        fireStore.collection("users").document(firebaseAuth.uid!!).collection("top10")
    )
    val recordRef = fireStore.collection("users").document(firebaseAuth.uid!!).collection("record")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewBinding = FragmentSettingBinding.inflate(layoutInflater)

        setProfile()

        binding.signOutTextView.setOnClickListener {
            if(MeasureService.targetDistance == 0f) {
                signOut()
            }
            else {
                val dlg = AlertDialog.Builder(requireContext())
                dlg.apply {
                    setTitle("로그아웃 불가")
                    setMessage("측정중인 기록이 있어 로그아웃을 할 수 없어요.")
                    setPositiveButton("확인", null)
                }
                dlg.show()
            }
        }

        binding.modifySexTextView.setOnClickListener {
            ModifySexDialog().show()
        }

        binding.modifyAgeTextView.setOnClickListener {
            ModifyAgeDialog().show()
        }

        binding.pippTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.notion.so/62f2a6308d7249a2841d4e8749a7a1ff"))
            startActivity(intent)
        }

        binding.accountWithdrawalTextView.setOnClickListener {
            if(MeasureService.targetDistance == 0f) {
                WithdrawalAccountDialog().show()
            }
            else {
                val dlg = AlertDialog.Builder(requireContext())
                dlg.apply {
                    setTitle("계정탈퇴 불가")
                    setMessage("측정중인 기록이 있어 계정탈퇴를 할 수 없어요.")
                    setPositiveButton("확인", null)
                }
                dlg.show()
            }
        }
        return binding.root
    }

    private fun signOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        val googleSignIn = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignIn.signOut()
        firebaseAuth.signOut()
        MainActivity.mainActivity?.finish()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        activity?.finish()
        startActivity(intent)
        Toast.makeText(requireContext(), "로그아웃 되었습니다", Toast.LENGTH_SHORT).show()
    }

    private fun setProfile() {
        Glide.with(this).load(googleAccount?.photoUrl).into(binding.settingProfileImageView)
        binding.settingEmailImageView.text = googleAccount?.email
        binding.sexValueTextView.text = MainActivity.sex
        binding.ageValueTextView.text = MainActivity.age.toString()
    }

    inner class ModifySexDialog: Dialog(requireContext()) {

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

    inner class ModifyAgeDialog : Dialog(requireContext()) {

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

    inner class WithdrawalAccountDialog : Dialog(requireContext()) {

        private var withdrawalViewBinding: DialogReallyWithdrawalBinding? = null
        private val withdrawalBinding get() = withdrawalViewBinding!!

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            withdrawalViewBinding = DialogReallyWithdrawalBinding.inflate(layoutInflater)
            setContentView(withdrawalBinding.root)

            withdrawalBinding.withdrawalButton.setOnClickListener {
                if(withdrawalBinding.withdrawalEditText.text.toString() == "계정탈퇴") {

                    hideKeyboard()

                    recordRef.get().addOnSuccessListener {
                        for(document in it.documents) {
                            recordRef.document(document.id).get().addOnSuccessListener { date ->
                                val list = date.data?.entries?.toList()

                                if(list != null) {

                                    for(map in list) {

                                        recordRef.document(document.id).collection(map.key).document("1500").delete()
                                        recordRef.document(document.id).collection(map.key).document("3000").delete()
                                    }
                                }
                                recordRef.document(document.id).delete()
                            }
                        }
                    }

                    for(reference in deleteRefList) {
                        reference.get().addOnSuccessListener {
                            for (document in it.documents) {
                                reference.document(document.id).delete()
                            }
                        }
                    }

                    fireStore.collection("users").document(firebaseAuth.uid!!).delete().addOnSuccessListener {
                        firebaseAuth.currentUser?.delete()?.addOnCompleteListener {
                            if(it.isSuccessful) {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(getString(R.string.default_web_client_id))
                                    .requestEmail()
                                    .requestProfile()
                                    .build()
                                val googleSignIn = GoogleSignIn.getClient(requireActivity(), gso)
                                googleSignIn.signOut()
                                val dlg = AlertDialog.Builder(context)
                                dlg.apply {
                                    setMessage("계정이 삭제되었습니다")
                                    setPositiveButton("확인", DialogInterface.OnClickListener { _, _ ->
                                        dismiss()
                                        activity?.finish()
                                        val intent = Intent(context, LoginActivity::class.java)
                                        startActivity(intent)
                                    })
                                    show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "텍스트가 일치하지 않아요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun hideKeyboard() {
        val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}