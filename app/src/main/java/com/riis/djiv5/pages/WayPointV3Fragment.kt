package com.riis.djiv5.pages

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.annotation.IntDef
import com.riis.djiv5.pages.DJIFragment
import com.riis.djiv5.models.WayPointV3VM
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.*
import java.io.File
import java.util.*

import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.SupportMapFragment

import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState
import java.io.IOException
import kotlin.collections.ArrayList
import android.content.DialogInterface

import android.content.DialogInterface.OnMultiChoiceClickListener
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dji.flysafe.mapkit.core.core.models.DJIBitmapDescriptor
import com.dji.flysafe.mapkit.core.core.models.DJILatLng
import com.dji.flysafe.mapkit.core.core.models.annotations.DJIMarkerOptions
import com.dji.flysafe.mapkit.core.core.models.annotations.DJIPolylineOptions
import com.dji.industry.mission.DocumentsUtils
import com.example.mapviewdemo.R
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style


import dji.sdk.wpmz.jni.JNIWPMZManager
import dji.sdk.wpmz.value.mission.WaylineExecuteWaypoint
import dji.v5.manager.aircraft.waypoint3.WPMZParserManager
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener
import dji.v5.manager.aircraft.waypoint3.WaypointActionListener
import dji.v5.manager.aircraft.waypoint3.model.WaylineExecutingInfo


import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap


/**
 * @author feel.feng
 * @time 2022/02/27 9:30 上午
 * @description:
 */
class WayPointV3Fragment : View.OnClickListener, AppCompatActivity(), OnMapReadyCallback {


    private val wayPointV3VM: WayPointV3VM by activityViewModels()
    private val WAYPOINT_SAMPLE_FILE_NAME: String = "waypointsample.kmz"
    private val WAYPOINT_SAMPLE_FILE_DIR: String = "waypoint/"
    private val WAYPOINT_SAMPLE_FILE_CACHE_DIR: String = "waypoint/cache/"
    private val WAYPOINT_FILE_TAG = ".kmz"
    private var unzipChildDir = "temp/"
    private var unzipDir = "wpmz/"
    private var mDisposable : Disposable ?= null
    private val OPEN_FILE_CHOOSER = 0
    private val OPEN_DOCUMENT_TREE = 1


