package com.example.horsegame

import android.graphics.Point
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableRow
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var cellSelected_x = 0
    private var cellSelected_y = 0

    private lateinit var board:Array<IntArray>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initScreenGame()
        resetBoard()
        setFirstPosition()
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
        x = (0..7).random()
        y = (0..7).random()

        cellSelected_x = x
        cellSelected_y = y

        selectCell(x,y)
    }

    private fun selectCell(x: Int, y: Int) {

        board[x][y] = 1
        paintHorseCell(cellSelected_x,cellSelected_y,"previous_cell")

        cellSelected_x = x
        cellSelected_y = y

        paintHorseCell(x,y,"selected_cell")
    }

    private fun paintHorseCell(x: Int, y: Int, color: String) {
        var iv:ImageView = findViewById(resources.getIdentifier("c$x$y","id",packageName))
        iv.setBackgroundColor(ContextCompat.getColor(this,resources.getIdentifier(color,"color",packageName)))
        iv.setImageResource((R.drawable.horse))
    }

    private fun initScreenGame() {
        setSizwBoard()
        hide_message()
    }

    private fun hide_message() {
        var lyMessage = findViewById<LinearLayout>(R.id.lyMensaje)
        lyMessage.visibility = View.INVISIBLE
    }

    private fun setSizwBoard() {
        var iv:ImageView

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x

        var width_dp = (width / resources.displayMetrics.density)

        var lateralMarginsDP = 0
        val width_cell = (width_dp - lateralMarginsDP) / 8
        val heigth_cell = width_cell

        for (i in 0..7){
            for (j in 0..7){
                iv = findViewById(resources.getIdentifier("c$i$j","id",packageName))

                var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,heigth_cell,resources.displayMetrics).toInt()
                var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,width_cell,resources.displayMetrics).toInt()

                iv.setLayoutParams(TableRow.LayoutParams(width,height))

            }
        }
    }

    fun checkChellClicked(view: View) {
        
    }

}