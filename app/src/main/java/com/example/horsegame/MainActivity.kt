package com.example.horsegame

import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot.capture
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.stripe.android.PaymentConfiguration
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var unloadedAd = true
    private var bitmap:Bitmap? = null

    private var LASTLEVEL = 9

    private var string_share = ""
    private var gaming = true
    private var mHandler: Handler? = null
    private var timeInSeconds:Long = 0

    private var cellSelected_x = 0
    private var cellSelected_y = 0

    private var nextLevel = false

    private var options = 0
    private var moves = 0
    private var movesRequired = 0
    private var bonus = 0
    private var widht_bonus = 0
    private var level = 1
    private var levelMoves = 0

    private var lives = 1
    private var score_lives = 1
    private var scoreLevel = 1

    private var checkMovement = true

    private var nameColorBlack = "black_cell"
    private var nameColorWhite = "white_cell"

    private var mInterstitialAd: InterstitialAd? = null

    private lateinit var board:Array<IntArray>

    private var premium: Boolean = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private var optionBlack = R.drawable.option_black
    private var optionWhite = R.drawable.option_white

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initScreenGame()
        initPreferences()
    }

    private fun initPreferences() {
        sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    override fun onResume() {
        super.onResume()

        checkPremium()
        startGame()
    }

    private fun checkPremium() {

        premium = sharedPreferences.getBoolean("PREMIUM",false)
        if(premium){

            LASTLEVEL = 14

            level = sharedPreferences.getInt("LEVEL",1)

            var lyPremium = findViewById<LinearLayout>(R.id.lyPremium)
            lyPremium.removeAllViews()

            var lyAdsPBanner = findViewById<LinearLayout>(R.id.lyAdsBanner)
            lyAdsPBanner.removeAllViews()

            var svGame = findViewById<ScrollView>(R.id.svGame)
            svGame.setPadding(0,0,0,0)

            var tvLiveData = findViewById<TextView>(R.id.tvLiveData)
            tvLiveData.background  = getDrawable(R.drawable.bg_data_bottom_contrast_premium)

            var tvLiveTitle = findViewById<TextView>(R.id.tvLiveTitle)
            tvLiveTitle.background  = getDrawable(R.drawable.bg_data_bottom_contrast_premium)

            var vNewBonus = findViewById<View>(R.id.vNewBonus)
            vNewBonus.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier("contrast_data_premium","color",packageName)))

            nameColorBlack = "black_cell_premium"
            nameColorWhite = "white_cell_premium"

            optionBlack = R.drawable.option_black_premium
            optionWhite = R.drawable.option_white_premium
        }else{
            initAds()
        }
    }

    private fun initAds() {
        MobileAds.initialize(this) {}

        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"

        var lyAdsBanner = findViewById<LinearLayout>(R.id.lyAdsBanner)
        lyAdsBanner.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

    }

    private fun showInterstitial(){
        if (mInterstitialAd != null) {
            unloadedAd = true

            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                }

                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    mInterstitialAd = null
                }

                override fun onAdImpression() {
                }

                override fun onAdShowedFullScreenContent() {
                }
            }

            mInterstitialAd?.show(this)
        }
    }
    private fun getReadyAds(){

        var adRequest = AdRequest.Builder().build()

        unloadedAd = false

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun resetTime(){
        mHandler?.removeCallbacks(chronometer)
        timeInSeconds = 0

        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        tvTimeData.text = "00:00"
    }
    private fun startTime(){
        mHandler = Handler(Looper.getMainLooper())
        chronometer.run()
    }

    private var chronometer:Runnable = object : Runnable{
        override fun run() {
            try {
                if(gaming){
                    timeInSeconds++
                    updateStopWatchVire(timeInSeconds)
                }
            }finally {
                mHandler!!.postDelayed(this,1000L)
            }
        }

        private fun updateStopWatchVire(timeInSeconds: Long) {
            val formattedTime = getFormatterStopWarch((timeInSeconds*1000))
            var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
            tvTimeData.text = formattedTime
        }
    }

    private fun getFormatterStopWarch(ms: Long): String {
        var milliseconds = ms
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        return "${if (minutes < 10) "0 else " else ""}$minutes:"+
                "${if (seconds < 10) "0" else ""}$seconds"
    }

    private fun startGame() {

        if (unloadedAd && !premium) getReadyAds()

        setLevel()
        if (level > LASTLEVEL){
            if (premium) showMessage("You have beaten the game!!", "Wait for more levels",false,true)
            else showMessage("More levels only with Premium access", "Get Premium Access",false,true)

        }else{

            setLevelParameters()

            resetBoard()
            clearBoard()

            setBoardLevel()
            setFirstPosition()

            resetTime()
            startTime()
            gaming = true
        }
    }

    private fun setBoardLevel() {
        when (level){
            2  -> paintLevel_2()
            3  -> paintLevel_3()
            4  -> paintLevel_4()
            5  -> paintLevel_5()
            6  -> paintLevel_6()
            7  -> paintLevel_7()
            8  -> paintLevel_8()
            9  -> paintLevel_9()
            10 -> paintLevel_10()
            11 -> paintLevel_11()
            12 -> paintLevel_12()
            13 -> paintLevel_13()
        }
    }

    private fun paintLevel_13() {
        paintLevel_12();paintLevel_11()
    }

    private fun paintLevel_12() {
        paintRow(4);paintRow(6)
    }

    private fun paintLevel_11() {
        paintColum(1);paintColum(7)
    }

    private fun paintLevel_10() {
        paintLevel_8();paintLevel_9()
    }

    private fun paintLevel_9() {
        paintColum(2)
    }

    private fun paintLevel_8() {
        paintRow(5)
    }

    private fun paintLevel_7() {
        paintLevel_6();paintLevel_4()
    }

    private fun paintLevel_6() {
        paintLevel_3();paintLevel_2()
    }


    private fun paintRow(row:Int) {

        for (i in 0..7) {
            board[i][row] = 1
            paintHorseCell(i, row, "previous_cell")
        }

    }

    private fun paintDiagonal() {

        for (i in 0..7) {
            board[i][i] = 1
            paintHorseCell(i, i, "previous_cell")
        }

    }

    private fun paintDiagonalInverse() {

        for (i in 0..7) {
            board[i][Math.abs(i - 7)] = 1
            paintHorseCell(i, Math.abs(i - 7), "previous_cell")
        }

    }
    private fun paintLevel_4() {
        paintLevel_3();paintLevel_5()
    }

    private fun paintLevel_5() {
        for (i in 0..3){
            for (j in 0..3){
                board[j][i]=1
                paintHorseCell(j,i,"previous_cell")
            }
        }
    }

    private fun paintLevel_3() {
        for (i in 0..7){
            for (j in 0..7){
                board[j][i] = 1
                paintHorseCell(j,i,"previous_cell")
            }
        }
    }

    private fun paintLevel_2() {
        paintColum(6)
    }

    private fun paintColum(colum: Int) {
        for(i in 0..7){
            board[colum][i] = 1
            paintHorseCell(colum,i,"previous_cell")
        }
    }

    private fun setLevelParameters() {
        var tvLiveData = findViewById<TextView>(R.id.tvLiveData)
        tvLiveData.text = lives.toString()

        if (premium) tvLiveData.text = "∞"

        score_lives = lives
        var tvLevelNumber = findViewById<TextView>(R.id.tvLevelNumber)
        tvLevelNumber.text = lives.toString()
        scoreLevel = level

        bonus = 0
        var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
        tvBonusData.text = ""

        setLevelMoves()
        moves = levelMoves
        movesRequired = setMovesRequired()

    }

    private fun setMovesRequired(): Int {
        var movesRequired = 0
        when (level){
            1  -> movesRequired = 8
            2  -> movesRequired = 10
            3  -> movesRequired = 12
            4  -> movesRequired = 10
            5  -> movesRequired = 10
            6  -> movesRequired = 12
            7  -> movesRequired = 5
            8  -> movesRequired = 7
            9  -> movesRequired = 9
            10 -> movesRequired = 8
            11 -> movesRequired = 1000
            12 -> movesRequired = 5
            13 -> movesRequired = 5
        }
        return movesRequired
    }

    private fun setLevelMoves() {
        when (level){
            1  -> levelMoves = 64
            2  -> levelMoves = 56
            3  -> levelMoves = 32
            4  -> levelMoves = 16
            5  -> levelMoves = 48
            6  -> levelMoves = 36
            7  -> levelMoves = 48
            8  -> levelMoves = 49
            9  -> levelMoves = 59
            10 -> levelMoves = 48
            11 -> levelMoves = 64
            12 -> levelMoves = 48
            13 -> levelMoves = 48
        }
    }

    private fun setLevel() {
        if(nextLevel){
            level++
            setLives()
           /* if(!premium) setLives()
            else{
                editor.apply{
                    putInt("LEVEL",level!!)
                }.apply()
            }*/
        }else{
            if(!premium){
                lives--
                if (lives < 1){
                    level = 1
                    lives = 1
                }
            }
        }
    }

    private fun setLives() {
        when (level){
            1  -> lives = 1
            2  -> lives = 4
            3  -> lives = 3
            4  -> lives = 3
            5  -> lives = 4
            6  -> lives = 3
            7  -> lives = 5
            8  -> lives = 3
            9  -> lives = 4
            10 -> lives = 5
            11 -> lives = 5
            12 -> lives = 3
            13 -> lives = 4
        }
        if (premium) lives = 99999999
    }

    private fun clearBoard() {
        var iv:ImageView

        var colorBlack = ContextCompat.getColor(this,resources.getIdentifier(nameColorBlack,"color",packageName))
        var colorWhite = ContextCompat.getColor(this,resources.getIdentifier(nameColorWhite,"color",packageName))

        for (i in 0..7){
            for (j in 0..7){
                iv = findViewById(resources.getIdentifier("c$i$j","id",packageName))
                //iv.setImageResource(R.drawable.horse)
                iv.setImageResource(0)

                if (checkColorCell(i,j) == "black") iv.setBackgroundColor(colorBlack)
                else iv.setBackgroundColor(colorWhite)
            }
        }
    }


    private fun initScreenGame() {
        setSizeBoard()
        hideMessage(false)
    }
    private fun hideMessage(start: Boolean) {

        var lyMessage = findViewById<LinearLayout>(R.id.lyMensaje)
        lyMessage.visibility = View.INVISIBLE

        if (start) startGame()
    }
    private fun setSizeBoard() {
        var iv:ImageView

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x

        var width_dp = (width / resources.displayMetrics.density)

        var lateralMarginsDP = 0
        val width_cell = (width_dp - lateralMarginsDP) / 8
        val heigth_cell = width_cell

        widht_bonus = 2 * width_cell.toInt()

        for (i in 0..7){
            for (j in 0..7){
                iv = findViewById(resources.getIdentifier("c$i$j","id",packageName))

                var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,heigth_cell,resources.displayMetrics).toInt()
                var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,width_cell,resources.displayMetrics).toInt()

                iv.setLayoutParams(TableRow.LayoutParams(width,height))

            }
        }
    }


    fun checkCellClicked(v: View) {
        var name = v.tag.toString()
        var x = name.subSequence(1,2).toString().toInt()
        var y = name.subSequence(2,3).toString().toInt()

        checkCell(x,y)
    }
    private fun checkCell(x: Int, y: Int) {

        var checkTrue = true
        if(checkMovement){
            var dif_x = x - cellSelected_x
            var dif_y = y - cellSelected_y

            checkTrue = false

            if(dif_x == 1 && dif_y == 2)   checkTrue = true  // right - top long
            if(dif_x == 1 && dif_y == -2)  checkTrue = true  // right - bottom long
            if(dif_x == 2 && dif_y == 1)   checkTrue = true  // right long - top
            if(dif_x == 2 && dif_y == -1)  checkTrue = true  // right long - bottom
            if(dif_x == -1 && dif_y == 2)  checkTrue = true  // left - top long
            if(dif_x == -1 && dif_y == -2) checkTrue = true  // left - bottom long
            if(dif_x == -2 && dif_y == 1)  checkTrue = true  // left long - top
            if(dif_x == -2 && dif_y == -1) checkTrue = true  // left long - bottom
        }else{
            if(board[x][y] != 1){
                bonus--
                var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
                tvBonusData.text = " + $bonus"

                if(bonus == 0) tvBonusData.text = ""
            }
        }

        if(board[x][y] == 1) checkTrue = false

        if(checkTrue) selectCell(x,y)
    }
    private fun selectCell(x: Int, y: Int) {

        moves--
        var tvMovesData = findViewById<TextView>(R.id.tvMovesData)
        tvMovesData.text = moves.toString()

        growProgressBonus()

        if(board[x][y] == 2){
            bonus++

            var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
            tvBonusData.text = " + $bonus"
        }

        board[x][y] = 1
        paintHorseCell(cellSelected_x,cellSelected_y,"previous_cell")

        cellSelected_x = x
        cellSelected_y = y

        clearOptions()

        paintHorseCell(x,y,"selected_cell")
        checkMovement = true
        checkOption(x,y)

        if(moves > 0){
            checkNewBonus()
            checkGameOver()
        }else{
            showMessage("You win!!", "Next Level", false, true)
        }
    }


    private fun resetBoard() {
        // 0 esta libre
        // 1 casilla marcada
        // 2 es un bonus
        // 9 es una opcion del movimiento actual
        board = arrayOf(
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
        )
    }
    private fun setFirstPosition() {
        var x = 0
        var y = 0
        var firstPosition = false

        while (!firstPosition){
            x = (0..7).random()
            y = (0..7).random()

            if(board[x][y] == 0) firstPosition = true
            checkOption(x,y)
            if(options == 0) firstPosition = false
        }

        cellSelected_x = x
        cellSelected_y = y

        selectCell(x,y)
    }


    private fun growProgressBonus() {

        var moves_done = levelMoves - moves
        var bonus_done = moves_done / movesRequired
        var move_rest = movesRequired * (bonus_done)
        var bonus_grow = moves_done - move_rest

        var v = findViewById<View>(R.id.vNewBonus)

        var widthBonus = ((widht_bonus/movesRequired) * bonus_grow).toFloat()

        var heigth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8f,resources.displayMetrics).toInt()
        var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,widthBonus,resources.displayMetrics).toInt()
        v.setLayoutParams(TableRow.LayoutParams(width,heigth))
    }
    private fun checkNewBonus() {
        if(moves % movesRequired == 0){
            var bonusCell_x = 0
            var bonusCell_y = 0

            var bonusCell = false
            while (bonusCell == false){
                bonusCell_x = (0..7).random()
                bonusCell_y = (0..7).random()

                if(board[bonusCell_x][bonusCell_y] == 0) bonusCell = true
            }

            board[bonusCell_x][bonusCell_y] = 2
            paintBonusCell(bonusCell_x,bonusCell_y)
        }
    }
    private fun paintBonusCell(x: Int, y: Int) {
        var iv:ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        iv.setImageResource(R.drawable.bonus)
    }


    private fun paintOption(x: Int, y: Int) {

        var iv:ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        if(checkColorCell(x,y) == "black") iv.setBackgroundResource(optionBlack)
        else iv.setBackgroundResource(optionWhite)

    }
    private fun paintAllOptions() {
        for (i in 0..7){
            for (j in 0..7){
                if (board[i][j] != 1) paintOption(i,j)
                if (board[i][j] == 0) board[i][j] = 9
            }
        }
    }
    private fun clearOptions() {
        for (i in 0..7){
            for (j in 0..7){
                if (board[i][j] == 9 || board[i][j] == 2){
                    if(board[i][j] == 9) board[i][j] = 0
                    clearOption(i,j)
                }
            }
        }
    }
    private fun clearOption(x: Int, y: Int) {

        var iv:ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        if(checkColorCell(x,y) == "black")
            iv.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier(nameColorBlack,"color",packageName)))
        else
            iv.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier(nameColorWhite,"color",packageName)))

        if(board[x][y] == 1) iv.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier("previous_cell","color",packageName)))

    }


    private fun checkGameOver() {
        if(options == 0){
            if(bonus > 0){
                checkMovement = false
                paintAllOptions()
            }else{
                showMessage("Game Over", "Try Again!", true, true)
            }
        }
    }
    private fun showMessage(tittle: String, action: String, gameOver: Boolean, endGame: Boolean = false) {
        gaming = false
        nextLevel = !gameOver

        var lyMessage = findViewById<LinearLayout>(R.id.lyMensaje)
        lyMessage.visibility = View.VISIBLE

        var tvTitleMessage = findViewById<TextView>(R.id.tvTitleMessage)
        tvTitleMessage.text = tittle

        var tvTimeData = findViewById<TextView>(R.id.tvTimeData)
        var score = ""
        if(gameOver){
            if(!premium) showInterstitial()
            score = "Score: " + (levelMoves-moves) + "/" + levelMoves
            string_share = "This game makes me sick !!!" + score + " https://github.com/kikiymini/curso-android"
        }else{
            score = tvTimeData.text.toString()
            string_share = "Let's go!! New challenge completed. Level: $level (" + score + ") https://github.com/kikiymini/curso-android"
        }

        if (endGame) score = ""

        var tvScoreMessage = findViewById<TextView>(R.id.tvScoreMessage)
        tvScoreMessage.text = score

        var tvAction = findViewById<TextView>(R.id.tvAction)
        tvAction.text = action


    }


    private fun checkOption(x: Int, y: Int) {
        options = 0
        checkMove(x,y,1,2) // check move right - top long
        checkMove(x,y,2,1) // check move right long - top
        checkMove(x,y,1,-2) // check move right - bottom long
        checkMove(x,y,2,-1) // check move right long - bottom
        checkMove(x,y,-1,2) // check move left - top long
        checkMove(x,y,-2,1) // check move left long - top
        checkMove(x,y,-1,-2) // check move left - bottom long
        checkMove(x,y,-2,-1) // check move left long - bottom

        var tvOptionsData = findViewById<TextView>(R.id.tvOptionsData)
        tvOptionsData.text = options.toString()
    }
    private fun checkMove(x: Int, y: Int, mov_x: Int, mov_y: Int) {
        var option_x = x + mov_x
        var option_y = y + mov_y

        if(option_x < 8 && option_y < 8 && option_x >= 0 && option_y >= 0){
            if(board[option_x][option_y] == 0 || board[option_x][option_y] == 2){
                options++
                paintOption(option_x,option_y)
               if( board[option_x][option_y] == 0) board[option_x][option_y] = 9
            }
        }
    }
    private fun checkColorCell(x: Int, y: Int): String {
        var color = ""
        var blackColum_x = arrayOf(0,2,4,6)
        var blackRow_x = arrayOf(1,3,5,7)

        if((blackColum_x.contains(x) && blackColum_x.contains(y)) || ((blackRow_x.contains(x) && blackRow_x.contains(y)) ) )
            color = "black"
        else color = "white"

        return color
    }

    private fun paintHorseCell(x: Int, y: Int, color: String) {
        var iv:ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        iv.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier(color,"color",packageName)))
        iv.setImageResource((R.drawable.horse))
    }

    fun lauchShareGame(v: View) {
        shareGame()
    }

    private fun shareGame() {
        //ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),1)
        //ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)

        var ssc: ScreenCapture = capture(this)
        bitmap = ssc.getBitmap()

        if (bitmap != null){
            var idGame = SimpleDateFormat("yyyy/MM/dd").format(Date())
            idGame = idGame.replace(":","")
            idGame = idGame.replace("/","")

            val path = saveImage(bitmap!!,"${idGame}.jpg")
            val bmpUri = Uri.parse(path)

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            shareIntent.putExtra(Intent.EXTRA_STREAM,bmpUri)
            shareIntent.putExtra(Intent.EXTRA_STREAM,string_share)
            shareIntent.type = "image/png"

            val finalShareIntent = Intent.createChooser(shareIntent,"Select the app you want to share the game to")
            finalShareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(finalShareIntent)

        }

    }

    private fun saveImage(bitmap: Bitmap, fileName: String): String? {
        if(bitmap == null)
            return null

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME,fileName)
                put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots")
            }

            val uri = this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues)

            if(uri!=null){
                this.contentResolver.openOutputStream(uri).use {
                    if(it == null)
                        return@use

                    bitmap.compress(Bitmap.CompressFormat.PNG,85,it)
                    it.flush()
                    it.close()

                    // add pic to gallery
                    MediaScannerConnection.scanFile(this, arrayOf(uri.toString()),null,null)

                }
            }
        }
        return null
    }

    fun lauchAction(v: View) {
        if (!premium && (level > LASTLEVEL)) callPayment()
        hideMessage(true)
    }

    fun lauchPaymentCard(v: View) {
        callPayment()
    }

    private fun callPayment() {

        var keyStripePayment = "pk_test_Dt4ZBItXSZT1EzmOd8yCxonL"
        PaymentConfiguration.init(applicationContext, keyStripePayment)

        val intent = Intent(this,CheckoutActivity::class.java)
        intent.putExtra("level",level)
        startActivity(intent)
    }


}