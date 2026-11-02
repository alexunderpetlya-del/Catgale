package com.catgale.game

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = 5894 or 1024 or 512
        setContentView(CatGameView(this))
    }
}

private class CatGameView(context: android.content.Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val walls = setOf("5,2","5,3","5,4","9,1","9,2","9,3","9,4","13,2","14,2","15,2","3,7","4,7","5,7","9,7","10,7","11,7","15,7","16,7","17,7","7,9","8,9","12,9","13,9")
    private val hiding = setOf("2,6","7,5","12,5","16,9")
    private val patrol = listOf(16 to 5, 16 to 6, 16 to 7, 16 to 6)
    private var catX = 2
    private var catY = 10
    private var dogStep = 0
    private var fishTaken = false
    private var ended = false
    private var victory = false
    private var score = 0
    private val buttons = mutableMapOf<String, RectF>()

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(Color.rgb(15, 24, 32))
        val scale = minOf(width / 800f, height / 520f)
        val ox = (width - 800f * scale) / 2f
        val oy = (height - 520f * scale) / 2f
        c.save()
        c.translate(ox, oy)
        c.scale(scale, scale)
        for (y in 0..12) for (x in 0..19) {
            p.color = if ((x + y) % 2 == 0) Color.rgb(39, 62, 53) else Color.rgb(33, 54, 47)
            c.drawRect(x*40f, y*40f, (x+1)*40f, (y+1)*40f, p)
            if (x == 0 || x == 19 || y == 0 || y == 12 || "$x,$y" in walls) {
                p.color = Color.rgb(75, 81, 88)
                c.drawRect(x*40f, y*40f, (x+1)*40f, (y+1)*40f, p)
                p.color = Color.rgb(111, 119, 124)
                c.drawRect(x*40f, y*40f, (x+1)*40f, y*40f+5f, p)
            }
        }
        hiding.forEach {
            val a = it.split(","); val x = a[0].toInt(); val y = a[1].toInt()
            p.color = Color.rgb(135, 88, 53)
            c.drawRoundRect(x*40f+3, y*40f+7, x*40f+37, y*40f+37, 5f, 5f, p)
            p.color = Color.rgb(184, 132, 77)
            c.drawRect(x*40f+7, y*40f+11, x*40f+33, y*40f+16, p)
        }
        val dog = patrol[dogStep]
        p.color = Color.argb(55, 255, 190, 80)
        c.drawCircle(dog.first*40f+20, dog.second*40f+20, 105f, p)
        if (!fishTaken) {
            p.color = Color.rgb(248, 194, 75)
            c.drawOval(690f, 52f, 728f, 77f, p)
            p.color = Color.BLACK
            c.drawCircle(719f, 60f, 2.5f, p)
        }
        p.color = Color.rgb(170, 102, 72)
        c.drawRoundRect(dog.first*40f+5, dog.second*40f+8, dog.first*40f+35, dog.second*40f+34, 8f, 8f, p)
        p.color = Color.rgb(245, 221, 180)
        c.drawCircle(dog.first*40f+27, dog.second*40f+17, 3f, p)
        p.color = Color.rgb(231, 228, 211)
        c.drawRoundRect(catX*40f+7, catY*40f+12, catX*40f+34, catY*40f+33, 8f, 8f, p)
        c.drawCircle(catX*40f+13, catY*40f+15, 6f, p)
        c.drawCircle(catX*40f+28, catY*40f+15, 6f, p)
        p.color = Color.rgb(30, 35, 38)
        c.drawCircle(catX*40f+25, catY*40f+21, 2f, p)
        p.color = Color.WHITE
        p.textSize = 20f
        c.drawText("CATGALE", 22f, 30f, p)
        c.drawText("FISH: " + (if (fishTaken) "1/1" else "0/1") + "    SCORE: $score", 22f, 55f, p)
        drawControls(c)
        if (ended) {
            p.color = Color.argb(210, 5, 10, 16)
            c.drawRect(0f, 0f, 800f, 520f, p)
            p.color = Color.rgb(250, 211, 125)
            p.textSize = 34f
            c.drawText(if (victory) "YOU GOT THE FISH!" else "THE DOG CAUGHT YOU!", 190f, 235f, p)
            p.textSize = 21f
            c.drawText("TAP RESET TO PLAY AGAIN", 235f, 275f, p)
        }
        c.restore()
        if (!ended) postInvalidateDelayed(350)
    }

    private fun drawControls(c: Canvas) {
        buttons.clear()
        val items = listOf(
            "up" to RectF(680f,360f,744f,414f),
            "left" to RectF(610f,420f,674f,474f),
            "down" to RectF(680f,420f,744f,474f),
            "right" to RectF(750f,420f,800f,474f),
            "reset" to RectF(22f,440f,150f,490f)
        )
        items.forEach { (name, r) ->
            buttons[name] = r
            p.color = if (name == "reset") Color.rgb(193,145,67) else Color.rgb(58,76,91)
            c.drawRoundRect(r, 10f, 10f, p)
            p.color = Color.WHITE
            p.textSize = 23f
            val label = when (name) {
                "up" -> "↑"; "down" -> "↓"; "left" -> "←"; "right" -> "→"; else -> "RESET"
            }
            c.drawText(label, r.centerX()-p.measureText(label)/2, r.centerY()+8, p)
        }
    }

    private fun move(dx: Int, dy: Int) {
        if (ended) return
        val nx = catX + dx; val ny = catY + dy
        if (nx !in 1..18 || ny !in 1..11 || "$nx,$ny" in walls) return
        catX = nx; catY = ny
        if (nx >= 17 && ny <= 2) {
            fishTaken = true; score += 100; ended = true; victory = true
        }
        val dog = patrol[dogStep]
        if ("$catX,$catY" !in hiding && abs(catX-dog.first)+abs(catY-dog.second) <= 1) {
            ended = true; victory = false
        }
        invalidate()
    }

    private fun reset() {
        catX = 2; catY = 10; dogStep = 0; fishTaken = false
        ended = false; victory = false; score = 0
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scale = minOf(width / 800f, height / 520f)
        val ox = (width - 800f*scale)/2f; val oy = (height - 520f*scale)/2f
        val x = (event.x-ox)/scale; val y = (event.y-oy)/scale
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (buttons.entries.firstOrNull { it.value.contains(x,y) }?.key) {
                "up" -> move(0,-1)
                "down" -> move(0,1)
                "left" -> move(-1,0)
                "right" -> move(1,0)
                "reset" -> reset()
            }
            return true
        }
        return true
    }
}
