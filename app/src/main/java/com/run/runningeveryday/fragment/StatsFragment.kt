package com.run.runningeveryday.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.run.runningeveryday.CheckNetwork
import com.run.runningeveryday.MainActivity
import com.run.runningeveryday.R
import com.run.runningeveryday.model.Record
import com.run.runningeveryday.adapter.StandardAdapter
import com.run.runningeveryday.databinding.DialogStatsStandardBinding
import com.run.runningeveryday.databinding.FragmentStatsBinding
import com.google.android.material.tabs.TabLayoutMediator

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [StatsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StatsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var viewBinding: FragmentStatsBinding? = null
    private val binding get() = viewBinding!!
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

        viewBinding = FragmentStatsBinding.inflate(layoutInflater)
        if(!CheckNetwork.checkNetworkState(requireContext())) {
            CheckNetwork.showNetworkLostDialog(binding.root)
        }
        CheckNetwork.registerFragmentNetworkCallback(this, binding.root)
        initViewPager()

        binding.statsStandardInfoImageView.setOnClickListener {
            StatsStandardDialog().show()
        }

        return binding.root
    }

    private fun initViewPager() {
        val viewPager2Adapter = object : FragmentStateAdapter(this) {
            val fragmentList: ArrayList<Fragment> = ArrayList()

            override fun getItemCount(): Int {
                return fragmentList.size
            }

            override fun createFragment(position: Int): Fragment {
                return fragmentList[position]
            }

            fun addFragment(fragment: Fragment) {
                fragmentList.add(fragment)
                notifyItemInserted(fragmentList.size - 1)
            }
        }

        viewPager2Adapter.addFragment(Stats1500Fragment())
        viewPager2Adapter.addFragment(Stats3000Fragment())

        binding.statsViewPager2.apply {
            adapter = viewPager2Adapter
        }

        TabLayoutMediator(binding.statsTabLayout, binding.statsViewPager2) {tab, position ->
            when(position) {
                0 -> tab.text = "1.5 km"
                1 -> tab.text = "3.0 km"
            }
        }.attach()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment StatsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StatsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    inner class StatsStandardDialog : Dialog(requireContext()) {


        private var dialogViewBinding: DialogStatsStandardBinding? = null
        private val dialogBinding get() = dialogViewBinding!!

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            dialogViewBinding = DialogStatsStandardBinding.inflate(layoutInflater)
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawableResource(R.drawable.round_dialog)
            val list1500 = Record().getGradeStandard(MainActivity.sex, MainActivity.age, 1500)
            val list3000 = Record().getGradeStandard(MainActivity.sex, MainActivity.age, 3000)

            dialogBinding.statsStandard1500RecyclerView.apply {
                adapter = StandardAdapter(list1500)
                layoutManager = LinearLayoutManager(mContext)
            }

            dialogBinding.statsStandard3000RecyclerView.apply {
                adapter = StandardAdapter(list3000)
                layoutManager = LinearLayoutManager(mContext)
            }

            dialogBinding.dismissStandardDialog.setOnClickListener {
                dismiss()
            }
        }
    }
}


