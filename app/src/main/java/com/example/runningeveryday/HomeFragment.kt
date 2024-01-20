package com.example.runningeveryday

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.bumptech.glide.Glide
import com.example.runningeveryday.adapter.MonthAdapter
import com.example.runningeveryday.databinding.DialogProgressBinding
import com.example.runningeveryday.databinding.FragmentHomeBinding
import com.example.runningeveryday.model.Weather
import com.example.runningeverytime.api.WeatherApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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
    val binding: FragmentHomeBinding get() = viewBinding!!
    private lateinit var loadingDialog : LoadingDialog
    var loadCount = 0
    var streak = 0
    //val loadingDlg = LoadingDialog(requireContext())

    private val dlgDismissHandler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        if(loadCount == 3) {
            loadingDialog.dismiss()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        viewBinding = FragmentHomeBinding.inflate(layoutInflater)
        loadingDialog = LoadingDialog(requireContext())
        loadingDialog.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val locationManager: LocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var curLocation: Location? = null
        try {
           curLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        curPoint = dfs_xy_conv(curLocation!!.latitude, curLocation.longitude)
        setWeather(curPoint!!.x.toString(), curPoint!!.y.toString() )
        initCalendar()
        getCurrentUserProfile()
        return binding.root
    }


    fun setWeather(nx : String, ny : String) {

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
                    var list: List<Weather.Item> = response.body()!!.response.body.items.item

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
                    setWeather(rainRatio, rainType, sky, temp)
                }
            }

            override fun onFailure(call: Call<Weather>, t: Throwable) {

            }
        })
    }

    private fun setWeather(rainRatio: String, rainType: String, sky: String, temp: String) {
        binding.popTextView.text = getString(R.string.pop, rainRatio)
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
                    binding.weatherImageView.setImageResource(R.drawable.sun)
                }
                "3" -> {
                    type = "구름 많음"
                    binding.weatherImageView.setImageResource(R.drawable.clouds_and_sun)
                }
                "4" -> {
                    type = "흐림"
                    binding.weatherImageView.setImageResource(R.drawable.clouds)
                }
            }
        }

        binding.tempTextView.text = getString(R.string.temp, temp)
        binding.weatherTextView.text = type

        if(rainRatio.toInt() >= 60) {
            binding.skipRunButton.visibility = View.VISIBLE
        } else {
            binding.skipRunButton.visibility = View.GONE
        }

        loadCount++
        dlgDismissHandler.post(runnable)
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
        val calRecyclerView = binding.calendarRecyclerView
        calRecyclerView.adapter = MonthAdapter(requireContext())
        calRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        calRecyclerView.scrollToPosition(11)
        PagerSnapHelper().attachToRecyclerView(calRecyclerView)
        loadCount++
        dlgDismissHandler.post(runnable)
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

    inner class LoadingDialog(private val context: Context) : Dialog(context) {

        private var viewBinding: DialogProgressBinding? = null
        private val binding get() = viewBinding!!
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            viewBinding = DialogProgressBinding.inflate(layoutInflater)
            Glide.with(context).load(R.raw.load_32_128).into(binding.loadingImageView)
            setContentView(binding.root)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    private fun getCurrentUserProfile() {
        val curUser = GoogleSignIn.getLastSignedInAccount(requireContext())
        Glide.with(requireContext()).load(curUser?.photoUrl.toString()).into(binding.profileImgView)
        binding.profileTextView.text = getString(R.string.profile_name, curUser?.displayName)
        getStreak()
    }

    private fun getStreak() {

        val fireStore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("YYYYMM", Locale.KOREA)


        val collectionReference = fireStore.collection("users")
            .document(auth.uid!!).collection("record")

        collectionReference.get().addOnSuccessListener {task ->

            val list = task.documents
            list.reverse()
            for(document in list) {

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
            binding.streakTextView.text = getString(R.string.streak, streak)
            loadCount++
            dlgDismissHandler.post(runnable)
        }
    }
}