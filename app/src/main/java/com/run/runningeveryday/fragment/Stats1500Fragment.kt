package com.run.runningeveryday.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.run.runningeveryday.MainActivity
import com.run.runningeveryday.R
import com.run.runningeveryday.model.Record
import com.run.runningeveryday.adapter.RecordAdapter
import com.run.runningeveryday.databinding.FragmentStats1500Binding
import com.run.runningeveryday.dialog.LoadingDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Stats1500Fragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class Stats1500Fragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var viewBinding: FragmentStats1500Binding? = null
    private val binding get() = viewBinding!!
    private lateinit var top10List  : List<Pair<String, Any>>
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var mContext: Context
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

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
        loadingDialog = LoadingDialog(requireContext())
        loadingDialog.show()
        viewBinding = FragmentStats1500Binding.inflate(layoutInflater)

        setTop10List()

        return binding.root
    }

    private fun setTop10List() {

        val fireStore = FirebaseFirestore.getInstance()
        val top10Reference = fireStore.collection("users")
            .document(FirebaseAuth.getInstance().uid!!)
            .collection("top10").document("1500")

        top10Reference.get().addOnSuccessListener {snapShot ->
            if(snapShot.data != null) {
                val list = snapShot.data?.toList()!!
                top10List = list.sortedBy { it.second as Long }
            }
            setView()
        }
    }

    private fun setView() {

        try {
            binding.notExistDataLinearLayout.visibility = View.GONE
            binding.existDataLinearLayout.visibility = View.VISIBLE

            when (Record().getGrade(
                MainActivity.sex,
                MainActivity.age,
                1500,
                top10List[0].second as Long
            )) {

                1 ->  {
                    binding.gradeImageView.setImageResource(R.drawable.first)
                    binding.gradeTextView.text = "1급"
                }

                2 -> {
                    binding.gradeImageView.setImageResource(R.drawable.second)
                    binding.gradeTextView.text = "2급"
                }

                3 -> {
                    binding.gradeImageView.setImageResource(R.drawable.third)
                    binding.gradeTextView.text = "3급"
                }

                4 -> {
                    binding.gradeImageView.setImageResource(R.drawable.ore)
                    binding.gradeTextView.text = "노력이 필요해"
                }

            }

            binding.dateTextView.text = top10List[0].first
            binding.timeTextView.text = String.format(
                Locale.KOREA,
                "%02d : %02d",
                (top10List[0].second as Long / 60),
                (top10List[0].second as Long % 60)
            )

            val decoration = DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL)
            binding.top10Of1500RecyclerView.apply {
                adapter = RecordAdapter(top10List, 1500)
                layoutManager = LinearLayoutManager(mContext)
                addItemDecoration(decoration)
            }
        } catch (e: UninitializedPropertyAccessException) {
            binding.notExistDataLinearLayout.visibility = View.VISIBLE
            binding.existDataLinearLayout.visibility = View.GONE

            binding.goToMeasureButton.setOnClickListener {

                MainActivity.setFragment(MeasureFragment())
            }
        }
        loadingDialog.dismiss()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Stats1500Fragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Stats1500Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}