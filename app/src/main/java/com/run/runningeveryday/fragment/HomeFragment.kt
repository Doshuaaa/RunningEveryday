package com.run.runningeveryday.fragment

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.run.runningeveryday.CheckNetwork
import com.run.runningeveryday.R
import com.run.runningeveryday.SettingActivity
import com.run.runningeveryday.adapter.MonthAdapter
import com.run.runningeveryday.databinding.FragmentHomeBinding
import com.run.runningeveryday.dialog.LoadingDialog
import com.run.runningeveryday.model.Weather
import com.run.runningeverytime.api.WeatherApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private val retrofit = Retrofit.Builder()
    .baseUrl("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

object ApiObject {
    val retrofitService: WeatherApi by lazy {
        retrofit.create(WeatherApi::class.java)
    }
}
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var base_date = ""  // 발표일자
    private var base_time = "1400"      // 발표 시각
    private var curPoint: Point? = null

    private var viewBinding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding by lazy { viewBinding!! }

    private var streak = 0
    private var calPosition = 11

    private val auth = FirebaseAuth.getInstance()
    private val fireStore = FirebaseFirestore.getInstance()
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("y년 M월", Locale.KOREA)

    private val weatherHandler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        binding.lottiLoadingAnimation.cancelAnimation()
    }

    private val calRecyclerView by lazy { binding.calendarRecyclerView }
    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        CheckNetwork.initNetworkLostDialog(mContext)
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

        streak = 0
        viewBinding = FragmentHomeBinding.inflate(layoutInflater)
        if(!CheckNetwork.checkNetworkState(mContext)) {
            CheckNetwork.showNetworkLostDialog(binding.root)
        }
        CheckNetwork.registerFragmentNetworkCallback(this, binding.root)

        getWeather()
        initCalendar()
        getCurrentUserProfile()

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
//        loadingDialog.dismiss()
    }

    private fun getWeather() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener {
                if(it != null) {
                    val curLocation: Location = it
                    curPoint = dfs_xy_conv(curLocation.latitude, curLocation.longitude)
                    setWeather(curPoint!!.x.toString(), curPoint!!.y.toString() )
                }
            }

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun setWeather(nx : String, ny : String) {

        val cal = Calendar.getInstance()
        base_date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)

        val time = SimpleDateFormat("HH", Locale.getDefault()).format(cal.time) // 현재 시간
        base_time = getTime(time)
        // 동네예보  API는 3시간마다 현재시간+4시간 뒤의 날씨 예보를 알려주기 때문에
        // 현재 시각이 00시가 넘었다면 어제 예보한 데이터를 가져와야함
        if (base_time >= "2000") {

            cal.add(Calendar.DATE, -1)
            base_date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
        }

        val call = ApiObject.retrofitService.getWeather(
            "JSON", 10, 1, base_date, base_time, nx, ny)

        call.enqueue(object : retrofit2.Callback<Weather> {
            override fun onResponse(call: Call<Weather>, response: Response<Weather>) {
                if(response.isSuccessful) {
                    val list: List<Weather.Item> = response.body()!!.response.body.items.item

                    var rainRatio = ""      // 강수 확률
                    var rainType = ""       // 강수 형태
                    var sky = ""            // 하늘 상태
                    var temp = ""           // 기온

                    for(i in 0..9) {
                        when(list[i].category) {
                            "POP" -> rainRatio = list[i].fcstValue
                            "PTY" -> rainType = list[i].fcstValue
                            "SKY" -> sky = list[i].fcstValue
                            "TMP" -> temp = list[i].fcstValue
                        }
                    }
                    setWeather(rainRatio, rainType, sky, temp, time)
                }
            }

            override fun onFailure(call: Call<Weather>, t: Throwable) {

            }
        })
    }

    private fun setWeather(rainRatio: String, rainType: String, sky: String, temp: String, hour: String) {

        if(activity != null) {
            binding.popTextView.text = getString(R.string.pop, rainRatio)
        }
        var type = ""
        when(rainType) {
            "0" -> {
                type = "없음"
            }
            "1" -> {
                type = "비"
                binding.weatherImageView.setImageResource(R.drawable.rain)
            }
            "2", "3" -> {
                type = "눈"
                binding.weatherImageView.setImageResource(R.drawable.snow)
            }
            "4" -> {
                type = "소나기"
                binding.weatherImageView.setImageResource(R.drawable.shower)
            }
            else -> type = "error"
        }

        if(type == "없음") {
            when(sky) {
                "1" -> {
                    type = "맑음"
                    if(hour.toInt() in 6..18) {
                        binding.weatherImageView.setImageResource(R.drawable.sun)
                    }
                    else {
                        binding.weatherImageView.setImageResource(R.drawable.moon)
                    }
                }
                "3" -> {
                    type = "구름 많음"
                    if(hour.toInt() in 6..18) {
                        binding.weatherImageView.setImageResource(R.drawable.clouds_and_sun)
                    }
                    else {
                        binding.weatherImageView.setImageResource(R.drawable.clouds_and_moon)
                    }
                }
                "4" -> {
                    type = "흐림"
                    binding.weatherImageView.setImageResource(R.drawable.clouds)
                }
            }
        }

        if(activity != null) {
            binding.tempTextView.text = getString(R.string.temp, temp)
        }
        binding.weatherTextView.text = type

        val documentReference = fireStore.collection("users")
            .document(auth.uid!!).collection("record").document(SimpleDateFormat("yMM", Locale.KOREA).format(calendar.time))

        documentReference.get().addOnSuccessListener { task ->

            if(rainRatio.toInt() >= 60) {
                if(task.get(calendar.get(Calendar.DAY_OF_MONTH).toString()) == null) {

                    binding.skipRunButton.visibility = View.VISIBLE

                    binding.skipRunButton.setOnClickListener {

                       //weather 로딩 필요?

                        binding.skipRunButton.visibility = View.GONE

                        val streakCalendar = Calendar.getInstance()

                        val streakData = hashMapOf(
                            streakCalendar.get(Calendar.DAY_OF_MONTH).toString() to "streak"
                        )
                        if(task.data == null) {
                            documentReference.set(streakData as Map<String, Any>)
                        } else {
                            documentReference.update(streakData as Map<String, Any>)
                        }
                        //calRecyclerView.onFlingListener = null
                        initCalendar()
                        getStreak()
                    }
                }
            } else {
                binding.skipRunButton.visibility = View.GONE
            }

            weatherHandler.post(runnable)
        }
    }

    private fun getTime(time : String) : String{

        return when(time) {
            in "00".."02" -> "2000"
            in "03".."05" -> "2300"
            in "06".."08" -> "0200"
            in "09".."11" -> "0500"
            in "12".."14" -> "0800"
            in "15".."17" -> "1100"
            in "18".."20" -> "1400"
            else -> "1700"
        }

    }

    private fun dfs_xy_conv(v1: Double, v2: Double) : Point {
        val RE = 6371.00877     // 지구 반경(km)
        val GRID = 5.0          // 격자 간격(km)
        val SLAT1 = 30.0        // 투영 위도1(degree)
        val SLAT2 = 60.0        // 투영 위도2(degree)
        val OLON = 126.0        // 기준점 경도(degree)
        val OLAT = 38.0         // 기준점 위도(degree)
        val XO = 43             // 기준점 X좌표(GRID)
        val YO = 136            // 기준점 Y좌표(GRID)
        val DEGRAD = Math.PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        var sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn)
        var sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5)
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn
        var ro = Math.tan(Math.PI * 0.25 + olat * 0.5)
        ro = re * sf / Math.pow(ro, sn)

        var ra = Math.tan(Math.PI * 0.25 + (v1) * DEGRAD * 0.5)
        ra = re * sf / Math.pow(ra, sn)
        var theta = v2 * DEGRAD - olon
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn

        val x = (ra * Math.sin(theta) + XO + 0.5).toInt()
        val y = (ro - ra * Math.cos(theta) + YO + 0.5).toInt()

        return Point(x, y)
    }

    private fun initCalendar() {

        calRecyclerView.onFlingListener = null
        calRecyclerView.adapter = MonthAdapter(mContext)
        calRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        calRecyclerView.scrollToPosition(calPosition)
        PagerSnapHelper().attachToRecyclerView(calRecyclerView)
        setCalendar()

        binding.calendarLeftImageButton.setOnClickListener{
            calRecyclerView.scrollToPosition(--calPosition)
            calendar.add(Calendar.MONTH, -1)
            setCalendar()
        }

        binding.calendarRightImageButton.setOnClickListener {
            calRecyclerView.scrollToPosition(++calPosition)
            calendar.add(Calendar.MONTH, 1)
            setCalendar()
        }

        binding.calendarRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            var isLeft = false

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if(dx > 0) {

                    isLeft = false

                } else if( dx < 0) {

                    isLeft = true
                }
            }
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if(newState == RecyclerView.SCROLL_STATE_SETTLING && isLeft && calPosition > 0) {
                    calendar.add(Calendar.MONTH, -1)
                    --calPosition
                    setCalendar()
                } else if(newState == RecyclerView.SCROLL_STATE_SETTLING && !isLeft && calPosition < 11){
                    calendar.add(Calendar.MONTH, 1)
                    ++calPosition
                    setCalendar()
                }

            }
        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

    }

    private fun getCurrentUserProfile() {
        val curUser = GoogleSignIn.getLastSignedInAccount(requireContext())
        Glide.with(requireContext()).load(curUser?.photoUrl.toString()).into(binding.profileImgView)
        binding.profileTextView.text = getString(R.string.profile_name, curUser?.displayName)
        getStreak()
    }

    private fun getStreak() {
        streak = 0
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yMM", Locale.KOREA)

        val collectionReference = fireStore.collection("users")
            .document(auth.uid!!).collection("record")

        collectionReference.get().addOnSuccessListener {task ->

            val list = task.documents
            list.reverse()
            for(document in list) {

                if(calendar.get(Calendar.DAY_OF_MONTH) == 1 && dateFormat.format(calendar.time) != document.id) {
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                }

                if(dateFormat.format(calendar.time) == document.id) {
                    val dayList = document.data
                    if (streak == 0) {

                        if(dayList?.get(calendar.get(Calendar.DAY_OF_MONTH).toString()) != null) {
                            streak++
                        }
                        calendar.add(Calendar.DAY_OF_MONTH, -1)
                    }

                    while (dayList?.get(calendar.get(Calendar.DAY_OF_MONTH).toString()) != null) {
                        streak++
                        calendar.add(Calendar.DAY_OF_MONTH, -1)

                    }
                }
            }
            if(activity != null) {
                binding.streakTextView.text = getString(R.string.streak, streak)
            }

        }
        binding.refreshWeatherImgButton.setOnClickListener {
            getWeather()
            binding.lottiLoadingAnimation.playAnimation()
            binding.weatherLinearLayout.visibility = View.GONE
            binding.lottiLoadingAnimation.visibility = View.VISIBLE
        }

        binding.lottiLoadingAnimation.addAnimatorListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {

            }

            override fun onAnimationEnd(p0: Animator) {

            }

            override fun onAnimationCancel(p0: Animator) {
                binding.lottiLoadingAnimation.visibility = View.GONE
                binding.weatherLinearLayout.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(p0: Animator) {
                val a = 3
            }

        })
    }

    private fun setCalendar() {

        when (calPosition) {
            0 -> {
                binding.calendarLeftImageButton.visibility = View.INVISIBLE
            }
            11 -> {
                binding.calendarRightImageButton.visibility = View.INVISIBLE
            }
            else -> {
                binding.calendarLeftImageButton.visibility = View.VISIBLE
                binding.calendarRightImageButton.visibility = View.VISIBLE
            }
        }
        binding.calendarMonthTextView.text = dateFormat.format(calendar.time)
    }
}