    var curMissionPath: String = DiskUtil.getExternalCacheDirPath(
        ContextUtil.getContext(),
        WAYPOINT_SAMPLE_FILE_DIR + WAYPOINT_SAMPLE_FILE_NAME
    )
    val rootDir = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_DIR)
    var validLenth: Int = 2
    var curMissionExecuteState: WaypointMissionExecuteState? = null
    var selectWaylines: ArrayList<Int> = ArrayList()

    private lateinit var locate: Button
    private lateinit var start: Button
    private lateinit var stop: Button
    private lateinit var config: Button
    private lateinit var upload: Button
    private lateinit var pause: Button
    private lateinit var resume: Button
    private lateinit var select: Button
    private lateinit var kmz_btn: Button

    private lateinit var showTrack : Button
    private lateinit var mTextGPS: TextView
    private lateinit var clearWaypoints : Button

    private var mapboxMap: MapboxMap? = null
    private var droneMarker: Marker? = null
    private val markers: MutableMap<Int, Marker> = ConcurrentHashMap<Int, Marker>()


    private fun initUi()
    {
        locate = findViewById(R.id.locate)
        start = findViewById(R.id.start)
        stop = findViewById(R.id.stop)
        config = findViewById(R.id.start)
        upload = findViewById(R.id.stop)
        pause = findViewById(R.id.pause)
        resume = findViewById(R.id.resume)
        select = findViewById(R.id.select)
        kmz_btn = findViewById(R.id.kmz_btn)

        mTextGPS = findViewById(R.id.GPSTextView)
        showTrack = findViewById(R.id.showTrack)
        clearWaypoints = findViewById(R.id.clearWaypoints)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap // initialize the map
        //mapboxMap.addOnMapClickListener(this)
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { // set the view of the map
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token)) // this will get your mapbox instance using your access token
        setContentView(R.layout.activity_waypoint1) // use the waypoint1 activity layout

        initUi() // initialize the UI

        var mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.onCreate(savedInstanceState)
        mapFragment.getMapAsync(this)

        prepareMissionData()
        initView(savedInstanceState)
        initData()

        }

    private fun markWaypoint(point: LatLng) {
        val markerOptions = MarkerOptions()
            .position(point)
        mapboxMap?.let {
            val marker = it.addMarker(markerOptions)
            markers.put(markers.size, marker)
        }
    }


    private fun prepareMissionData() {

        val dir = File(rootDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val cachedirName = DiskUtil.getExternalCacheDirPath(
            ContextUtil.getContext(),
            WAYPOINT_SAMPLE_FILE_CACHE_DIR
        )
        val cachedir = File(cachedirName)
        if (!cachedir.exists()) {
            cachedir.mkdirs()
        }
        val destPath = rootDir + WAYPOINT_SAMPLE_FILE_NAME
        if (!File(destPath).exists()) {
            FileUtils.copyAssetsFile(
                ContextUtil.getContext(),
                WAYPOINT_SAMPLE_FILE_NAME,
                destPath
            )
        }
    }

    private fun initView(savedInstanceState: Bundle?) {
        //sp_map_switch.adapter = wayPointV3VM.getMapSpinnerAdapter()
        wayPointV3VM.addMissionStateListener() {
            //mission_execute_state_tv?.text = "Mission Execute State : ${it.name}"
            //btn_mission_upload.isEnabled = it == WaypointMissionExecuteState.READY
            curMissionExecuteState = it
        }
        wayPointV3VM.addWaylineExecutingInfoListener(object :WaylineExecutingInfoListener {
            override fun onWaylineExecutingInfoUpdate(it: WaylineExecutingInfo) {
                //wayline_execute_state_tv?.text = "Wayline Execute Info WaylineID:${it.waylineID} \n" +
                        "WaypointIndex:${it.currentWaypointIndex} \n" +
                        "MissionName : ${if (curMissionExecuteState == WaypointMissionExecuteState.READY) "" else it.missionFileName}"
            }

            override fun onWaylineExecutingInterruptReasonUpdate(error: IDJIError?) {
                if (error != null) {
                    //LogUtils.e(logTag , "interrupt error${error.description()}")
                }
            }

        });


        wayPointV3VM.addWaypointActionListener(object :WaypointActionListener{
            override fun onExecutionStart(actionId: Int) {
                //waypint_action_state_tv?.text = "onExecutionStart: ${actionId} "
            }

            override fun onExecutionFinish(actionId: Int, error: IDJIError?) {
                //waypint_action_state_tv?.text = "onExecutionFinish: ${actionId}  error ${error?.toString()}"
            }

        })

        upload.setOnClickListener {
            val waypointFile = File(curMissionPath)
            if (waypointFile.exists()) {
                wayPointV3VM.pushKMZFileToAircraft(curMissionPath)
            } else {
                ToastUtils.showToast("Mission file not found!");
            }
            markWaypoints()
        }

       /* wayPointV3VM.missionUploadState.observe(viewLifecycleOwner) {
            it?.let {
                when {
                    it.error != null -> {
                        //mission_upload_state_tv?.text = "Upload State: error:${getErroMsg(it.error)} "
                    }
                    it.tips.isNotEmpty() -> {
                        //mission_upload_state_tv?.text = it.tips
                    }
                    else -> {
                        //mission_upload_state_tv?.text = "Upload State: progress:${it.updateProgress} "
                    }
                }

            }
        }*/


        start.setOnClickListener {
            wayPointV3VM.startMission(
                FileUtils.getFileName(curMissionPath, WAYPOINT_FILE_TAG),
                selectWaylines,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("startMission Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("startMission Failed " + getErroMsg(error))
                    }
                })

        }

        pause.setOnClickListener {
            wayPointV3VM.pauseMission(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("pauseMission Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("pauseMission Failed " + getErroMsg(error))
                }
            })

        }

        resume.setOnClickListener {
            wayPointV3VM.resumeMission(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("resumeMission Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("resumeMission Failed " + getErroMsg(error))
                }
            })

        }

        select.setOnClickListener {
            if (curMissionPath == null) {
                ToastUtils.showToast("please upload mission")
                return@setOnClickListener
            }
            selectWaylines.clear()
            var waylineids = wayPointV3VM.getAvailableWaylineIDs(curMissionPath)
            //showMultiChoiceDialog(waylineids)
        }

        kmz_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(intent, "Select KMZ File"), OPEN_FILE_CHOOSER
            )
        }

        /*map_locate.setOnClickListener {
            map_widget.setMapCenterLock(MapWidget.MapCenterLock.AIRCRAFT)
        }

        sp_map_switch.setSelection(wayPointV3VM.getMapType(context))*/

        stop.setOnClickListener {
            if (curMissionExecuteState == WaypointMissionExecuteState.READY) {
                ToastUtils.showToast("Mission not start")
                return@setOnClickListener
            }
            wayPointV3VM.stopMission(
                FileUtils.getFileName(curMissionPath, WAYPOINT_FILE_TAG),
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("stopMission Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("stopMission Failed " + getErroMsg(error))
                    }
                })
        }
        /*btn_editKmz.setOnClickListener {
            showEditDialog()
        }*/

        /*createMapView(savedInstanceState)*/

    }



    /*private fun showEditDialog() {
        val waypointFile = File(curMissionPath)
        if (!waypointFile.exists()) {
            ToastUtils.showToast("Please upload kmz file")
            return
        }

        val unzipFolder = File(rootDir, unzipChildDir)
        // 解压后的waylines路径
        val templateFile = File(rootDir + unzipChildDir + unzipDir, WPMZParserManager.TEMPLATE_FILE)
        val waylineFile = File(rootDir + unzipChildDir + unzipDir, WPMZParserManager.WAYLINE_FILE)

        mDisposable = Single.fromCallable {
            //在cache 目录创建一个wmpz文件夹，并将template.kml 与 waylines.wpml 拷贝进wpmz ，然后压缩wpmz文件夹
            WPMZParserManager.unZipFolder(ContextUtil.getContext(), curMissionPath, unzipFolder.path, false)
            FileUtils.readFile(waylineFile.path , null)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { wpmlContent: String? ->
                    DialogUtil.showInputDialog(requireActivity() ,"",wpmlContent , "", false , object :CommonCallbacks.CompletionCallbackWithParam<String> {
                        override fun onSuccess(newContent: String?) {
                            newContent?.let {
                                updateWPML(it)
                            }
                        }
                        override fun onFailure(error: IDJIError) {
                            LogUtils.e(logTag , "show input Dialog Failed ${error.description()} ")
                        }

                    })
                }
            ) { throwable: Throwable ->
                LogUtils.e(logTag , "show input Dialog Failed ${throwable.message} ")
            }
    }*/

    private fun updateWPML(newContent: String) {
        val waylineFile = File(rootDir + unzipChildDir + unzipDir, WPMZParserManager.WAYLINE_FILE)

        Single.fromCallable {
            FileUtils.writeFile(waylineFile.path, newContent, false)
            //将修改后的waylines.wpml重新压缩打包成 kmz
            val zipFiles = mutableListOf<String>()
            val cacheFolder = File(rootDir, unzipChildDir + unzipDir)
            var zipFile = File(rootDir + unzipChildDir + "waypoint.kmz")
            if (waylineFile.exists()) {
                zipFiles.add(cacheFolder.path)
                zipFile.createNewFile()
                WPMZParserManager.zipFiles(ContextUtil.getContext(), zipFiles, zipFile.path)
            }
            //将用户选择的kmz用修改的后的覆盖
            FileUtils.copyFileByChannel(zipFile.path, curMissionPath)
        }.subscribeOn(Schedulers.io()).subscribe()

    }

    private fun initData() {
        wayPointV3VM.listenFlightControlState()
        /*wayPointV3VM.flightControlState.observe(viewLifecycleOwner) {
            it?.let {
                //wayline_aircraft_height?.text = String.format("Aircraft Height: %.2f", it.height)
                //wayline_aircraft_distance?.text =
                    String.format("Aircraft Distance: %.2f", it.distance)
            }
        }*/
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        wayPointV3VM.cancelListenFlightControlState()
        wayPointV3VM.removeAllMissionStateListener()
        wayPointV3VM.clearAllWaylineExecutingInfoListener()
        wayPointV3VM.clearAllWaypointActionListener()

        mDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    fun getErroMsg(error: IDJIError): String {
        if (!TextUtils.isEmpty(error.description())) {
            return error.description();
        }
        return error.errorCode()
    }


    /*fun showMultiChoiceDialog(waylineids: List<Int>) {
        var items: ArrayList<String> = ArrayList()
        waylineids
            .filter {
                it >= 0
            }
            .map {
                items.add(it.toString())
            }

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle("Select Wayline")
        builder.setPositiveButton("OK", null)
        builder.setMultiChoiceItems(
            items.toTypedArray(),
            null,
            object : OnMultiChoiceClickListener {
                override fun onClick(p0: DialogInterface?, index: Int, isSelect: Boolean) {
                    if (isSelect) {
                        selectWaylines.add(index)
                    } else {
                        selectWaylines.remove(index)
                    }
                }
            }).create().show()

    }*/

    fun markWaypoints() {
        // version参数实际未用到
        var waypoints: ArrayList<WaylineExecuteWaypoint> = ArrayList<WaylineExecuteWaypoint>()
        val parseInfo = JNIWPMZManager.getWaylines("1.0.0", curMissionPath)
        var waylines = parseInfo.waylines
        waylines.forEach() {
            waypoints.addAll(it.waypoints)
            markLine(it.waypoints)
        }
        waypoints.forEach() {
            markWaypoint(DJILatLng(it.location.latitude, it.location.longitude), it.waypointIndex)
        }
    }

    fun markWaypoint(latlong: DJILatLng, waypointIndex: Int) {
        var markOptions = DJIMarkerOptions()
        markOptions.position(latlong)
        markOptions.icon(DJIBitmapDescriptor(R.drawable.aircraft))
        markOptions.title(waypointIndex.toString())
        markOptions.isInfoWindowEnable = true
        //map_widget.map?.addMarker(markOptions)
    }

    fun markLine(waypoints: List<WaylineExecuteWaypoint>) {

        var djiwaypoints = waypoints.filter {
            true
        }.map {
            DJILatLng(it.location.latitude, it.location.longitude)
        }
        var lineOptions = DJIPolylineOptions()
        lineOptions.width(5f)
        lineOptions.color(Color.GREEN)
        lineOptions.addAll(djiwaypoints)
        //map_widget.map?.addPolyline(lineOptions)
    }


    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }


}