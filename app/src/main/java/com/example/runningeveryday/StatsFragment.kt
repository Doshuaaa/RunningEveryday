package com.example.runningeveryday

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.runningeveryday.databinding.FragmentStatsBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        initViewPager()
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

//            registerOnPageChangeCallback(object : OnPageChangeCallback() {
//                override fun onPageSelected(position: Int) {
//                    super.onPageSelected(position)
//                }
//            })
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
}